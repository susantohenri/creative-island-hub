package com.henrisusanto.creativeislandhub.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henrisusanto.creativeislandhub.ads.AdManager
import com.henrisusanto.creativeislandhub.ads.AppOpenAdManager
import com.henrisusanto.creativeislandhub.analytics.AnalyticsLogger
import com.henrisusanto.creativeislandhub.data.model.AdsConfig
import com.henrisusanto.creativeislandhub.data.model.Island
import com.henrisusanto.creativeislandhub.data.repository.IslandRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainViewModel(
    private val repository: IslandRepository,
    val adManager: AdManager,
    val appOpenAdManager: AppOpenAdManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _unlockTargetCode = MutableStateFlow<String?>(null)
    val unlockTargetCode: StateFlow<String?> = _unlockTargetCode.asStateFlow()

    val adsConfig: StateFlow<AdsConfig> = repository.adsConfig
    val unlockedIslands: StateFlow<Set<String>> = repository.unlockedIslandsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    val likedIslands: StateFlow<Set<String>> = repository.likedIslandsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val themeMode: StateFlow<String> = repository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "SYSTEM")
    val language: StateFlow<String> = repository.languageFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "AUTO")

    private val debouncedSearchQuery = _searchQuery.debounce(300L)

    val filteredIslands: StateFlow<List<Island>> = combine(
        repository.islands,
        debouncedSearchQuery,
        _selectedCategory
    ) { islands, query, category ->
        val cleanQuery = query.trim()
        islands.filter { island ->
            val matchesQuery = cleanQuery.isEmpty() ||
                island.title.contains(cleanQuery, ignoreCase = true) ||
                island.code.contains(cleanQuery, ignoreCase = true) ||
                (island.creatorCode?.contains(cleanQuery, ignoreCase = true) == true) ||
                (island.category?.contains(cleanQuery, ignoreCase = true) == true) ||
                (island.createdIn?.contains(cleanQuery, ignoreCase = true) == true) ||
                island.tags.any { it.contains(cleanQuery, ignoreCase = true) }

            val matchesCategory = category == null ||
                island.category?.equals(category, ignoreCase = true) == true ||
                island.createdIn?.equals(category, ignoreCase = true) == true ||
                island.tags.any { it.equals(category, ignoreCase = true) }

            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCategories: StateFlow<List<String>> = repository.islands.map { islands ->
        islands.flatMap { island ->
            val list = mutableListOf<String>()
            island.category?.let { list.add(it) }
            island.createdIn?.let { list.add(it) }
            list.addAll(island.tags)
            list
        }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        fetchData()
        preloadAdsWhenReady()
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            debouncedSearchQuery.collect { query ->
                val trimmed = query.trim()
                if (trimmed.length >= 3) {
                    AnalyticsLogger.logSearch(trimmed)
                }
            }
        }
    }

    private fun preloadAdsWhenReady() {
        viewModelScope.launch {
            adsConfig.collect { config ->
                appOpenAdManager.adUnitId = config.appOpenAdUnitId
                appOpenAdManager.isAdsEnabled = config.isAdsEnabled
                if (config.isAdsEnabled) {
                    config.rewardedAdUnitId?.let { adManager.loadRewardedAd(it) }
                    config.interstitialAdUnitId?.let { adManager.loadInterstitialAd(it) }
                    config.appOpenAdUnitId?.let { appOpenAdManager.loadAd() }
                }
            }
        }
    }

    fun onCodeCopied(activity: android.app.Activity) {
        if (adsConfig.value.isAdsEnabled) {
            adManager.onCodeCopied(activity, adsConfig.value.interstitialAdUnitId)
        }
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

    fun requestUnlockIsland(code: String) {
        _unlockTargetCode.value = code
    }

    fun dismissUnlockDialog() {
        _unlockTargetCode.value = null
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

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            repository.setLanguage(lang)
        }
    }
}
