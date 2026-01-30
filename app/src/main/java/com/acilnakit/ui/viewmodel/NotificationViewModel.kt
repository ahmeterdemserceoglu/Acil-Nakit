package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.Notification
import com.acilnakit.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<Notification>> = notificationRepository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = notificationRepository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId)
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Error marking as read: ${e.message}")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                notificationRepository.markAllAsRead()
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Error marking all as read: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
            } catch (e: Exception) {
                android.util.Log.e("NotificationVM", "Error deleting: ${e.message}")
            }
        }
    }
}
