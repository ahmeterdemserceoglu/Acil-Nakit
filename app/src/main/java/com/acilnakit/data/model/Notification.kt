package com.acilnakit.data.model

import com.google.firebase.Timestamp

/**
 * Bildirim türleri
 */
enum class NotificationType {
    TASK_REQUEST,       // Göreve yeni başvuru
    TASK_ASSIGNED,      // Görev size atandı
    TASK_COMPLETED,     // Görev tamamlandı
    TASK_CANCELLED,     // Görev iptal edildi
    PAYMENT_RECEIVED,   // Ödeme alındı
    PAYMENT_SENT,       // Ödeme gönderildi
    NEW_MESSAGE,        // Yeni mesaj
    NEW_REVIEW,         // Yeni değerlendirme
    SYSTEM              // Sistem bildirimi
}

/**
 * Bildirim modeli
 */
data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: String = NotificationType.SYSTEM.name,
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val taskId: String? = null,      // İlgili görev (varsa)
    val chatId: String? = null,      // İlgili sohbet (varsa)
    val actionUrl: String? = null,   // Tıklanınca gidilecek yer
    val createdAt: Timestamp? = null
) {
    fun getNotificationType(): NotificationType {
        return try {
            NotificationType.valueOf(type)
        } catch (e: Exception) {
            NotificationType.SYSTEM
        }
    }
}
