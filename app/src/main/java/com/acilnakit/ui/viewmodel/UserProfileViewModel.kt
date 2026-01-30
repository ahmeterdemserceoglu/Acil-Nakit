package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.Review
import com.acilnakit.data.repository.ReviewRepository
import com.acilnakit.data.repository.UserProfile
import com.acilnakit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _userId = MutableStateFlow<String?>(null)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val profile: StateFlow<UserProfile?> = _userId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else userRepository.getUserProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val reviews: StateFlow<List<Review>> = _userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else reviewRepository.getUserReviews(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadUser(userId: String) {
        _isLoading.value = true
        _userId.value = userId
        viewModelScope.launch {
            // Small delay to show loading state
            kotlinx.coroutines.delay(300)
            _isLoading.value = false
        }
    }
}
