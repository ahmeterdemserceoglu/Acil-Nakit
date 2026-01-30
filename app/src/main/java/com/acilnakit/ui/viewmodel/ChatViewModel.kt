package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.ChatRoom
import com.acilnakit.data.model.Message
import com.acilnakit.data.repository.AuthRepository
import com.acilnakit.data.repository.ChatRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentUserId: String? get() = authRepository.currentUser.value?.uid

    // Mesajlar (Real-time)
    val messages: StateFlow<List<Message>> = _currentChatId.flatMapLatest { chatId ->
        chatId?.let { chatRepository.getMessages(it) } ?: flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat Odası Bilgileri (Typing vb.)
    val chatRoom: StateFlow<ChatRoom?> = _currentChatId.flatMapLatest { chatId ->
        chatId?.let { chatRepository.getChatRoom(it) } ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setChatId(chatId: String) {
        _currentChatId.value = chatId
    }

    fun sendMessage(text: String) {
        val chatId = _currentChatId.value ?: return
        val senderId = currentUserId ?: return
        
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId,
                Message(
                    senderId = senderId,
                    text = text,
                    timestamp = Timestamp.now()
                )
            )
            // Mesaj gönderince typing'i kapat
            setTypingStatus(false)
        }
    }

    fun setTypingStatus(isTyping: Boolean) {
        val chatId = _currentChatId.value ?: return
        val userId = currentUserId ?: return
        
        // Eğer durum zaten aynıysa tekrar Firestore'a yazma (Optimized)
        val currentStatus = chatRoom.value?.typingStatus?.get(userId) ?: false
        if (currentStatus == isTyping) return

        viewModelScope.launch {
            chatRepository.setTypingStatus(chatId, userId, isTyping)
        }
    }

    fun markAsRead(messageId: String) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            chatRepository.markAsRead(chatId, messageId)
        }
    }
}
