package com.acilnakit.data.repository

import com.acilnakit.data.model.Dispute
import com.acilnakit.data.model.DisputeStatus
import com.acilnakit.data.model.PaymentStatus
import com.acilnakit.data.model.Task
import com.acilnakit.data.model.TaskRequest
import com.acilnakit.data.model.TaskStatus
import com.acilnakit.util.toModel
import com.acilnakit.util.toListModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val tasksCollection = firestore.collection("tasks")
    private val requestsCollection = firestore.collection("task_requests")
    private val disputesCollection = firestore.collection("disputes")

    // ==================== GÖREV LİSTELEME ====================
    
    /**
     * Okulun açık görevlerini getir
     */
    fun getSchoolTasks(school: String): Flow<List<Task>> = callbackFlow {
        val subscription = tasksCollection
            .whereEqualTo("schoolName", school)
            .whereIn("status", listOf(TaskStatus.OPEN.name, TaskStatus.REQUESTED.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("TaskRepository", "Firestore error: ${error.message}")
                    return@addSnapshotListener
                }
                
                val tasks = snapshot?.toListModel<Task>() ?: emptyList()
                trySend(tasks)
            }
            
        awaitClose { subscription.remove() }
    }

    /**
     * Tek bir görevi getir (real-time)
     */
    fun getTask(taskId: String): Flow<Task?> = callbackFlow {
        val subscription = tasksCollection.document(taskId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("TaskRepository", "Error fetching task: ${error.message}")
                    return@addSnapshotListener
                }
                val task = snapshot?.toModel<Task>()
                trySend(task)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Kullanıcının kendi görevlerini getir (oluşturduğu)
     */
    fun getMyCreatedTasks(userId: String): Flow<List<Task>> = callbackFlow {
        val subscription = tasksCollection
            .whereEqualTo("creatorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val tasks = snapshot?.toListModel<Task>() ?: emptyList()
                trySend(tasks)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Kullanıcının üstlendiği görevleri getir
     */
    fun getMyAssignedTasks(userId: String): Flow<List<Task>> = callbackFlow {
        val subscription = tasksCollection
            .whereEqualTo("workerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val tasks = snapshot?.toListModel<Task>() ?: emptyList()
                trySend(tasks)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Kullanıcının tüm geçmişini getir (Tamamlanmış ve İptal edilmiş)
     */
    fun getTaskHistory(userId: String): Flow<List<Task>> = callbackFlow {
        // Not: Firestore merge query kısıtlamaları nedeniyle tüm görevleri çekip filtreliyoruz 
        // veya iki ayrı query birleştirilebilir. Basitlik ve real-time için iki listener:
        val subscription = tasksCollection
            .whereIn("status", listOf(TaskStatus.COMPLETED.name, TaskStatus.CANCELLED.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val allArchived = snapshot?.toListModel<Task>() ?: emptyList()
                val myHistory = allArchived.filter { it.creatorId == userId || it.workerId == userId }
                trySend(myHistory)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Kullanıcının dahil olduğu tüm AKTİF görevler
     */
    fun getActiveTasks(userId: String): Flow<List<Task>> = callbackFlow {
        val subscription = tasksCollection
            .whereIn("status", listOf(
                TaskStatus.OPEN.name, 
                TaskStatus.REQUESTED.name, 
                TaskStatus.ASSIGNED.name, 
                TaskStatus.IN_PROGRESS.name, 
                TaskStatus.DELIVERED.name
            ))
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val active = snapshot?.toListModel<Task>() ?: emptyList()
                val myActive = active.filter { it.creatorId == userId || it.workerId == userId }
                trySend(myActive)
            }
        awaitClose { subscription.remove() }
    }

    // ==================== GÖREV OLUŞTURMA ====================
    
    // ==================== GÖREV OLUŞTURMA (PROVİZYONLU) ====================
    
    suspend fun createTask(task: Task): String {
        try {
            val data = hashMapOf(
                "title" to task.title,
                "description" to task.description,
                "rewardAmount" to task.rewardAmount,
                "category" to task.category,
                "schoolName" to task.schoolName,
                "campusName" to task.campusName,
                "creatorPhone" to task.creatorPhone,
                "creatorRating" to task.creatorRating,
                "creatorTasksCompleted" to task.creatorTasksCompleted
            )
            
            val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
            val result = functions
                .getHttpsCallable("createTaskV2")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            return resultData?.get("taskId") as? String ?: throw Exception("Görev oluşturulamadı")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Create task error: ${e.message}")
            throw e
        }
    }

    // ==================== İPTAL VE İADE SİSTEMİ ====================
    
    suspend fun cancelTask(taskId: String) {
        firestore.runTransaction { transaction ->
            val taskRef = tasksCollection.document(taskId)
            val taskSnapshot = transaction.get(taskRef)
            val task = taskSnapshot.toObject(Task::class.java) ?: throw Exception("Görev bulunamadı")
            
            // Kullanıcıyı da erkenden oku (ALL READS FIRST)
            val userRef = firestore.collection("users").document(task.creatorId)
            val userSnapshot = transaction.get(userRef)
            
            // Eğer bir işçi atandıysa, iptal edilemez (şimdilik sadece OPEN iptal)
            if (task.status != TaskStatus.OPEN && task.status != TaskStatus.REQUESTED) {
                throw Exception("Sadece henüz atanmamış görevler iptal edilebilir.")
            }
            
            // --- READS DONE, START WRITES ---
            
            // 1. Durumu güncelle
            transaction.update(taskRef, "status", TaskStatus.CANCELLED.name)
            transaction.update(taskRef, "paymentStatus", PaymentStatus.REFUNDED.name)
            
            // 2. Parayı iade et (Escrow'dan Balance'a)
            val currentBalance = userSnapshot.getDouble("balance") ?: 0.0
            val currentEscrow = userSnapshot.getDouble("escrowBalance") ?: 0.0
            
            // Escrow düş, Balance art
            if (currentEscrow >= task.rewardAmount) {
                transaction.update(userRef, "escrowBalance", currentEscrow - task.rewardAmount)
            } else {
                transaction.update(userRef, "escrowBalance", 0.0) // Güvenlik önlemi
            }
            transaction.update(userRef, "balance", currentBalance + task.rewardAmount)
            
            // 3. İade işlemini kaydet
            val txRef = firestore.collection("transactions").document()
            transaction.set(txRef, hashMapOf(
                "userId" to task.creatorId,
                "amount" to task.rewardAmount,
                "type" to "REFUND",
                "description" to "Görev iptal iadesi",
                "taskId" to taskId,
                "status" to "completed",
                "createdAt" to Timestamp.now()
            ))
        }.await()
    }

    // ==================== GÖREV DÜZENLEME ====================
    
    /**
     * Görev detaylarını güncelle (sadece OPEN veya REQUESTED durumda)
     */
    suspend fun updateTaskDetails(taskId: String, title: String, description: String, category: String) {
        tasksCollection.document(taskId).update(
            mapOf(
                "title" to title,
                "description" to description,
                "category" to category
            )
        ).await()
        android.util.Log.d("TaskRepository", "Task details updated: $taskId")
    }

    // ==================== BAŞVURU SİSTEMİ ====================
    
    /**
     * Göreve başvur
     */
    suspend fun requestTask(taskId: String, request: TaskRequest) {
        try {
            // Başvuruyu kaydet
            val requestData = hashMapOf(
                "taskId" to taskId,
                "userId" to request.userId,
                "userName" to request.userName,
                "userRating" to request.userRating,
                "phone" to request.phone,
                "message" to request.message,
                "createdAt" to Timestamp.now()
            )
            requestsCollection.add(requestData).await()
            
            // Görevi güncelle - başvuranlar listesine ekle
            tasksCollection.document(taskId).update(
                mapOf(
                    "requestedBy" to FieldValue.arrayUnion(request.userId),
                    "status" to TaskStatus.REQUESTED.name
                )
            ).await()
            
            android.util.Log.d("TaskRepository", "Task request submitted for: $taskId")
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to request task: ${e.message}")
            throw e
        }
    }

    /**
     * Görev başvurularını getir
     */
    fun getTaskRequests(taskId: String): Flow<List<TaskRequest>> = callbackFlow {
        val subscription = requestsCollection
            .whereEqualTo("taskId", taskId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TaskRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { subscription.remove() }
    }

    // ==================== ATAMA SİSTEMİ ====================
    
    /**
     * Görevi birine ata
     */
    suspend fun assignTask(taskId: String, workerId: String, workerPhone: String) {
        tasksCollection.document(taskId).update(
            mapOf(
                "workerId" to workerId,
                "workerPhone" to workerPhone,
                "status" to TaskStatus.ASSIGNED.name,
                "assignedAt" to Timestamp.now()
            )
        ).await()
        android.util.Log.d("TaskRepository", "Task $taskId assigned to $workerId")
    }

    /**
     * İşe başla
     */
    suspend fun startWork(taskId: String) {
        tasksCollection.document(taskId).update(
            "status", TaskStatus.IN_PROGRESS.name
        ).await()
    }

    // ==================== TESLİM VE ONAY SİSTEMİ ====================
    
    /**
     * İşçi: İşi teslim ettim
     */
    suspend fun markAsDelivered(taskId: String) {
        tasksCollection.document(taskId).update(
            mapOf(
                "status" to TaskStatus.DELIVERED.name,
                "workerConfirmed" to true,
                "deliveredAt" to Timestamp.now()
            )
        ).await()
        android.util.Log.d("TaskRepository", "Task $taskId marked as delivered")
    }

    /**
     * Görev Sahibi: Teslim aldım, onaylıyorum
     */
    suspend fun confirmByCreator(taskId: String) {
        tasksCollection.document(taskId).update(
            "creatorConfirmed", true
        ).await()
        android.util.Log.d("TaskRepository", "Task $taskId confirmed by creator")
    }

    /**
     * İşçi onayı (teslim sonrası)
     */
    suspend fun confirmByWorker(taskId: String) {
        tasksCollection.document(taskId).update(
            "workerConfirmed", true
        ).await()
        android.util.Log.d("TaskRepository", "Task $taskId confirmed by worker")
    }

    // ==================== ŞİKAYET SİSTEMİ ====================
    
    /**
     * Şikayet oluştur
     */
    suspend fun createDispute(dispute: Dispute): String {
        try {
            val disputeData = hashMapOf(
                "taskId" to dispute.taskId,
                "reporterId" to dispute.reporterId,
                "reportedUserId" to dispute.reportedUserId,
                "reason" to dispute.reason.name,
                "description" to dispute.description,
                "videoUrl" to dispute.videoUrl,
                "status" to DisputeStatus.PENDING.name,
                "createdAt" to Timestamp.now()
            )
            
            val docRef = disputesCollection.add(disputeData).await()
            
            // Görevi şikayette durumuna al
            tasksCollection.document(dispute.taskId).update(
                mapOf(
                    "status" to TaskStatus.DISPUTED.name,
                    "disputeId" to docRef.id,
                    "paymentStatus" to PaymentStatus.HELD.name
                )
            ).await()
            
            android.util.Log.d("TaskRepository", "Dispute created: ${docRef.id}")
            return docRef.id
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to create dispute: ${e.message}")
            throw e
        }
    }

    /**
     * Şikayet detaylarını getir
     */
    fun getDispute(disputeId: String): Flow<Dispute?> = callbackFlow {
        val subscription = disputesCollection.document(disputeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val dispute = snapshot?.toObject(Dispute::class.java)?.copy(id = snapshot.id)
                trySend(dispute)
            }
        awaitClose { subscription.remove() }
    }

}
