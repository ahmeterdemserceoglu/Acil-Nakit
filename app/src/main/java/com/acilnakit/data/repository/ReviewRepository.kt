package com.acilnakit.data.repository

import com.acilnakit.data.model.Review
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun submitReview(review: Review, isCreatorRating: Boolean) {
        firestore.runTransaction { transaction ->
            // 1. İncelemeyi ekle
            val reviewRef = firestore.collection("reviews").document()
            transaction.set(reviewRef, review.copy(id = reviewRef.id))

            // 2. Kullanıcının rating ve trustScore değerlerini güncelle
            val userRef = firestore.collection("users").document(review.reviewedUserId)
            val userDoc = transaction.get(userRef)
            
            val currentRating = userDoc.getDouble("rating") ?: 5.0
            val currentReviewCount = userDoc.getLong("reviewCount") ?: 0L
            
            val newReviewCount = currentReviewCount + 1
            val newRating = ((currentRating * currentReviewCount) + review.rating) / newReviewCount
            
            transaction.update(userRef, "rating", newRating)
            transaction.update(userRef, "reviewCount", newReviewCount)
            
            // 3. Görev üzerindeki "oylandı" bayrağını güncelle
            val taskRef = firestore.collection("tasks").document(review.taskId)
            val field = if (isCreatorRating) "ratedByCreator" else "ratedByWorker"
            transaction.update(taskRef, field, true)

            // Trust score artışı (opsiyonel logic)
            if (review.rating >= 4) {
                transaction.update(userRef, "trustScore", FieldValue.increment(2.0))
            }
        }.await()
    }

    fun getUserReviews(userId: String): Flow<List<Review>> = callbackFlow {
        val subscription = firestore.collection("reviews")
            .whereEqualTo("reviewedUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val reviews = snapshot?.documents?.mapNotNull { it.toObject(Review::class.java) } ?: emptyList()
                trySend(reviews)
            }
        awaitClose { subscription.remove() }
    }
}
