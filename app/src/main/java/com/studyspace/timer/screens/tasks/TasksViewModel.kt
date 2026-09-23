package com.studyspace.timer.screens.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyspace.timer.StudySpaceApplication
import com.studyspace.timer.data.TaskPriority
import com.studyspace.timer.data.db.SubjectEntity
import com.studyspace.timer.data.db.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs [TasksScreen]: the task list, the subject list its editor dialog's subject picker needs, and every CRUD action. */
class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudySpaceApplication
    private val taskRepository = app.taskRepository
    private val subjectRepository = app.subjectRepository

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = subjectRepository.allSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTask(
        title: String,
        chapter: String?,
        topic: String?,
        subjectId: Long?,
        priority: TaskPriority,
        deadlineEpochDay: Long?,
        estimatedDurationMinutes: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.createTask(title, chapter, topic, subjectId, priority, deadlineEpochDay, estimatedDurationMinutes)
        }
    }

    fun updateTask(
        existing: TaskEntity,
        title: String,
        chapter: String?,
        topic: String?,
        subjectId: Long?,
        priority: TaskPriority,
        deadlineEpochDay: Long?,
        estimatedDurationMinutes: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskRepository.updateTask(existing, title, chapter, topic, subjectId, priority, deadlineEpochDay, estimatedDurationMinutes)
        }
    }

    fun setCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch { taskRepository.setCompleted(task, completed) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.deleteTask(task) }
    }
}
