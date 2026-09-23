package com.studyspace.timer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * App-wide Room database.
 *
 * Version 1 -> 2 (Study Goals): added [StudyGoalEntity] via [MIGRATION_1_2].
 *
 * Version 2 -> 3 (Subjects): added [SubjectEntity] and a nullable
 * [StudySessionEntity.subjectId] foreign key via [MIGRATION_2_3]. SQLite
 * can't add an enforced `FOREIGN KEY` to an existing table with a plain
 * `ALTER TABLE ADD COLUMN`, so this migration rebuilds `study_sessions`
 * (create a new table with the full desired schema, copy every existing
 * row across with `subjectId` = NULL, drop the old table, rename) instead
 * of a destructive fallback — every existing session survives with no
 * subject attributed, which is exactly correct since none of them could
 * have had one.
 *
 * Version 3 -> 4 (Tasks): added [TaskEntity] and a nullable
 * [StudySessionEntity.taskId] foreign key via [MIGRATION_3_4] — same
 * rebuild approach as [MIGRATION_2_3], except this time the existing
 * `subjectId` values must be **carried over**, not reset to NULL (unlike
 * the 2->3 rebuild, some rows genuinely have one by now).
 *
 * Version 4 -> 5 (Study Planner): added [PlannedSessionEntity] via
 * [MIGRATION_4_5]. Unlike the two prior migrations, this one does **not**
 * touch `study_sessions` at all — no new column on it, no rebuild — since
 * this phase deliberately does not add a `study_sessions` -> `planned_sessions`
 * link column (see [PlannedSessionEntity]'s class doc for the scope
 * decision). Lowest-risk migration of the four so far: purely additive,
 * one new table, zero rebuilds.
 *
 * Version 5 -> 6 (Daily Summary): added a nullable
 * [TaskEntity.completedAtEpochMillis] via [MIGRATION_5_6] — a plain
 * `ALTER TABLE ADD COLUMN` (it's a nullable, non-foreign-key column, so no
 * table rebuild is needed, unlike [MIGRATION_2_3]/[MIGRATION_3_4]). Existing
 * rows get `NULL`, which is exactly right: no completion time was ever
 * recorded for them.
 */
@Database(
    entities = [
        StudySessionEntity::class, StudyGoalEntity::class, SubjectEntity::class,
        TaskEntity::class, PlannedSessionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao
    abstract fun studyGoalDao(): StudyGoalDao
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun plannedSessionDao(): PlannedSessionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Creates the `study_goals` table added for Study Goals. Column
         * order and the unique index name
         * (`index_study_goals_scope_subjectId`) match what Room generates
         * from [StudyGoalEntity]'s annotations, so Room's runtime schema
         * check on open doesn't flag a mismatch.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_goals` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `scope` TEXT NOT NULL,
                        `subjectId` INTEGER,
                        `targetMinutes` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_study_goals_scope_subjectId` ON `study_goals` (`scope`, `subjectId`)"
                )
            }
        }

        /**
         * Creates `subjects`, then rebuilds `study_sessions` to add the
         * `subjectId` foreign-key column (see class doc for why a rebuild
         * rather than `ALTER TABLE ADD COLUMN`). Column order, the foreign
         * key, and the index name (`index_study_sessions_subjectId`) match
         * what Room generates from the [StudySessionEntity] schema as of
         * this version (i.e. before [MIGRATION_3_4] adds `taskId`).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `colorArgb` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE `study_sessions_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `type` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `startEpochMillis` INTEGER NOT NULL,
                        `durationMillis` INTEGER NOT NULL,
                        `completedNaturally` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `subjectId` INTEGER,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `study_sessions_new`
                        (id, type, label, startEpochMillis, durationMillis, completedNaturally, dateEpochDay, subjectId)
                    SELECT id, type, label, startEpochMillis, durationMillis, completedNaturally, dateEpochDay, NULL
                    FROM `study_sessions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `study_sessions`")
                db.execSQL("ALTER TABLE `study_sessions_new` RENAME TO `study_sessions`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_sessions_subjectId` ON `study_sessions` (`subjectId`)"
                )
            }
        }

        /**
         * Creates `tasks`, then rebuilds `study_sessions` again to add the
         * `taskId` foreign-key column — same "SQLite can't ALTER TABLE ADD
         * an enforced FOREIGN KEY" reasoning as [MIGRATION_2_3]. Unlike
         * that migration, existing `subjectId` values are carried over
         * (`subjectId` from the old table, not `NULL`) since by this
         * version real rows may already have one set.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `title` TEXT NOT NULL,
                        `chapter` TEXT,
                        `topic` TEXT,
                        `subjectId` INTEGER,
                        `priority` TEXT NOT NULL,
                        `deadlineEpochDay` INTEGER,
                        `estimatedDurationMinutes` INTEGER,
                        `completed` INTEGER NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tasks_subjectId` ON `tasks` (`subjectId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE `study_sessions_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `type` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `startEpochMillis` INTEGER NOT NULL,
                        `durationMillis` INTEGER NOT NULL,
                        `completedNaturally` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `subjectId` INTEGER,
                        `taskId` INTEGER,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON DELETE SET NULL,
                        FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `study_sessions_new`
                        (id, type, label, startEpochMillis, durationMillis, completedNaturally, dateEpochDay, subjectId, taskId)
                    SELECT id, type, label, startEpochMillis, durationMillis, completedNaturally, dateEpochDay, subjectId, NULL
                    FROM `study_sessions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `study_sessions`")
                db.execSQL("ALTER TABLE `study_sessions_new` RENAME TO `study_sessions`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_sessions_subjectId` ON `study_sessions` (`subjectId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_sessions_taskId` ON `study_sessions` (`taskId`)"
                )
            }
        }

        /**
         * Creates `planned_sessions` only — no `study_sessions` rebuild
         * this time (see class doc). Column order and index names
         * (`index_planned_sessions_subjectId`, `_taskId`, `_dateEpochDay`)
         * match what Room generates from [PlannedSessionEntity]'s
         * annotations.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `planned_sessions` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `dateEpochDay` INTEGER NOT NULL,
                        `startMinuteOfDay` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `subjectId` INTEGER,
                        `taskId` INTEGER,
                        `notes` TEXT,
                        `status` TEXT NOT NULL,
                        `createdAtEpochMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON DELETE SET NULL,
                        FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_planned_sessions_subjectId` ON `planned_sessions` (`subjectId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_planned_sessions_taskId` ON `planned_sessions` (`taskId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_planned_sessions_dateEpochDay` ON `planned_sessions` (`dateEpochDay`)"
                )
            }
        }

        /**
         * Adds `tasks.completedAtEpochMillis`. Room's expected column for a
         * `Long?` with no default is `INTEGER` with no `NOT NULL` and no
         * `DEFAULT`, which is exactly what this statement produces, so the
         * runtime schema check on open passes. Existing tasks — including
         * already-completed ones — get `NULL`; see
         * [TaskEntity.completedAtEpochMillis] for why they are deliberately
         * not back-filled.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `completedAtEpochMillis` INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studyspace_timer.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build().also { instance = it }
            }
    }
}
