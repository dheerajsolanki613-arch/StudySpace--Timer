package com.studyspace.timer.data.repository

import com.studyspace.timer.data.db.SubjectDao
import com.studyspace.timer.data.db.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for [SubjectEntity] storage — same role
 * [SessionRepository]/[GoalRepository] play for their own tables.
 */
class SubjectRepository(private val dao: SubjectDao) {

    fun allSubjects(): Flow<List<SubjectEntity>> = dao.allSubjects()

    suspend fun createSubject(name: String, icon: String, colorArgb: Int): Long =
        dao.insert(
            SubjectEntity(
                name = name,
                icon = icon,
                colorArgb = colorArgb,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )

    suspend fun updateSubject(existing: SubjectEntity, name: String, icon: String, colorArgb: Int) {
        dao.update(existing.copy(name = name, icon = icon, colorArgb = colorArgb))
    }

    /** Sessions already attributed to this subject are kept, just un-attributed — see [com.studyspace.timer.data.db.StudySessionEntity.subjectId]'s KDoc. */
    suspend fun deleteSubject(subject: SubjectEntity) = dao.delete(subject)

    companion object {
        /** Preset emoji offered by the subject editor — a fixed, curated list rather than a full emoji picker. */
        val ICON_PRESETS = listOf("📐", "⚡", "🧪", "💻", "📚", "📖", "🧠", "🎨", "🎵", "🌍", "🏛️", "🔬")

        /** Preset accent colors (packed ARGB ints) offered by the subject editor. */
        val COLOR_PRESETS: List<Int> = listOf(
            0xFF7C4DFF.toInt(), // violet
            0xFF00BCD4.toInt(), // cyan
            0xFFFF6B6B.toInt(), // coral
            0xFFFFB300.toInt(), // amber
            0xFF4CAF50.toInt(), // green
            0xFFEC407A.toInt(), // pink
            0xFF29B6F6.toInt(), // sky blue
            0xFF8D6E63.toInt()  // brown
        )
    }
}
