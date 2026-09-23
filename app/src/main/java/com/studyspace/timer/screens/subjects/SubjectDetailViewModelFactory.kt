package com.studyspace.timer.screens.subjects

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Every other ViewModel in this app is an `AndroidViewModel` constructed
 * with no extra args, which Compose's default `viewModel()` factory
 * already knows how to build (see [com.studyspace.timer.StudySpaceApplication]'s
 * class doc). [SubjectDetailViewModel] is the first one that also needs a
 * runtime value (`subjectId`, from the nav route argument), so it needs its
 * own small factory — plain `ViewModelProvider.Factory`, not a DI framework,
 * consistent with the rest of the project staying dependency-light.
 */
class SubjectDetailViewModelFactory(
    private val application: Application,
    private val subjectId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return SubjectDetailViewModel(application, subjectId) as T
    }
}
