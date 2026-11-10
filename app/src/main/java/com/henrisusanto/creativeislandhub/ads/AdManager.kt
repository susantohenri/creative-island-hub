package com.henrisusanto.creativeislandhub.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.henrisusanto.creativeislandhub.analytics.AnalyticsLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdManager(private val context: Context) {

    private val TAG = "AdManager"
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isMobileAdsInitializeCalled = false
    private var currentRewardedAdUnitId: String? = null
    private var currentInterstitialAdUnitId: String? = null
    private var isInterstitialLoading = false
    private var copyCodeCounter = 0
    private var lastInterstitialShowTime = 0L

    companion object {
        private const val INTERSTITIAL_COOLDOWN_MS = 5 * 60 * 1000L // 5 menit cooldown
    }

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
            currentInterstitialAdUnitId?.let { loadInterstitialAd(it) }
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
        val screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
        AnalyticsLogger.logAdRequest(
            adUnitId = adUnitId,
            adFormat = "REWARDED",
            screen = screen
        )
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "RewardedAd failed to load: ${adError.message}")
                AnalyticsLogger.logAdFailed(
                    adUnitId = adUnitId,
                    adFormat = "REWARDED",
                    errorCode = adError.code,
                    screen = AnalyticsLogger.getCurrentScreen() ?: screen
                )
                rewardedAd = null
                _isRewardedAdReady.value = false
                _isAdLoading.value = false
                onFailed?.invoke(adError)
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "RewardedAd loaded successfully.")
                ad.setOnPaidEventListener { adValue ->
                    val adSource = ad.responseInfo.loadedAdapterResponseInfo?.adSourceName
                        ?: ad.responseInfo.mediationAdapterClassName
                    AnalyticsLogger.logAdImpression(
                        adUnitId = adUnitId,
                        adFormat = "REWARDED",
                        adSource = adSource,
                        screen = AnalyticsLogger.getCurrentScreen() ?: screen,
                        currency = adValue.currencyCode,
                        value = adValue.valueMicros / 1_000_000.0
                    )
                }

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
                override fun onAdClicked() {
                    Log.d(TAG, "RewardedAd clicked.")
                    AnalyticsLogger.logAdClick(
                        adUnitId = currentRewardedAdUnitId ?: "",
                        adFormat = "REWARDED",
                        screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
                    )
                }

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

    fun loadInterstitialAd(
        adUnitId: String,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        currentInterstitialAdUnitId = adUnitId
        if (!isMobileAdsInitializeCalled) {
            initializeMobileAds()
        }

        if (interstitialAd != null) {
            onLoaded?.invoke()
            return
        }

        if (isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        val screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
        AnalyticsLogger.logAdRequest(
            adUnitId = adUnitId,
            adFormat = "INTERSTITIAL",
            screen = screen
        )

        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.d(TAG, "InterstitialAd failed to load: ${loadAdError.message}")
                AnalyticsLogger.logAdFailed(
                    adUnitId = adUnitId,
                    adFormat = "INTERSTITIAL",
                    errorCode = loadAdError.code,
                    screen = AnalyticsLogger.getCurrentScreen() ?: screen
                )
                interstitialAd = null
                isInterstitialLoading = false
                onFailed?.invoke(loadAdError)
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "InterstitialAd loaded successfully.")
                ad.setOnPaidEventListener { adValue ->
                    val adSource = ad.responseInfo.loadedAdapterResponseInfo?.adSourceName
                        ?: ad.responseInfo.mediationAdapterClassName
                    AnalyticsLogger.logAdImpression(
                        adUnitId = adUnitId,
                        adFormat = "INTERSTITIAL",
                        adSource = adSource,
                        screen = AnalyticsLogger.getCurrentScreen() ?: screen,
                        currency = adValue.currencyCode,
                        value = adValue.valueMicros / 1_000_000.0
                    )
                }
                interstitialAd = ad
                isInterstitialLoading = false
                onLoaded?.invoke()
            }
        })
    }

    fun showInterstitialAd(
        activity: Activity,
        onAdClosed: (() -> Unit)? = null
    ) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(TAG, "InterstitialAd clicked.")
                    AnalyticsLogger.logAdClick(
                        adUnitId = currentInterstitialAdUnitId ?: "",
                        adFormat = "INTERSTITIAL",
                        screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
                    )
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "InterstitialAd dismissed.")
                    interstitialAd = null
                    onAdClosed?.invoke()
                    currentInterstitialAdUnitId?.let { loadInterstitialAd(it) }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "InterstitialAd failed to show: ${adError.message}")
                    interstitialAd = null
                    onAdClosed?.invoke()
                    currentInterstitialAdUnitId?.let { loadInterstitialAd(it) }
                }
            }
            ad.show(activity)
        } else {
            onAdClosed?.invoke()
        }
    }

    /**
     * Triggered every time the user copies a map code.
     * Shows an Interstitial Ad on every 3rd copy, respecting a 5-minute cooldown.
     */
    fun onCodeCopied(activity: Activity, interstitialAdUnitId: String?): Boolean {
        copyCodeCounter++
        Log.d(TAG, "Code copied. Counter: $copyCodeCounter")

        if (interstitialAdUnitId.isNullOrBlank()) return false

        if (copyCodeCounter >= 3) {
            val now = System.currentTimeMillis()
            val timeSinceLast = now - lastInterstitialShowTime
            if (timeSinceLast >= INTERSTITIAL_COOLDOWN_MS) {
                if (interstitialAd != null) {
                    Log.d(TAG, "Triggering Interstitial Ad after 3rd copy (cooldown satisfied).")
                    copyCodeCounter = 0
                    lastInterstitialShowTime = now
                    showInterstitialAd(activity)
                    return true
                } else {
                    // Preload for next time if not ready
                    loadInterstitialAd(interstitialAdUnitId)
                }
            } else {
                Log.d(TAG, "Interstitial on copy cooldown active: ${(INTERSTITIAL_COOLDOWN_MS - timeSinceLast) / 1000}s remaining.")
            }
        }
        return false
    }

    fun loadNativeAd(
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (!isMobileAdsInitializeCalled) {
            initializeMobileAds()
        }
        val builder = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "NativeAd loaded successfully.")
                nativeAd.setOnPaidEventListener { adValue ->
                    val adSource = nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName
                        ?: nativeAd.responseInfo?.mediationAdapterClassName
                    AnalyticsLogger.logAdImpression(
                        adUnitId = adUnitId,
                        adFormat = "NATIVE",
                        adSource = adSource,
                        screen = AnalyticsLogger.getCurrentScreen() ?: "unknown",
                        currency = adValue.currencyCode,
                        value = adValue.valueMicros / 1_000_000.0
                    )
                }
                onLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "NativeAd failed to load: ${loadAdError.message}")
                    onFailed?.invoke(loadAdError)
                }

                override fun onAdClicked() {
                    AnalyticsLogger.logAdClick(
                        adUnitId = adUnitId,
                        adFormat = "NATIVE",
                        screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
                    )
                }
            })
            .build()

        val screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
        AnalyticsLogger.logAdRequest(
            adUnitId = adUnitId,
            adFormat = "NATIVE",
            screen = screen
        )
        builder.loadAd(AdRequest.Builder().build())
    }
}
