package com.acilnakit.data.model

import com.google.firebase.Timestamp

data class Message(
    var id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)

data class ChatRoom(
    var id: String = "",
    val taskId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastTimestamp: Timestamp = Timestamp.now(),
    val typingStatus: Map<String, Boolean> = emptyMap(),
    val unreadCount: Map<String, Int> = emptyMap()
)

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EARNING,
    val description: String = "",
    val taskId: String? = null,
    val withdrawalId: String? = null,
    val disputeId: String? = null,
    val status: String = "completed",
    val createdAt: Timestamp = Timestamp.now()
)

enum class TransactionType {
    DEPOSIT,        // Para yükleme
    EARNING,        // Görev kazancı
    WITHDRAWAL,     // Para çekme
    TASK_PAYMENT,   // Görev ödemesi (escrow'a)
    REFUND          // İade
}

data class Review(
    val id: String = "",
    val taskId: String = "",
    val reviewerId: String = "",
    val reviewedUserId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class WithdrawalRequest(
    var id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val iban: String = "",
    val accountHolder: String = "",
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp? = null,
    val note: String? = null
)

enum class WithdrawalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED
}
