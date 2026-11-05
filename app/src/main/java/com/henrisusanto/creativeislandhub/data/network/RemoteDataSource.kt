package com.henrisusanto.creativeislandhub.data.network

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.henrisusanto.creativeislandhub.BuildConfig
import com.henrisusanto.creativeislandhub.data.model.AdsConfig
import com.henrisusanto.creativeislandhub.data.model.IslandDataResponse
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class RemoteDataSource(private val client: OkHttpClient) {
    
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getIslands(): IslandDataResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/susantohenri/creative-island-hub/refs/heads/main/content/data.json")
            .build()
        
        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to load islands: ${response.code}")
        
        val body = response.body?.string() ?: throw Exception("Empty response body")
        json.decodeFromString<IslandDataResponse>(body)
    }

    suspend fun getAdsConfig(): AdsConfig = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
                    .build()
                remoteConfig.setConfigSettingsAsync(configSettings)

                // In-app default fallback parameters (support both snake_case and camelCase)
                val defaults = mapOf<String, Any>(
                    "is_ads_enabled" to AdsConfig.DEFAULT.isAdsEnabled,
                    "isAdsEnabled" to AdsConfig.DEFAULT.isAdsEnabled,
                    "banner_ad_unit_id" to (AdsConfig.DEFAULT.bannerAdUnitId ?: ""),
                    "bannerAdUnitId" to (AdsConfig.DEFAULT.bannerAdUnitId ?: ""),
                    "rewarded_ad_unit_id" to (AdsConfig.DEFAULT.rewardedAdUnitId ?: ""),
                    "rewardedAdUnitId" to (AdsConfig.DEFAULT.rewardedAdUnitId ?: ""),
                    "interstitial_ad_unit_id" to (AdsConfig.DEFAULT.interstitialAdUnitId ?: ""),
                    "interstitialAdUnitId" to (AdsConfig.DEFAULT.interstitialAdUnitId ?: ""),
                    "app_open_ad_unit_id" to (AdsConfig.DEFAULT.appOpenAdUnitId ?: ""),
                    "appOpenAdUnitId" to (AdsConfig.DEFAULT.appOpenAdUnitId ?: ""),
                    "native_ad_unit_id" to (AdsConfig.DEFAULT.nativeAdUnitId ?: ""),
                    "nativeAdUnitId" to (AdsConfig.DEFAULT.nativeAdUnitId ?: "")
                )
                remoteConfig.setDefaultsAsync(defaults)

                remoteConfig.fetchAndActivate().addOnCompleteListener { _ ->
                    try {
                        if (continuation.isCancelled) return@addOnCompleteListener

                        // 1. Check if complete JSON blob "ads_config" or "adsConfig" is provided in console
                        val jsonConfig = remoteConfig.getString("ads_config").ifBlank {
                            remoteConfig.getString("adsConfig")
                        }
                        if (jsonConfig.isNotBlank() && jsonConfig != "{}") {
                            val parsed = json.decodeFromString<AdsConfig>(jsonConfig)
                            Log.d("RemoteDataSource", "Loaded AdsConfig from JSON blob: $parsed")
                            continuation.resume(parsed)
                            return@addOnCompleteListener
                        }

                        // Helper to get string value supporting snake_case or camelCase
                        fun getStringConfig(snakeKey: String, camelKey: String): String {
                            val snakeVal = remoteConfig.getValue(snakeKey)
                            if (snakeVal.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                                return snakeVal.asString()
                            }
                            val camelVal = remoteConfig.getValue(camelKey)
                            if (camelVal.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                                return camelVal.asString()
                            }
                            // Fallback to whichever is non-blank (e.g. from in-app defaults)
                            val snakeStr = snakeVal.asString()
                            if (snakeStr.isNotBlank()) return snakeStr
                            return camelVal.asString()
                        }

                        // Helper to get boolean value supporting snake_case or camelCase
                        fun getBooleanConfig(snakeKey: String, camelKey: String, default: Boolean): Boolean {
                            val snakeVal = remoteConfig.getValue(snakeKey)
                            if (snakeVal.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                                return snakeVal.asBoolean()
                            }
                            val camelVal = remoteConfig.getValue(camelKey)
                            if (camelVal.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                                return camelVal.asBoolean()
                            }
                            return default
                        }

                        // 2. Read individual Remote Config parameters with dual snake_case & camelCase support
                        val isAdsEnabled = getBooleanConfig("is_ads_enabled", "isAdsEnabled", AdsConfig.DEFAULT.isAdsEnabled)
                        val banner = getStringConfig("banner_ad_unit_id", "bannerAdUnitId").ifBlank { AdsConfig.DEFAULT.bannerAdUnitId }
                        val rewarded = getStringConfig("rewarded_ad_unit_id", "rewardedAdUnitId").ifBlank { AdsConfig.DEFAULT.rewardedAdUnitId }
                        val interstitial = getStringConfig("interstitial_ad_unit_id", "interstitialAdUnitId").ifBlank { AdsConfig.DEFAULT.interstitialAdUnitId }
                        val appOpen = getStringConfig("app_open_ad_unit_id", "appOpenAdUnitId").ifBlank { AdsConfig.DEFAULT.appOpenAdUnitId }
                        val native = getStringConfig("native_ad_unit_id", "nativeAdUnitId").ifBlank { AdsConfig.DEFAULT.nativeAdUnitId }

                        val config = AdsConfig(
                            appOpenAdUnitId = appOpen,
                            bannerAdUnitId = banner,
                            interstitialAdUnitId = interstitial,
                            rewardedAdUnitId = rewarded,
                            nativeAdUnitId = native,
                            isAdsEnabled = isAdsEnabled
                        )
                        Log.d("RemoteDataSource", "Loaded AdsConfig from Remote Config params: $config")
                        continuation.resume(config)
                    } catch (e: Exception) {
                        Log.e("RemoteDataSource", "Failed to parse remote config, fallback to default", e)
                        if (!continuation.isCancelled) {
                            continuation.resume(AdsConfig.DEFAULT)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteDataSource", "Failed to initialize Remote Config fetch", e)
                if (!continuation.isCancelled) {
                    continuation.resume(AdsConfig.DEFAULT)
                }
            }
        }
    }
}
