package com.acilnakit.data.repository

import com.acilnakit.data.model.Transaction
import com.acilnakit.data.model.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class PaymentResult(
    val success: Boolean,
    val paymentUrl: String? = null,
    val formData: Map<String, String>? = null, // İş Bankası POST parametreleri
    val errorMessage: String? = null
)

@Singleton
class WalletRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    
    // ==================== BAKİYE ====================
    
    fun getBalance(userId: String): Flow<Double> = callbackFlow {
        if (FirebaseAuth.getInstance().currentUser == null) {
            trySend(0.0)
            close()
            return@callbackFlow
        }
        val subscription = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("WalletRepository", "Balance error: ${error.message}")
                    return@addSnapshotListener
                }
                val balance = snapshot?.getDouble("balance") ?: 0.0
                trySend(balance)
            }
        awaitClose { subscription.remove() }
    }

    fun getEscrowBalance(userId: String): Flow<Double> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val escrow = snapshot?.getDouble("escrowBalance") ?: 0.0
                trySend(escrow)
            }
        awaitClose { subscription.remove() }
    }

    // ==================== İŞLEMLER ====================
    
    fun getTransactions(userId: String): Flow<List<Transaction>> = callbackFlow {
        val subscription = firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("WalletRepository", "Transactions error: ${error.message}")
                    return@addSnapshotListener
                }
                val txs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(txs)
            }
        awaitClose { subscription.remove() }
    }

    // ==================== ÖDEME BAŞLATMA (iyzico) ====================
    
    /**
     * Cloud Function çağırarak iyzico ödeme formu başlat
     */
    suspend fun initiatePayment(amount: Double): PaymentResult {
        return try {
            val data = hashMapOf(
                "amount" to amount,
                "conversationId" to "conv_${System.currentTimeMillis()}"
            )
            
            val result = functions
                .getHttpsCallable("initiatePaymentV2")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            
            if (resultData?.get("success") == true) {
                PaymentResult(
                    success = true,
                    paymentUrl = resultData["paymentPageUrl"] as? String,
                    formData = resultData["formData"] as? Map<String, String>
                )
            } else {
                PaymentResult(
                    success = false,
                    errorMessage = "Ödeme başlatılamadı"
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("WalletRepository", "Payment init error code: ${(e as? com.google.firebase.functions.FirebaseFunctionsException)?.code}")
            android.util.Log.e("WalletRepository", "Payment init error message: ${e.message}", e)
            PaymentResult(
                success = false,
                errorMessage = e.message ?: "Ödeme başlatılamadı"
            )
        }
    }

    // ==================== PARA ÇEKME ====================
    
    suspend fun requestWithdrawal(userId: String, amount: Double, method: String, details: String) {
        // Cloud Function kullanarak güvenli para çekme
        try {
            val data = hashMapOf(
                "amount" to amount,
                "iban" to if (method == "IBAN") details else "",
                "accountName" to "Kullanıcı" // Gerçek uygulamada kullanıcıdan alınmalı
            )
            
            val result = functions
                .getHttpsCallable("requestWithdrawalV2")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            if (resultData?.get("success") != true) {
                throw Exception(resultData?.get("message") as? String ?: "Para çekme başarısız")
            }
        } catch (e: Exception) {
            android.util.Log.e("WalletRepository", "Withdrawal error: ${e.message}")
            throw e
        }
    }

    // ==================== ESCROW ====================
    
    suspend fun releaseEscrow(userId: String, amount: Double, taskId: String) {
        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("users").document(userId)
            val currentBalance = transaction.get(userRef).getDouble("balance") ?: 0.0
            transaction.update(userRef, "balance", currentBalance + amount)
            
            // Transaction kaydı
            val txRef = firestore.collection("transactions").document()
            transaction.set(txRef, hashMapOf(
                "userId" to userId,
                "amount" to amount,
                "type" to TransactionType.EARNING.name,
                "description" to "Görev kazancı",
                "taskId" to taskId,
                "status" to "completed",
                "createdAt" to com.google.firebase.Timestamp.now()
            ))
        }.await()
    }
    
    // ==================== FLASH TASK (BOOST) ====================
    
    suspend fun boostTask(taskId: String): Boolean {
        return try {
            val data = hashMapOf(
                "taskId" to taskId
            )
            
            val result = functions
                .getHttpsCallable("boostTaskV2")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            resultData?.get("success") == true
        } catch (e: Exception) {
            android.util.Log.e("WalletRepository", "Boost task error: ${e.message}")
            false
        }
    }
}
