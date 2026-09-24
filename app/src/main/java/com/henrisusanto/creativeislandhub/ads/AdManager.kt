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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdManager(private val context: Context) {

    private val TAG = "AdManager"
    private var rewardedAd: RewardedAd? = null
    private var isMobileAdsInitializeCalled = false
    private var currentRewardedAdUnitId: String? = null

    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    fun requestConsentAndInit(activity: Activity, onConsentGathered: () -> Unit) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        Log.w(TAG, "Consent form error: ${loadAndShowError.errorCode} - ${loadAndShowError.message}")
                    }
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAds()
                    }
                    onConsentGathered()
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent request error: ${requestConsentError.errorCode} - ${requestConsentError.message}")
                if (consentInformation.canRequestAds()) {
                    initializeMobileAds()
                }
                onConsentGathered()
            }
        )

        if (consentInformation.canRequestAds()) {
            initializeMobileAds()
        }
    }

    fun initializeMobileAds() {
        if (isMobileAdsInitializeCalled) return
        isMobileAdsInitializeCalled = true
        MobileAds.initialize(context) {
            Log.d(TAG, "MobileAds initialized.")
            // If ad unit ID was already set before init completed, trigger preload
            currentRewardedAdUnitId?.let { loadRewardedAd(it) }
        }
    }

    fun loadRewardedAd(
        adUnitId: String,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        currentRewardedAdUnitId = adUnitId
        if (!isMobileAdsInitializeCalled) {
            initializeMobileAds()
        }

        if (rewardedAd != null) {
            _isRewardedAdReady.value = true
            onLoaded?.invoke()
            return
        }

        if (_isAdLoading.value) return

        _isAdLoading.value = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "RewardedAd failed to load: ${adError.message}")
                rewardedAd = null
                _isRewardedAdReady.value = false
                _isAdLoading.value = false
                onFailed?.invoke(adError)
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "RewardedAd loaded successfully.")
                rewardedAd = ad
                _isRewardedAdReady.value = true
                _isAdLoading.value = false
                onLoaded?.invoke()
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "RewardedAd dismissed.")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    onAdClosed()
                    // Preload the next ad automatically
                    currentRewardedAdUnitId?.let { loadRewardedAd(it) }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "RewardedAd failed to show: ${adError.message}")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    onAdClosed()
                    currentRewardedAdUnitId?.let { loadRewardedAd(it) }
                }
            }

            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned()
            }
        } else {
            Log.d(TAG, "Rewarded ad was not ready yet.")
            onAdNotReady()
        }
    }

    fun loadAndShowRewardedAd(
        activity: Activity,
        adUnitId: String,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (_isRewardedAdReady.value && rewardedAd != null) {
            showRewardedAd(
                activity = activity,
                onRewardEarned = onRewardEarned,
                onAdClosed = onAdClosed,
                onAdNotReady = {
                    onFailed("Ad not ready")
                }
            )
            return
        }

        loadRewardedAd(
            adUnitId = adUnitId,
            onLoaded = {
                showRewardedAd(
                    activity = activity,
                    onRewardEarned = onRewardEarned,
                    onAdClosed = onAdClosed,
                    onAdNotReady = {
                        onFailed("Ad not ready")
                    }
                )
            },
            onFailed = { error ->
                onFailed(error.message)
            }
        )
    }
}
