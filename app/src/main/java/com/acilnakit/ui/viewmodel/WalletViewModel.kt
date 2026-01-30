package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.Transaction
import com.acilnakit.data.repository.WalletRepository
import com.acilnakit.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletState>(WalletState.Idle)
    val uiState: StateFlow<WalletState> = _uiState

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl

    val balance: StateFlow<Double> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(0.0)
            else walletRepository.getBalance(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    val escrowBalance: StateFlow<Double> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(0.0)
            else walletRepository.getEscrowBalance(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val transactions: StateFlow<List<Transaction>> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else walletRepository.getTransactions(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * iyzico ile ödeme başlat
     */
    fun initiatePayment(amount: Double) {
        viewModelScope.launch {
            _uiState.value = WalletState.Loading
            try {
                val result = walletRepository.initiatePayment(amount)
                if (result.success && result.paymentUrl != null) {
                    _paymentUrl.value = result.paymentUrl
                    _uiState.value = WalletState.Success("Ödeme sayfasına yönlendiriliyorsunuz...")
                } else {
                    _uiState.value = WalletState.Error(result.errorMessage ?: "Ödeme başlatılamadı")
                }
            } catch (e: Exception) {
                _uiState.value = WalletState.Error(e.message ?: "Ödeme başlatılamadı")
            }
        }
    }

    fun clearPaymentUrl() {
        _paymentUrl.value = null
    }

    /**
     * Para çekme talebi
     */
    fun withdraw(amount: Double, method: String, details: String) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _uiState.value = WalletState.Loading
            try {
                walletRepository.requestWithdrawal(userId, amount, method, details)
                _uiState.value = WalletState.Success("Para çekme talebiniz alındı. 1-3 iş günü içinde işlenecektir.")
            } catch (e: Exception) {
                _uiState.value = WalletState.Error(e.message ?: "İşlem başarısız.")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = WalletState.Idle
    }

    fun releaseEscrow(taskId: String, amount: Double) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            walletRepository.releaseEscrow(userId, amount, taskId)
        }
    }
}

sealed class WalletState {
    object Idle : WalletState()
    object Loading : WalletState()
    data class Success(val message: String) : WalletState()
    data class Error(val message: String) : WalletState()
}
