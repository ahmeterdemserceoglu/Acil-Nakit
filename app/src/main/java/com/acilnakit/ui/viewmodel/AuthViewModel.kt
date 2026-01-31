package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object VerificationSent : AuthState()
    object Verified : AuthState()
    object NeedsCampus : AuthState()
    object Unauthenticated : AuthState()
    object Suspended : AuthState()
    data class NotVerifiedYet(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _schoolName = MutableStateFlow<String?>(null)
    val schoolName: StateFlow<String?> = _schoolName
    
    // Çıkış yapılırken başka işlemlerin durumu değiştirmesini engeller
    private var isLoggingOut = false

    init {
        android.util.Log.d("AuthViewModel", "Init called")
        checkInitialSession()
    }

    private fun checkInitialSession() {
        if (isLoggingOut) return
        
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "checkInitialSession started")
            val user = repository.currentUser.value
            android.util.Log.d("AuthViewModel", "Current user: ${user?.email}")
            if (user != null) {
                performVerificationCheck(user)
            } else {
                android.util.Log.d("AuthViewModel", "No user, setting Unauthenticated")
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private suspend fun performVerificationCheck(user: com.google.firebase.auth.FirebaseUser) {
        if (isLoggingOut) {
            android.util.Log.d("AuthViewModel", "performVerificationCheck skipped - logging out")
            return
        }
        
        android.util.Log.d("AuthViewModel", "performVerificationCheck started for ${user.email}")
        _authState.value = AuthState.Loading
        
        val result = withTimeoutOrNull(10000L) {
            try {
                android.util.Log.d("AuthViewModel", "Reloading user...")
                user.reload().await()
                android.util.Log.d("AuthViewModel", "Reload complete. isEmailVerified: ${user.isEmailVerified}")
                
                if (user.isEmailVerified) {
                    android.util.Log.d("AuthViewModel", "Email verified, getting token...")
                    user.getIdToken(true).await()
                    android.util.Log.d("AuthViewModel", "Token received, checking Firestore...")
                    
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userRef = db.collection("users").document(user.uid)
                    
                    val userDoc = userRef.get().await()
                    android.util.Log.d("AuthViewModel", "Firestore doc exists: ${userDoc.exists()}")
                    
                    if (userDoc.exists() && userDoc.getBoolean("isSuspended") == true) {
                        android.util.Log.d("AuthViewModel", "User is suspended")
                        return@withTimeoutOrNull AuthState.Suspended
                    }
                    
                    if (!userDoc.exists()) {
                        android.util.Log.d("AuthViewModel", "Creating skeleton profile...")
                        val skeleton = mapOf(
                            "email" to user.email,
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "balance" to 0.0,
                            "name" to "",
                            "school" to "",
                            "campusName" to ""
                        )
                        userRef.set(skeleton).await()
                        android.util.Log.d("AuthViewModel", "Skeleton created, returning NeedsCampus")
                        return@withTimeoutOrNull AuthState.NeedsCampus
                    }
                    
                    val campus = userDoc.getString("campusName") ?: userDoc.getString("campus")
                    val school = userDoc.getString("school")
                    val name = userDoc.getString("name")

                    _schoolName.value = school
                    android.util.Log.d("AuthViewModel", "Profile data - campus: $campus, school: $school, name: $name")
                    
                    if (campus.isNullOrEmpty() || school.isNullOrEmpty() || name.isNullOrEmpty()) {
                        android.util.Log.d("AuthViewModel", "Profile incomplete, returning NeedsCampus")
                        return@withTimeoutOrNull AuthState.NeedsCampus
                    } else {
                        android.util.Log.d("AuthViewModel", "Profile complete, returning Verified")
                        syncFcmToken() // Ensure token is fresh
                        return@withTimeoutOrNull AuthState.Verified
                    }
                } else {
                    android.util.Log.d("AuthViewModel", "Email not verified")
                    return@withTimeoutOrNull AuthState.NotVerifiedYet("E-posta adresiniz henüz doğrulanmadı.")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error in verification: ${e.message}")
                return@withTimeoutOrNull AuthState.NeedsCampus
            }
        }
        
        // Çıkış yapılıyorsa durumu değiştirme
        if (isLoggingOut) {
            android.util.Log.d("AuthViewModel", "State update skipped - logging out")
            return
        }
        
        if (result != null) {
            android.util.Log.d("AuthViewModel", "Setting state: $result")
            _authState.value = result
        } else {
            android.util.Log.e("AuthViewModel", "Timeout! Forcing NeedsCampus")
            _authState.value = AuthState.NeedsCampus
        }
    }

    fun loginOrRegister(email: String) {
        if (_authState.value is AuthState.Loading) return
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val alreadyVerified = repository.sendVerification(email)
                if (alreadyVerified) {
                    // Kullanıcı zaten doğrulanmış, direkt kontrol et
                    android.util.Log.d("AuthViewModel", "User already verified, checking status")
                    checkStatus()
                } else {
                    // Doğrulama e-postası gönderildi
                    _authState.value = AuthState.VerificationSent
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "İşlem başarısız.")
            }
        }
    }

    fun checkStatus() {
        if (isLoggingOut) return
        
        android.util.Log.d("AuthViewModel", "checkStatus called")
        val user = repository.currentUser.value
        if (user == null) {
            android.util.Log.d("AuthViewModel", "checkStatus: No user")
            _authState.value = AuthState.Unauthenticated
            return
        }
        viewModelScope.launch {
            performVerificationCheck(user)
        }
    }

    fun resendVerification() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = repository.currentUser.value
                user?.sendEmailVerification()?.await()
                _authState.value = AuthState.VerificationSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Yeniden gönderme başarısız.")
            }
        }
    }

    fun updateProfile(name: String, school: String, campus: String) {
        val user = repository.currentUser.value ?: return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val profileData = mapOf(
                    "name" to name,
                    "school" to school,
                    "campus" to campus,
                    "campusName" to campus,
                    "email" to user.email,
                    "balance" to 0.0,
                    "department" to "Öğrenci",
                    "rating" to 5.0,
                    "completedTasks" to 0,
                    "publishedTasks" to 0,
                    "badges" to emptyList<String>()
                )
                db.collection("users").document(user.uid)
                    .set(profileData, com.google.firebase.firestore.SetOptions.merge()).await()
                
                _authState.value = AuthState.Verified
                syncFcmToken()
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Profil güncellenemedi: ${e.message}")
            }
        }
    }

    /**
     * FCM Token'ını Firestore'a senkronize eder
     */
    fun syncFcmToken() {
        val user = repository.currentUser.value ?: return
        viewModelScope.launch {
            try {
                val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("users").document(user.uid)
                    .update("fcmToken", token).await()
                android.util.Log.d("AuthViewModel", "FCM Token synchronized")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Failed to sync FCM Token: ${e.message}")
            }
        }
    }

    fun logout() {
        android.util.Log.d("AuthViewModel", "Logout started")
        isLoggingOut = true // Diğer işlemleri durdur
        
        viewModelScope.launch {
            try {
                repository.logout()
                android.util.Log.d("AuthViewModel", "Logout complete")
            } finally {
                _authState.value = AuthState.Unauthenticated
                isLoggingOut = false
                android.util.Log.d("AuthViewModel", "State set to Unauthenticated")
            }
        }
    }

    fun resetState() {
        if (!isLoggingOut) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}
