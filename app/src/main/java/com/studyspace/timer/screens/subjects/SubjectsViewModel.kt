package com.studyspace.timer.screens.subjects

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.db.SubjectEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs [SubjectsScreen]'s list and the "add subject" flow. */
class SubjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val subjectRepository = app.subjectRepository

    val subjects: StateFlow<List<SubjectEntity>> = subjectRepository.allSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSubject(name: String, icon: String, colorArgb: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { subjectRepository.createSubject(trimmed, icon, colorArgb) }
    }
}
