package com.acilnakit.data.repository

import com.acilnakit.data.model.Notification
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val notificationsCollection = firestore.collection("notifications")
    
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Kullanıcının tüm bildirimlerini dinle
     */
    fun getNotifications(): Flow<List<Notification>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val subscription = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationRepo", "Error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(notifications)
            }
        
        awaitClose { subscription.remove() }
    }

    /**
     * Okunmamış bildirim sayısını dinle
     */
    fun getUnreadCount(): Flow<Int> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(0)
            close()
            return@callbackFlow
        }
        
        val subscription = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        
        awaitClose { subscription.remove() }
    }

    /**
     * Bildirimi okundu olarak işaretle
     */
    suspend fun markAsRead(notificationId: String) {
        notificationsCollection.document(notificationId)
            .update("isRead", true)
            .await()
    }

    /**
     * Tüm bildirimleri okundu olarak işaretle
     */
    suspend fun markAllAsRead() {
        val userId = currentUserId ?: return
        
        val unreadNotifications = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .await()
        
        val batch = firestore.batch()
        unreadNotifications.documents.forEach { doc ->
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }

    /**
     * Bildirimi sil
     */
    suspend fun deleteNotification(notificationId: String) {
        notificationsCollection.document(notificationId).delete().await()
    }

    /**
     * Yeni bildirim oluştur (local helper - genellikle Cloud Functions kullanır)
     */
    suspend fun createNotification(
        targetUserId: String,
        type: String,
        title: String,
        message: String,
        taskId: String? = null,
        chatId: String? = null
    ) {
        val notification = hashMapOf(
            "userId" to targetUserId,
            "type" to type,
            "title" to title,
            "message" to message,
            "isRead" to false,
            "taskId" to taskId,
            "chatId" to chatId,
            "createdAt" to Timestamp.now()
        )
        
        notificationsCollection.add(notification).await()
    }
}
