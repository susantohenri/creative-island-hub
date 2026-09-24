package com.henrisusanto.creativeislandhub.data.repository

import com.henrisusanto.creativeislandhub.data.local.PreferencesDataStore
import com.henrisusanto.creativeislandhub.data.model.AdsConfig
import com.henrisusanto.creativeislandhub.data.model.Island
import com.henrisusanto.creativeislandhub.data.network.RemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IslandRepository(
    private val remoteDataSource: RemoteDataSource,
    private val preferencesDataStore: PreferencesDataStore
) {
    private val _islands = MutableStateFlow<List<Island>>(emptyList())
    val islands: StateFlow<List<Island>> = _islands.asStateFlow()

    private val _adsConfig = MutableStateFlow(AdsConfig.DEFAULT)
    val adsConfig: StateFlow<AdsConfig> = _adsConfig.asStateFlow()

    val unlockedIslandsFlow: Flow<Set<String>> = preferencesDataStore.unlockedIslandsFlow
    val likedIslandsFlow: Flow<Set<String>> = preferencesDataStore.likedIslandsFlow
    val themeModeFlow: Flow<String> = preferencesDataStore.themeModeFlow
    val languageFlow: Flow<String> = preferencesDataStore.languageFlow

    suspend fun fetchIslands() {
        try {
            val response = remoteDataSource.getIslands()
            _islands.value = response.data
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun fetchAdsConfig() {
        try {
            val config = remoteDataSource.getAdsConfig()
            _adsConfig.value = config
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep using fallback default if fetching fails, app must not crash
        }
    }

    suspend fun unlockIsland(code: String) {
        preferencesDataStore.unlockIsland(code)
    }

    suspend fun toggleLike(code: String) {
        preferencesDataStore.toggleLikeIsland(code)
    }

    suspend fun setThemeMode(mode: String) {
        preferencesDataStore.setThemeMode(mode)
    }

    suspend fun setLanguage(lang: String) {
        preferencesDataStore.setLanguage(lang)
    }
}
