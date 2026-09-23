package com.studyspace.timer.data.repository

import com.studyspace.timer.data.db.StudySessionDao
import com.studyspace.timer.data.db.StudySessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * [SessionRepository.computeSubjectStats] takes an already-fetched session
 * list and has no Flow/Room dependency of its own (see its KDoc) — this
 * fake DAO exists purely so a [SessionRepository] can be constructed; none
 * of its methods are actually invoked by these tests.
 */
private class FakeStudySessionDao : StudySessionDao {
    override suspend fun insert(session: StudySessionEntity): Long = error("not used by this test")
    override fun recentSessions(limit: Int): Flow<List<StudySessionEntity>> = flowOf(emptyList())
    override fun sessionsSince(sinceEpochDay: Long): Flow<List<StudySessionEntity>> = flowOf(emptyList())
    override fun distinctSessionDaysDesc(): Flow<List<Long>> = flowOf(emptyList())
    override fun sessionsForSubject(subjectId: Long): Flow<List<StudySessionEntity>> = flowOf(emptyList())
}

class SubjectStatsTest {
    private val repository = SessionRepository(FakeStudySessionDao())
    private val today = LocalDate.of(2026, 9, 20)

    private fun session(daysAgo: Long, minutes: Long): StudySessionEntity {
        val date = today.minusDays(daysAgo)
        return StudySessionEntity(
            type = "SELF_STUDY",
            label = "Self-Study",
            startEpochMillis = 0L,
            durationMillis = minutes * 60_000L,
            completedNaturally = false,
            dateEpochDay = date.toEpochDay(),
            subjectId = 1L
        )
    }

    @Test
    fun `no sessions yields all-zero stats`() {
        val stats = repository.computeSubjectStats(emptyList(), today)
        assertEquals(SubjectStats(), stats)
    }

    @Test
    fun `today, week, and month windows only include sessions within range`() {
        val sessions = listOf(
            session(daysAgo = 0, minutes = 30),  // today, week, month
            session(daysAgo = 6, minutes = 20),  // week (edge, inclusive), month
            session(daysAgo = 7, minutes = 20),  // month only
            session(daysAgo = 29, minutes = 10), // month (edge, inclusive)
            session(daysAgo = 30, minutes = 999) // outside all three windows
        )
        val stats = repository.computeSubjectStats(sessions, today)

        assertEquals(30 * 60_000L, stats.todayMillis)
        assertEquals(50 * 60_000L, stats.weekMillis) // 30 + 20
        assertEquals(80 * 60_000L, stats.monthMillis) // 30 + 20 + 20 + 10
        assertEquals((30 + 20 + 20 + 10 + 999) * 60_000L, stats.totalMillis)
        assertEquals(5, stats.sessionCount)
    }

    @Test
    fun `average is total divided by session count`() {
        val sessions = listOf(session(0, 30), session(1, 90))
        val stats = repository.computeSubjectStats(sessions, today)
        assertEquals(2, stats.sessionCount)
        assertEquals(60 * 60_000L, stats.averageSessionMillis) // (30+90)/2 = 60
    }

    @Test
    fun `streak counts consecutive days ending today, ignoring a later gap`() {
        // Studied today, yesterday, the day before, then a gap at 3 days ago.
        val sessions = listOf(session(0, 10), session(1, 10), session(2, 10), session(3, 10))
            .filterIndexed { index, _ -> index != 3 } // drop the "3 days ago" one to create the gap
        val stats = repository.computeSubjectStats(sessions, today)
        assertEquals(3, stats.streakDays)
    }
}
