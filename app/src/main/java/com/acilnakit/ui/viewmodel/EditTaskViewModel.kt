package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.Task
import com.acilnakit.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _taskId = MutableStateFlow<String?>(null)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    val task: StateFlow<Task?> = _taskId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else taskRepository.getTask(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadTask(taskId: String) {
        _taskId.value = taskId
    }

    fun updateTask(title: String, description: String, category: String) {
        val taskId = _taskId.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                taskRepository.updateTaskDetails(taskId, title, description, category)
                android.util.Log.d("EditTaskVM", "Task updated: $taskId")
                _updateSuccess.value = true
            } catch (e: Exception) {
                android.util.Log.e("EditTaskVM", "Failed to update task: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
