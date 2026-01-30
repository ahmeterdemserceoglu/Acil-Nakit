package com.acilnakit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        auth.addAuthStateListener {
            _currentUser.value = it.currentUser
        }
    }

    fun isCampusEmail(email: String): Boolean {
        return email.endsWith(".edu.tr") || email.endsWith(".edu")
    }

    suspend fun sendVerification(email: String): Boolean {
        if (!isCampusEmail(email)) throw Exception("Geçerli bir kampüs e-postası giriniz.")
        
        // Önce giriş yapmayı veya hesap oluşturmayı dene
        try {
            auth.createUserWithEmailAndPassword(email, "AcilNakit123!").await()
        } catch (e: Exception) {
            // Kullanıcı zaten var, giriş yap
            auth.signInWithEmailAndPassword(email, "AcilNakit123!").await()
        }
        
        // Kullanıcı zaten doğrulanmış mı kontrol et
        auth.currentUser?.let { user ->
            user.reload().await()
            if (user.isEmailVerified) {
                // Zaten doğrulanmış, e-posta gönderme
                user.getIdToken(true).await()
                return true // true = zaten doğrulanmış
            }
        }
        
        // Doğrulanmamış, e-posta gönder
        auth.currentUser?.sendEmailVerification()?.await()
        return false // false = doğrulama e-postası gönderildi
    }


    suspend fun checkVerificationStatus(): Boolean {
        auth.currentUser?.let { user ->
            user.reload().await()
            if (user.isEmailVerified) {
                // Force refresh token to update claims (like email_verified) for Firestore rules
                user.getIdToken(true).await()
                return true
            }
        }
        return false
    }

    fun logout() {
        auth.signOut()
    }
}
