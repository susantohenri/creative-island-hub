package com.henrisusanto.creativeislandhub.data.network

import com.henrisusanto.creativeislandhub.data.model.AdsConfig
import com.henrisusanto.creativeislandhub.data.model.IslandDataResponse
import kotlinx.coroutines.Dispatchers
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
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/creativeIslandHub/ads_config.json")
            .build()
        
        val response: Response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to load ads config: ${response.code}")
        
        val body = response.body?.string() ?: throw Exception("Empty response body")
        json.decodeFromString<AdsConfig>(body)
    }
}
