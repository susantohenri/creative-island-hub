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
            appOpenAdUnitId = "ca-app-pub-3360913653749022/7252831641",
            bannerAdUnitId = "ca-app-pub-3360913653749022/1199307619",
            interstitialAdUnitId = "ca-app-pub-3360913653749022/8071089297",
            rewardedAdUnitId = "ca-app-pub-3360913653749022/3278549739",
            nativeAdUnitId = "ca-app-pub-3360913653749022/2747444572",
            isAdsEnabled = true
        )
    }
}
