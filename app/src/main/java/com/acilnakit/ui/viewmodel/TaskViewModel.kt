package com.acilnakit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acilnakit.data.model.Task
import com.acilnakit.data.repository.TaskRepository
import com.acilnakit.data.repository.AuthRepository
import com.acilnakit.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class SortOrder {
    NEWEST, PRICE_HIGH, PRICE_LOW
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    // Arama sorgusu
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Fiyat aralığı filtresi
    private val _minPrice = MutableStateFlow<Double?>(null)
    private val _maxPrice = MutableStateFlow<Double?>(null)
    val minPrice: StateFlow<Double?> = _minPrice
    val maxPrice: StateFlow<Double?> = _maxPrice

    // Optimized: Get user's school and fetch all tasks for that school
    val tasks: StateFlow<List<Task>> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else userRepository.getUserProfile(user.uid).flatMapLatest { profile ->
                val school = profile?.school ?: ""
                if (school.isEmpty()) flowOf(emptyList())
                else taskRepository.getSchoolTasks(school)
            }
        }
        .combine(_sortOrder) { taskList, order ->
            when (order) {
                SortOrder.NEWEST -> taskList.reversed()
                SortOrder.PRICE_HIGH -> taskList.sortedByDescending { it.rewardAmount }
                SortOrder.PRICE_LOW -> taskList.sortedBy { it.rewardAmount }
            }
        }
        .combine(_selectedCategory) { taskList, category ->
            if (category == null || category == "Hepsi") taskList
            else taskList.filter { it.category == category }
        }
        .combine(_searchQuery) { taskList, query ->
            if (query.isBlank()) taskList
            else taskList.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true) 
            }
        }
        .combine(_minPrice) { taskList, min ->
            if (min == null) taskList
            else taskList.filter { it.rewardAmount >= min }
        }
        .combine(_maxPrice) { taskList, max ->
            if (max == null) taskList
            else taskList.filter { it.rewardAmount <= max }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPriceRange(min: Double?, max: Double?) {
        _minPrice.value = min
        _maxPrice.value = max
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _searchQuery.value = ""
        _minPrice.value = null
        _maxPrice.value = null
        _sortOrder.value = SortOrder.NEWEST
    }

    // Kullanıcının Geçmişi
    val taskHistory: StateFlow<List<Task>> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else taskRepository.getTaskHistory(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Kullanıcının Aktif İşleri
    val activeTasks: StateFlow<List<Task>> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else taskRepository.getActiveTasks(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

