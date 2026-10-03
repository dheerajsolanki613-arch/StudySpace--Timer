package com.studyspace.timer.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 21: instrumented (needs a device/emulator) tests of the Room layer —
 * subject/task CRUD and the ON DELETE SET NULL relations. Written, NOT run:
 * the standard CI build does not execute androidTest.
 */
@RunWith(AndroidJUnit4::class)
class RoomRelationsTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    private fun subject(name: String) =
        SubjectEntity(name = name, icon = "📐", colorArgb = 0xFF0000, createdAtEpochMillis = 1L)

    private fun task(title: String, subjectId: Long? = null) =
        TaskEntity(title = title, subjectId = subjectId, priority = "MEDIUM", createdAtEpochMillis = 1L)

    private fun session(subjectId: Long?, taskId: Long?) = StudySessionEntity(
        type = "SELF_STUDY", label = "x", startEpochMillis = 1000L, durationMillis = 60_000L,
        completedNaturally = true, dateEpochDay = 1L, subjectId = subjectId, taskId = taskId
    )

    @Test
    fun subjectCrud() = runBlocking {
        val dao = db.subjectDao()
        val id = dao.insert(subject("Math"))
        dao.update(dao.allSubjects().first().single().copy(name = "Maths"))
        assertEquals("Maths", dao.allSubjects().first().single().name)
        dao.delete(dao.allSubjects().first().single())
        assertEquals(0, dao.allSubjects().first().size)
        assertEquals(true, id > 0)
    }

    @Test
    fun deletingSubjectKeepsSessionsAndTasksButClearsLink() = runBlocking {
        val sid = db.subjectDao().insert(subject("Physics"))
        val tid = db.taskDao().insert(task("Ch1", sid))
        db.studySessionDao().insert(session(sid, tid))

        db.subjectDao().delete(db.subjectDao().allSubjects().first().single())

        val s = db.studySessionDao().allSessions().first().single()
        assertNull(s.subjectId)
        assertEquals(tid, s.taskId)
        assertNull(db.taskDao().allTasks().first().single().subjectId)
    }

    @Test
    fun deletingTaskKeepsSessionButClearsLink() = runBlocking {
        val tid = db.taskDao().insert(task("T"))
        db.studySessionDao().insert(session(null, tid))
        db.taskDao().delete(db.taskDao().allTasks().first().single())
        assertNull(db.studySessionDao().allSessions().first().single().taskId)
    }

    @Test
    fun taskCrudAndCompletion() = runBlocking {
        val dao = db.taskDao()
        dao.insert(task("A"))
        dao.update(dao.allTasks().first().single().copy(completed = true, completedAtEpochMillis = 5L))
        val t = dao.allTasks().first().single()
        assertEquals(true, t.completed)
        assertEquals(5L, t.completedAtEpochMillis)
    }
}
