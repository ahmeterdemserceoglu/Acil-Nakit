package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.Task
import com.acilnakit.data.model.TaskStatus
import com.acilnakit.data.repository.AuthRepository
import com.acilnakit.data.repository.TaskRepository
import com.acilnakit.data.repository.UserRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CreateTaskState {
    object Idle : CreateTaskState()
    object Loading : CreateTaskState()
    object Success : CreateTaskState()
    data class InsufficientBalance(val required: Double, val current: Double) : CreateTaskState()
    data class Error(val message: String) : CreateTaskState()
}

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CreateTaskState>(CreateTaskState.Idle)
    val state: StateFlow<CreateTaskState> = _state

    private val _userBalance = MutableStateFlow(0.0)
    val userBalance: StateFlow<Double> = _userBalance

    init {
        loadUserBalance()
    }

    private fun loadUserBalance() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value ?: return@launch
            userRepository.getUserProfile(user.uid).collect { profile ->
                _userBalance.value = profile?.balance ?: 0.0
            }
        }
    }

    fun createTask(
        title: String,
        description: String,
        reward: Double,
        category: String,
        onSuccess: () -> Unit,
        onInsufficientBalance: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = CreateTaskState.Loading
            
            try {
                val user = authRepository.currentUser.value
                if (user == null) {
                    _state.value = CreateTaskState.Error("Oturum bulunamadı")
                    return@launch
                }
                
                // Kullanıcının profil bilgilerini al
                val profile = userRepository.getUserProfile(user.uid).first()
                val currentBalance = profile?.balance ?: 0.0
                
                // Bakiye kontrolü
                if (currentBalance < reward) {
                    _state.value = CreateTaskState.InsufficientBalance(
                        required = reward,
                        current = currentBalance
                    )
                    onInsufficientBalance()
                    return@launch
                }
                
                val task = Task(
                    title = title,
                    description = description,
                    rewardAmount = reward,
                    category = category,
                    schoolName = profile?.school ?: "",
                    campusName = profile?.campus ?: "",
                    creatorId = user.uid,
                    creatorPhone = profile?.phone ?: "",
                    status = TaskStatus.OPEN,
                    createdAt = Timestamp.now()
                )
                
                android.util.Log.d("CreateTaskVM", "Creating task: $title for school: ${profile?.school}")
                taskRepository.createTask(task)
                
                android.util.Log.d("CreateTaskVM", "Task created successfully through atomic transaction")
                _state.value = CreateTaskState.Success
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("CreateTaskVM", "Error creating task: ${e.message}")
                _state.value = CreateTaskState.Error(e.message ?: "Görev oluşturulamadı")
            }
        }
    }

    fun resetState() {
        _state.value = CreateTaskState.Idle
    }
}
