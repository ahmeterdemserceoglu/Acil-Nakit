package com.acilnakit.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val profile = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)?.copy(id = snapshot.id)
                } else {
                    null 
                }
                trySend(profile)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateCampus(userId: String, campus: String) {
        firestore.collection("users").document(userId)
            .update("campus", campus, "campusName", campus).await()
    }

    suspend fun updateName(userId: String, name: String) {
        firestore.collection("users").document(userId)
            .update("name", name).await()
    }

    suspend fun updatePhone(userId: String, phone: String) {
        firestore.collection("users").document(userId)
            .update("phone", phone).await()
    }

    suspend fun updateIdCardImageUrl(userId: String, url: String) {
        firestore.collection("users").document(userId)
            .update("idCardImageUrl", url, "verificationStatus", "pending").await()
    }

    suspend fun incrementCompletedTasks(userId: String) {
        firestore.collection("users").document(userId)
            .update("completedTasks", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }

    suspend fun incrementPublishedTasks(userId: String) {
        firestore.collection("users").document(userId)
            .update("publishedTasks", com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }

    // ==================== BAKİYE YÖNETİMİ ====================
    
    /**
     * Bakiyeden düş (görev oluşturma, escrow'a alma)
     */
    suspend fun deductBalance(userId: String, amount: Double) {
        firestore.collection("users").document(userId)
            .update("balance", com.google.firebase.firestore.FieldValue.increment(-amount))
            .await()
        android.util.Log.d("UserRepository", "Deducted $amount from user $userId")
    }

    /**
     * Bakiyeye ekle (görev tamamlama, para yükleme)
     */
    suspend fun addBalance(userId: String, amount: Double) {
        firestore.collection("users").document(userId)
            .update("balance", com.google.firebase.firestore.FieldValue.increment(amount))
            .await()
        android.util.Log.d("UserRepository", "Added $amount to user $userId")
    }

    /**
     * Anlık bakiye sorgulama
     */
    suspend fun getBalanceOnce(userId: String): Double {
        val doc = firestore.collection("users").document(userId).get().await()
        return doc.getDouble("balance") ?: 0.0
    }
}

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val campus: String = "",
    val campusName: String = "",
    val school: String = "",
    val schoolName: String = "",  // Okul adı (görüntüleme için)
    val department: String = "",
    val rating: Double = 5.0,
    val reviewCount: Int = 0,             // Değerlendirme sayısı
    val completedTasks: Int = 0,
    val publishedTasks: Int = 0,
    val trustScore: Double = 100.0,       // Güven skoru (1-100)
    val badges: List<String> = emptyList(),
    val balance: Double = 0.0,
    val escrowBalance: Double = 0.0,      // Blokeli / İşlemdeki bakiye
    val fcmToken: String? = null,
    val profileImageUrl: String? = null,  // Profil fotoğrafı URL
    val idCardImageUrl: String? = null,   // Okul Kimlik Fotoğrafı URL (Storage)
    val verificationStatus: String = "none", // none, pending, verified, rejected
    val isVerified: Boolean = false,      // Öğrenci doğrulama (Mavi Tik)
    val createdAt: com.google.firebase.Timestamp? = null
)
