package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.repository.UserProfile
import com.acilnakit.data.repository.UserRepository
import com.acilnakit.data.repository.AuthRepository
import com.acilnakit.data.repository.ReviewRepository
import com.acilnakit.data.model.Review
import dagger.hilt.android.lifecycle.HiltViewModel
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress = _uploadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val profile: StateFlow<UserProfile?> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) flowOf<UserProfile?>(null)
        else userRepository.getUserProfile(user.uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reviews: StateFlow<List<Review>> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) flowOf<List<Review>>(emptyList())
        else reviewRepository.getUserReviews(user.uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateName(name: String) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            userRepository.updateName(userId, name)
        }
    }

    fun setCampus(campus: String) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            userRepository.updateCampus(userId, campus)
        }
    }
    
    
    fun uploadIdCard(uri: Uri) {
        val userId = authRepository.currentUser.value?.uid ?: return
        
        viewModelScope.launch {
            _uploadProgress.value = 0.01f
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("id_cards/$userId/id_card_${System.currentTimeMillis()}.jpg")
                
                storageRef.putFile(uri)
                    .addOnProgressListener { taskSnapshot: com.google.firebase.storage.UploadTask.TaskSnapshot ->
                        val progress = taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount
                        _uploadProgress.value = progress
                    }
                    .await()
                
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                // Firestore güncelle
                userRepository.updateIdCardImageUrl(userId, downloadUrl)
                
                _uploadProgress.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Yükleme hatası: ${e.message}"
                _uploadProgress.value = null
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun logout() {
        authRepository.logout()
    }
}
