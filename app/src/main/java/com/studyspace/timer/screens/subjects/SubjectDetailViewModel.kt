package com.studyspace.timer.screens.subjects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.repository.SubjectStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs Subject Detail for one [subjectId]. Takes the id as a constructor
 * param (via [SubjectDetailViewModelFactory]) rather than a second
 * "load(id)" call, so [subject]/[stats] are correct from the very first
 * emission instead of briefly showing an empty/default state.
 */
class SubjectDetailViewModel(
    application: Application,
    private val subjectId: Long
) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val subjectRepository = app.subjectRepository
    private val sessionRepository = app.sessionRepository

    val subject: StateFlow<SubjectEntity?> = subjectRepository.allSubjects()
        .map { subjects -> subjects.find { it.id == subjectId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stats: StateFlow<SubjectStats> = sessionRepository.subjectStats(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectStats())

    fun rename(name: String, icon: String, colorArgb: Int) {
        val current = subject.value ?: return
        viewModelScope.launch { subjectRepository.updateSubject(current, name, icon, colorArgb) }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = subject.value ?: return
        viewModelScope.launch {
            subjectRepository.deleteSubject(current)
            onDeleted()
        }
    }
}
