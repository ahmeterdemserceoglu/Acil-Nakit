package com.acilnakit.data.model

import com.google.firebase.Timestamp

enum class TaskStatus {
    OPEN,           // Görev açık, herkes başvurabilir
    REQUESTED,      // Başvuru var, atama bekleniyor
    ASSIGNED,       // Bir kişiye atandı
    IN_PROGRESS,    // İş devam ediyor
    DELIVERED,      // İşçi teslim etti, onay bekleniyor
    COMPLETED,      // Karşılıklı onaylandı
    DISPUTED,       // Şikayette, inceleniyor
    CANCELLED       // İptal edildi
}

data class Task(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val rewardAmount: Double = 0.0,
    val schoolName: String = "",
    val campusName: String = "",
    val category: String = "Genel",
    
    // Kullanıcı Bilgileri
    val creatorId: String = "",
    val creatorPhone: String = "",      
    val creatorRating: Double = 5.0,    // Görev sahibinin puanı
    val creatorTasksCompleted: Int = 0, // Görev sahibinin bitirdiği toplam iş
    val workerId: String? = null,       
    val workerPhone: String? = null,    // İşçinin telefonu
    
    // Başvuru Sistemi
    val requestedBy: List<String> = emptyList(),  // Başvuran kullanıcı ID'leri
    
    // Durum Takibi
    val status: TaskStatus = TaskStatus.OPEN,
    val createdAt: Timestamp = Timestamp.now(),
    val assignedAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    
    // Onay Sistemi
    val creatorConfirmed: Boolean = false,  // Görev sahibi onayladı mı?
    val workerConfirmed: Boolean = false,   // İşçi onayladı mı?
    
    // Şikayet Sistemi
    val disputeId: String? = null,
    
    // Ödeme Sistemi
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val paymentReleasedAt: Timestamp? = null,
    
    // Oylama Durumu
    val ratedByCreator: Boolean = false,
    val ratedByWorker: Boolean = false,

    // Teslimat Kanıtı
    val deliveryProofUrl: String? = null,
    val deliveryNote: String? = null
)

enum class PaymentStatus {
    PENDING,        // Beklemede (escrow'da)
    PROCESSING,     // İşleniyor (24 saat bekleme)
    RELEASED,       // Serbest bırakıldı
    REFUNDED,       // İade edildi
    HELD            // Şikayet nedeniyle tutuldu
}

// Şikayet Modeli
data class Dispute(
    var id: String = "",
    val taskId: String = "",
    val reporterId: String = "",        // Şikayet eden kullanıcı
    val reportedUserId: String = "",    // Şikayet edilen kullanıcı
    val reason: DisputeReason = DisputeReason.OTHER,
    val description: String = "",
    val videoUrl: String? = null,       // Kanıt videosu (opsiyonel)
    val status: DisputeStatus = DisputeStatus.PENDING,
    val resolution: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val resolvedAt: Timestamp? = null
)

enum class DisputeReason {
    WORK_NOT_DELIVERED,     // İş teslim edilmedi
    POOR_QUALITY,           // Kalite düşük
    LATE_DELIVERY,          // Geç teslim
    SCAM,                   // Dolandırıcılık
    HARASSMENT,             // Taciz
    OTHER                   // Diğer
}

enum class DisputeStatus {
    PENDING,                // İnceleniyor
    RESOLVED_CREATOR_WIN,   // Görev sahibi haklı bulundu
    RESOLVED_WORKER_WIN,    // İşçi haklı bulundu
    RESOLVED_PARTIAL,       // Kısmi çözüm
    REJECTED                // Şikayet reddedildi
}

// Görev Başvurusu (opsiyonel detaylı başvuru için)
data class TaskRequest(
    var id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRating: Double = 0.0,
    val phone: String = "",
    val message: String = "",           // Başvuru mesajı
    val createdAt: Timestamp = Timestamp.now()
)
