package com.henrisusanto.creativeislandhub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henrisusanto.creativeislandhub.ads.AdManager
import com.henrisusanto.creativeislandhub.data.model.AdsConfig
import com.henrisusanto.creativeislandhub.data.model.Island
import com.henrisusanto.creativeislandhub.data.repository.IslandRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: IslandRepository,
    val adManager: AdManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val adsConfig: StateFlow<AdsConfig> = repository.adsConfig
    val unlockedIslands = repository.unlockedIslandsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    val likedIslands = repository.likedIslandsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val filteredIslands = combine(
        repository.islands,
        _searchQuery,
        _selectedCategory
    ) { islands, query, category ->
        islands.filter { island ->
            val matchesQuery = query.isEmpty() || 
                island.title.contains(query, ignoreCase = true) ||
                island.code.contains(query, ignoreCase = true) ||
                island.tags.any { it.contains(query, ignoreCase = true) }
            
            val matchesCategory = category == null || 
                island.category?.equals(category, ignoreCase = true) == true ||
                island.tags.any { it.equals(category, ignoreCase = true) }
                
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val allCategories = repository.islands.mapState { islands ->
        islands.flatMap { 
            val cats = mutableListOf<String>()
            it.category?.let { c -> cats.add(c) }
            cats.addAll(it.tags)
            cats
        }.distinct().sorted()
    }

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.fetchAdsConfig()
                repository.fetchIslands()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown Error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun unlockIsland(code: String) {
        viewModelScope.launch {
            repository.unlockIsland(code)
        }
    }

    fun toggleLike(code: String) {
        viewModelScope.launch {
            repository.toggleLike(code)
        }
    }
    
    private fun <T, M> StateFlow<T>.mapState(mapper: (value: T) -> M): StateFlow<M> {
        val flow = MutableStateFlow(mapper(this.value))
        viewModelScope.launch {
            this@mapState.collect { flow.value = mapper(it) }
        }
        return flow
    }
}
