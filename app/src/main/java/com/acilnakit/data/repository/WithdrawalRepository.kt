package com.acilnakit.data.repository

import com.acilnakit.data.model.WithdrawalRequest
import com.acilnakit.data.model.Transaction
import com.acilnakit.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WithdrawalRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun requestWithdrawal(request: WithdrawalRequest) {
        firestore.runTransaction { transaction ->
            // 1. Kullanıcının bakiyesini kontrol et
            val userRef = firestore.collection("users").document(request.userId)
            val userSnapshot = transaction.get(userRef)
            val currentBalance = userSnapshot.getDouble("balance") ?: 0.0
            
            if (currentBalance < request.amount) {
                throw Exception("Yetersiz bakiye.")
            }
            
            // 2. Bakiyeyi düş
            transaction.update(userRef, "balance", currentBalance - request.amount)
            
            // 3. Çekme talebini oluştur
            val withdrawalRef = firestore.collection("withdrawals").document()
            val finalRequest = request.copy(id = withdrawalRef.id)
            transaction.set(withdrawalRef, finalRequest)
            
            // 4. İşlem (Transaction) kaydı oluştur
            val transactionRef = firestore.collection("transactions").document()
            val walletTransaction = Transaction(
                id = transactionRef.id,
                userId = request.userId,
                amount = -request.amount,
                type = TransactionType.WITHDRAWAL,
                description = "Banka hesabına çekim talebi",
                withdrawalId = withdrawalRef.id,
                status = "pending"
            )
            transaction.set(transactionRef, walletTransaction)
        }.await()
    }
    
    fun getWithdrawalRequests(userId: String) = firestore.collection("withdrawals")
        .whereEqualTo("userId", userId)
        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
}
