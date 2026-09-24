package com.henrisusanto.creativeislandhub.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class AdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isMobileAdsInitializeCalled = false

    fun requestConsentAndInit(activity: Activity, onConsentGathered: () -> Unit) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        
        // Uncomment & modify for testing in EEA
        // val debugSettings = ConsentDebugSettings.Builder(activity)
        //     .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        //     .addTestDeviceHashedId("YOUR_DEVICE_HASH")
        //     .build()
        // val params = ConsentRequestParameters.Builder()
        //     .setConsentDebugSettings(debugSettings)
        //     .build()

        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w("AdManager", "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAds()
                    }
                    onConsentGathered()
                }
            },
            { requestConsentError ->
                Log.w("AdManager", "${requestConsentError.errorCode}: ${requestConsentError.message}")
                onConsentGathered()
            }
        )

        // Check if you can initialize in parallel
        if (consentInformation.canRequestAds()) {
            initializeMobileAds()
        }
    }

    private fun initializeMobileAds() {
        if (isMobileAdsInitializeCalled) return
        isMobileAdsInitializeCalled = true
        MobileAds.initialize(context)
    }

    fun loadRewardedAd(adUnitId: String) {
        if (!isMobileAdsInitializeCalled) return
        if (rewardedAd != null) return // Already loaded

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdManager", adError.toString())
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d("AdManager", "Ad was loaded.")
                rewardedAd = ad
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdManager", "Ad was dismissed.")
                    rewardedAd = null
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("AdManager", "Ad failed to show.")
                    rewardedAd = null
                    onAdClosed()
                }
            }

            rewardedAd?.show(activity) { rewardItem ->
                Log.d("AdManager", "User earned the reward.")
                onRewardEarned()
            }
        } else {
            Log.d("AdManager", "The rewarded ad wasn't ready yet.")
            onAdClosed()
        }
    }
}
