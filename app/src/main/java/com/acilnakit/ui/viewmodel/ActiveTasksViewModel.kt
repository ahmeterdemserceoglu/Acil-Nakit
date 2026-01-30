package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.Task
import com.acilnakit.data.model.TaskStatus
import com.acilnakit.data.repository.AuthRepository
import com.acilnakit.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ActiveTasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userId: String?
        get() = authRepository.currentUser.value?.uid

    // Oluşturduğum aktif görevler (OPEN, REQUESTED, ASSIGNED, IN_PROGRESS, DELIVERED)
    val myCreatedTasks: StateFlow<List<Task>> = flow {
        val uid = userId ?: return@flow emit(emptyList())
        taskRepository.getMyCreatedTasks(uid).collect { tasks ->
            val activeTasks = tasks.filter { 
                it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED 
            }.sortedByDescending { it.createdAt }
            emit(activeTasks)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Üstlendiğim aktif görevler
    val myAssignedTasks: StateFlow<List<Task>> = flow {
        val uid = userId ?: return@flow emit(emptyList())
        taskRepository.getMyAssignedTasks(uid).collect { tasks ->
            val activeTasks = tasks.filter { 
                it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED 
            }.sortedByDescending { it.assignedAt }
            emit(activeTasks)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
