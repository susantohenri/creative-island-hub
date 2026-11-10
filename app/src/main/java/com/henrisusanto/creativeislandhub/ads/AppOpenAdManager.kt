package com.henrisusanto.creativeislandhub.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.UserMessagingPlatform
import com.henrisusanto.creativeislandhub.analytics.AnalyticsLogger
import java.util.Date

class AppOpenAdManager(
    private val application: Application
) : Application.ActivityLifecycleCallbacks {

    private val TAG = "AppOpenAdManager"

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false
        private set

    private var loadTime: Long = 0
    private var lastShowTime: Long = 0
    private var currentActivity: Activity? = null
    private var startedActivityCount = 0

    var adUnitId: String? = null
    var isAdsEnabled: Boolean = false

    companion object {
        // Frequency cap: minimum 4 hours between app open impressions
        private const val FREQUENCY_CAP_MS = 4 * 60 * 60 * 1000L
        // Google AdMob App Open Ads expire after 4 hours
        private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun loadAd() {
        val unitId = adUnitId
        if (!isAdsEnabled || unitId.isNullOrBlank()) return
        if (isLoadingAd || isAdAvailable()) return

        val consentInfo = UserMessagingPlatform.getConsentInformation(application)
        if (!consentInfo.canRequestAds()) {
            Log.d(TAG, "Cannot request AppOpenAd: Consent not gathered yet.")
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AnalyticsLogger.logAdRequest(
            adUnitId = unitId,
            adFormat = "APP_OPEN",
            screen = AnalyticsLogger.getCurrentScreen() ?: "app_open"
        )

        AppOpenAd.load(
            application,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "AppOpenAd loaded successfully.")
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time

                    ad.setOnPaidEventListener { adValue ->
                        val adSource = ad.responseInfo.loadedAdapterResponseInfo?.adSourceName
                            ?: ad.responseInfo.mediationAdapterClassName
                        AnalyticsLogger.logAdImpression(
                            adUnitId = unitId,
                            adFormat = "APP_OPEN",
                            adSource = adSource,
                            screen = AnalyticsLogger.getCurrentScreen() ?: "app_open",
                            currency = adValue.currencyCode,
                            value = adValue.valueMicros / 1_000_000.0
                        )
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "AppOpenAd failed to load: ${loadAdError.message}")
                    AnalyticsLogger.logAdFailed(
                        adUnitId = unitId,
                        adFormat = "APP_OPEN",
                        errorCode = loadAdError.code,
                        screen = AnalyticsLogger.getCurrentScreen() ?: "app_open"
                    )
                    isLoadingAd = false
                    appOpenAd = null
                }
            }
        )
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanHoursAgo(AD_TIMEOUT_MS)
    }

    private fun wasLoadTimeLessThanHoursAgo(hoursMs: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        return dateDifference < hoursMs
    }

    private fun isFrequencyCapSatisfied(): Boolean {
        if (lastShowTime == 0L) return true
        val elapsed = Date().time - lastShowTime
        val satisfied = elapsed >= FREQUENCY_CAP_MS
        if (!satisfied) {
            Log.d(TAG, "AppOpenAd frequency capped: ${(FREQUENCY_CAP_MS - elapsed) / 1000}s remaining.")
        }
        return satisfied
    }

    fun showAdIfAvailable(activity: Activity) {
        if (isShowingAd) {
            Log.d(TAG, "AppOpenAd already showing.")
            return
        }

        if (!isFrequencyCapSatisfied()) {
            return
        }

        if (!isAdAvailable()) {
            Log.d(TAG, "AppOpenAd not available yet, triggering load.")
            loadAd()
            return
        }

        val ad = appOpenAd ?: return
        val unitId = adUnitId ?: ""

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "AppOpenAd dismissed.")
                appOpenAd = null
                isShowingAd = false
                lastShowTime = Date().time
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "AppOpenAd failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "AppOpenAd showed full screen content.")
                isShowingAd = true
            }

            override fun onAdClicked() {
                Log.d(TAG, "AppOpenAd clicked.")
                AnalyticsLogger.logAdClick(
                    adUnitId = unitId,
                    adFormat = "APP_OPEN",
                    screen = AnalyticsLogger.getCurrentScreen() ?: "app_open"
                )
            }
        }

        isShowingAd = true
        ad.show(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        startedActivityCount++
        if (startedActivityCount == 1 && !activity.isChangingConfigurations) {
            // App entered foreground from background or fresh launch
            showAdIfAvailable(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }
}
