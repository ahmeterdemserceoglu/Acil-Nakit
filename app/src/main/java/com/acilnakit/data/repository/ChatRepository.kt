package com.acilnakit.data.repository

import com.acilnakit.data.model.ChatRoom
import com.acilnakit.data.model.Message
import com.acilnakit.util.toModel
import com.acilnakit.util.toListModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.let {
                    trySend(it.toListModel<Message>())
                }
            }
        awaitClose { listener.remove() }
    }

    fun getChatRoom(chatId: String): Flow<ChatRoom?> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.let {
                    trySend(it.toModel<ChatRoom>())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, message: Message) {
        val batch = firestore.batch()
        val chatRef = firestore.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document()
        
        val finalMessage = message.copy(id = messageRef.id)
        
        batch.set(messageRef, finalMessage)
        batch.update(chatRef, mapOf(
            "lastMessage" to finalMessage.text,
            "lastTimestamp" to finalMessage.timestamp
        ))
        
        batch.commit().await()
    }

    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        firestore.collection("chats").document(chatId)
            .update("typingStatus.$userId", isTyping)
            .await()
    }

    suspend fun markAsRead(chatId: String, messageId: String) {
        firestore.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .update("isRead", true)
            .await()
    }
    
    suspend fun getOrCreateChat(taskId: String, participants: List<String>): String {
        val query = firestore.collection("chats")
            .whereEqualTo("taskId", taskId)
            .get()
            .await()
            
        if (!query.isEmpty) {
            return query.documents[0].id
        }
        
        val newChatRef = firestore.collection("chats").document()
        val chatRoom = ChatRoom(
            id = newChatRef.id,
            taskId = taskId,
            participants = participants
        )
        newChatRef.set(chatRoom).await()
        return newChatRef.id
    }
}
