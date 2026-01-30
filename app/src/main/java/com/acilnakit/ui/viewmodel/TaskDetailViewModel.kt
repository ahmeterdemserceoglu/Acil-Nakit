package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.*
import com.acilnakit.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _taskId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Görev bilgisi
    val task: StateFlow<Task?> = _taskId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else taskRepository.getTask(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Başvurular (sadece görev sahibi görür)
    val requests: StateFlow<List<TaskRequest>> = _taskId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else taskRepository.getTaskRequests(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mevcut kullanıcı ID
    private val currentUserId: String?
        get() = authRepository.currentUser.value?.uid

    // Görev sahibi mi?
    val isOwner: StateFlow<Boolean> = task.map { task ->
        task?.creatorId == currentUserId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Atanan işçi mi?
    val isWorker: StateFlow<Boolean> = task.map { task ->
        task?.workerId == currentUserId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Zaten başvurmuş mu?
    val hasRequested: StateFlow<Boolean> = task.map { task ->
        currentUserId?.let { uid ->
            task?.requestedBy?.contains(uid) == true
        } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadTask(id: String) {
        _taskId.value = id
    }

    // ==================== BAŞVURU ====================
    
    fun requestTask(message: String) {
        val taskId = _taskId.value ?: return
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Kullanıcı profilini al
                val profile = userRepository.getUserProfile(userId).first()
                
                val request = TaskRequest(
                    taskId = taskId,
                    userId = userId,
                    userName = profile?.name ?: "Kullanıcı",
                    userRating = profile?.rating ?: 5.0,
                    phone = profile?.phone ?: "",
                    message = message
                )
                
                taskRepository.requestTask(taskId, request)
                android.util.Log.d("TaskDetailVM", "Task requested: $taskId")
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to request task: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== ATAMA ====================
    
    fun assignToUser(request: TaskRequest) {
        val taskId = _taskId.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                taskRepository.assignTask(taskId, request.userId, request.phone)
                android.util.Log.d("TaskDetailVM", "Task assigned to: ${request.userId}")
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to assign task: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== İŞ AKIŞI ====================
    
    fun startWork() {
        val taskId = _taskId.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                taskRepository.startWork(taskId)
                android.util.Log.d("TaskDetailVM", "Work started for: $taskId")
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to start work: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsDelivered() {
        val taskId = _taskId.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                taskRepository.markAsDelivered(taskId)
                android.util.Log.d("TaskDetailVM", "Task marked as delivered: $taskId")
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to mark as delivered: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmDelivery() {
        val taskId = _taskId.value ?: return
        val userId = currentUserId ?: return
        val task = task.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (task.creatorId == userId) {
                    // Görev sahibi onaylıyor
                    taskRepository.confirmByCreator(taskId)
                } else if (task.workerId == userId) {
                    // İşçi onaylıyor
                    taskRepository.confirmByWorker(taskId)
                }
                android.util.Log.d("TaskDetailVM", "Delivery confirmed for: $taskId")
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to confirm delivery: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== ŞİKAYET ====================
    
    fun createDispute(description: String) {
        val taskId = _taskId.value ?: return
        val userId = currentUserId ?: return
        val task = task.value ?: return
        
        // Şikayet edilen kişi: eğer görev sahibiysem işçiyi, işçiysem görev sahibini şikayet ederim
        val reportedUserId = if (task.creatorId == userId) {
            task.workerId ?: return
        } else {
            task.creatorId
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dispute = Dispute(
                    taskId = taskId,
                    reporterId = userId,
                    reportedUserId = reportedUserId,
                    reason = DisputeReason.WORK_NOT_DELIVERED,
                    description = description
                )
                
                val disputeId = taskRepository.createDispute(dispute)
                android.util.Log.d("TaskDetailVM", "Dispute created: $disputeId")
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to create dispute: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _cancelSuccess = MutableStateFlow(false)
    val cancelSuccess: StateFlow<Boolean> = _cancelSuccess

    // ==================== İPTAL ====================
    
    fun cancelTask() {
        val taskId = _taskId.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                taskRepository.cancelTask(taskId)
                android.util.Log.d("TaskDetailVM", "Task cancelled: $taskId")
                _cancelSuccess.value = true
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to cancel task: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== OYLAMA ====================

    fun submitReview(rating: Float, comment: String) {
        val taskId = _taskId.value ?: return
        val userId = currentUserId ?: return
        val task = task.value ?: return
        
        // Kimi oyluyoruz?
        val (reviewedUserId, isCreatorRating) = if (task.creatorId == userId) {
            (task.workerId ?: return) to true
        } else {
            task.creatorId to false
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val review = Review(
                    taskId = taskId,
                    reviewerId = userId,
                    reviewedUserId = reviewedUserId,
                    rating = rating,
                    comment = comment
                )
                reviewRepository.submitReview(review, isCreatorRating)
            } catch (e: Exception) {
                android.util.Log.e("TaskDetailVM", "Failed to submit review: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getOrCreateChat(): String? {
        val task = task.value ?: return null
        val workerId = task.workerId ?: return null
        val participants = listOf(task.creatorId, workerId)
        return try {
            chatRepository.getOrCreateChat(task.id, participants)
        } catch (e: Exception) {
            null
        }
    }
}
