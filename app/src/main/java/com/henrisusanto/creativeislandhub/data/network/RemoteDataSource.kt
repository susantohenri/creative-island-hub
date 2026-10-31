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

                // In-app default fallback parameters
                val defaults = mapOf<String, Any>(
                    "is_ads_enabled" to AdsConfig.DEFAULT.isAdsEnabled,
                    "banner_ad_unit_id" to (AdsConfig.DEFAULT.bannerAdUnitId ?: ""),
                    "rewarded_ad_unit_id" to (AdsConfig.DEFAULT.rewardedAdUnitId ?: ""),
                    "interstitial_ad_unit_id" to (AdsConfig.DEFAULT.interstitialAdUnitId ?: ""),
                    "app_open_ad_unit_id" to (AdsConfig.DEFAULT.appOpenAdUnitId ?: ""),
                    "native_ad_unit_id" to (AdsConfig.DEFAULT.nativeAdUnitId ?: "")
                )
                remoteConfig.setDefaultsAsync(defaults)

                remoteConfig.fetchAndActivate().addOnCompleteListener { _ ->
                    try {
                        if (continuation.isCancelled) return@addOnCompleteListener

                        // 1. Check if complete JSON blob "ads_config" is provided in console
                        val jsonConfig = remoteConfig.getString("ads_config")
                        if (jsonConfig.isNotBlank() && jsonConfig != "{}") {
                            val parsed = json.decodeFromString<AdsConfig>(jsonConfig)
                            continuation.resume(parsed)
                            return@addOnCompleteListener
                        }

                        // 2. Otherwise read individual Remote Config parameters
                        val isAdsEnabled = if (remoteConfig.getString("is_ads_enabled").isNotBlank()) {
                            remoteConfig.getBoolean("is_ads_enabled")
                        } else {
                            AdsConfig.DEFAULT.isAdsEnabled
                        }

                        val banner = remoteConfig.getString("banner_ad_unit_id").ifBlank { AdsConfig.DEFAULT.bannerAdUnitId }
                        val rewarded = remoteConfig.getString("rewarded_ad_unit_id").ifBlank { AdsConfig.DEFAULT.rewardedAdUnitId }
                        val interstitial = remoteConfig.getString("interstitial_ad_unit_id").ifBlank { AdsConfig.DEFAULT.interstitialAdUnitId }
                        val appOpen = remoteConfig.getString("app_open_ad_unit_id").ifBlank { AdsConfig.DEFAULT.appOpenAdUnitId }
                        val native = remoteConfig.getString("native_ad_unit_id").ifBlank { AdsConfig.DEFAULT.nativeAdUnitId }

                        val config = AdsConfig(
                            appOpenAdUnitId = appOpen,
                            bannerAdUnitId = banner,
                            interstitialAdUnitId = interstitial,
                            rewardedAdUnitId = rewarded,
                            nativeAdUnitId = native,
                            isAdsEnabled = isAdsEnabled
                        )
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
