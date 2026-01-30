package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.WithdrawalRequest
import com.acilnakit.data.repository.AuthRepository
import com.acilnakit.data.repository.WithdrawalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WithdrawalViewModel @Inject constructor(
    private val withdrawalRepository: WithdrawalRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val currentUserId: String? get() = authRepository.currentUser.value?.uid

    fun requestWithdrawal(amount: Double, iban: String, accountHolder: String) {
        val userId = currentUserId ?: return
        
        if (amount <= 0) {
            _error.value = "Geçersiz tutar."
            return
        }
        
        if (iban.length < 24) {
            _error.value = "Geçersiz IBAN formatı."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val request = WithdrawalRequest(
                    userId = userId,
                    amount = amount,
                    iban = iban,
                    accountHolder = accountHolder
                )
                withdrawalRepository.requestWithdrawal(request)
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Bir hata oluştu."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
