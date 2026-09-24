package com.henrisusanto.creativeislandhub.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AdsConfig(
    val appOpenAdUnitId: String? = null,
    val bannerAdUnitId: String? = null,
    val interstitialAdUnitId: String? = null,
    val rewardedAdUnitId: String? = null,
    val nativeAdUnitId: String? = null,
    val isAdsEnabled: Boolean = false
) {
    companion object {
        val DEFAULT = AdsConfig(
            bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111", // Test Banner
            rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917", // Test Rewarded
            isAdsEnabled = true
        )
    }
}
