package com.henrisusanto.creativeislandhub.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.henrisusanto.creativeislandhub.BuildConfig

object AnalyticsLogger {
    private const val TAG = "AnalyticsLogger"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var firebaseCrashlytics: FirebaseCrashlytics? = null

    @Volatile
    private var lastLoggedScreen: String? = null

    /**
     * Initializes Firebase Analytics and Crashlytics with Application context.
     * Configures default event parameters (app_version) and enables data collection.
     */
    fun initialize(context: Context) {
        try {
            val appContext = context.applicationContext

            // 1. Firebase Analytics
            val analytics = FirebaseAnalytics.getInstance(appContext)
            analytics.setAnalyticsCollectionEnabled(true)
            val defaultParams = bundleOf(
                "app_version" to BuildConfig.VERSION_NAME
            )
            analytics.setDefaultEventParameters(defaultParams)
            firebaseAnalytics = analytics

            // 2. Firebase Crashlytics
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            firebaseCrashlytics = crashlytics

            Log.d(TAG, "Analytics & Crashlytics initialized. app_version=${BuildConfig.VERSION_NAME}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AnalyticsLogger", e)
        }
    }

    /**
     * Logs screen_view event with anti-duplication protection.
     * Follows Firebase official parameters: SCREEN_NAME and SCREEN_CLASS.
     */
    fun logScreenView(screenName: String, screenClass: String? = "MainActivity") {
        if (screenName.isBlank()) return
        if (lastLoggedScreen == screenName) {
            // Anti-duplication: skip if screen has not changed
            return
        }

        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                if (!screenClass.isNullOrBlank()) {
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
                }
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            lastLoggedScreen = screenName
            Log.d(TAG, "Screen view logged: $screenName ($screenClass)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log screen view: $screenName", e)
        }
    }

    /**
     * Logs custom or standard Firebase event safely.
     */
    fun logEvent(name: String, params: Bundle? = null) {
        if (name.isBlank()) return
        try {
            firebaseAnalytics?.logEvent(name, params)
            Log.d(TAG, "Event logged: $name, params: $params")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event: $name", e)
        }
    }

    /**
     * Sets user property safely.
     */
    fun setUserProperty(name: String, value: String?) {
        if (name.isBlank()) return
        try {
            firebaseAnalytics?.setUserProperty(name, value)
            Log.d(TAG, "User property set: $name = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property: $name", e)
        }
    }

    /**
     * Logs non-fatal exceptions to Crashlytics safely.
     */
    fun recordException(throwable: Throwable) {
        try {
            firebaseCrashlytics?.recordException(throwable)
            Log.d(TAG, "Exception recorded to Crashlytics: ${throwable.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exception", e)
        }
    }

    /**
     * Sets custom key in Crashlytics safely.
     */
    fun setCrashlyticsCustomKey(key: String, value: String) {
        if (key.isBlank()) return
        try {
            firebaseCrashlytics?.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Crashlytics key: $key", e)
        }
    }

    /**
     * Retrieves current logged screen for state inspection.
     */
    fun getCurrentScreen(): String? = lastLoggedScreen

    /**
     * Logs ad_request event right before calling loadAd.
     */
    fun logAdRequest(adUnitId: String, adFormat: String, screen: String) {
        if (adUnitId.isBlank()) return
        try {
            val bundle = Bundle().apply {
                putString("ad_unit_id", adUnitId)
                putString("ad_format", adFormat)
                putString("screen", screen)
            }
            firebaseAnalytics?.logEvent("ad_request", bundle)
            Log.d(TAG, "Ad request logged: format=$adFormat, unit=$adUnitId, screen=$screen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log ad_request", e)
        }
    }

    /**
     * Logs ad_impression event with revenue from OnPaidEventListener.
     */
    fun logAdImpression(
        adUnitId: String,
        adFormat: String,
        adSource: String?,
        screen: String,
        currency: String?,
        value: Double?
    ) {
        if (adUnitId.isBlank()) return
        try {
            val bundle = Bundle().apply {
                putString("ad_unit_id", adUnitId)
                putString("ad_format", adFormat)
                if (!adSource.isNullOrBlank()) {
                    putString("ad_source", adSource)
                }
                putString("screen", screen)
                if (!currency.isNullOrBlank()) {
                    putString("currency", currency)
                }
                if (value != null) {
                    putDouble("value", value)
                }
            }
            firebaseAnalytics?.logEvent("ad_impression", bundle)
            Log.d(TAG, "Ad impression logged: format=$adFormat, unit=$adUnitId, value=$value $currency, source=$adSource, screen=$screen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log ad_impression", e)
        }
    }

    /**
     * Logs ad_click event when ad click callback is received.
     */
    fun logAdClick(adUnitId: String, adFormat: String, screen: String) {
        if (adUnitId.isBlank()) return
        try {
            val bundle = Bundle().apply {
                putString("ad_unit_id", adUnitId)
                putString("ad_format", adFormat)
                putString("screen", screen)
            }
            firebaseAnalytics?.logEvent("ad_click", bundle)
            Log.d(TAG, "Ad click logged: format=$adFormat, unit=$adUnitId, screen=$screen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log ad_click", e)
        }
    }

    /**
     * Logs ad_failed event when ad load fails.
     */
    fun logAdFailed(adUnitId: String, adFormat: String, errorCode: Int, screen: String) {
        if (adUnitId.isBlank()) return
        try {
            val bundle = Bundle().apply {
                putString("ad_unit_id", adUnitId)
                putString("ad_format", adFormat)
                putInt("error_code", errorCode)
                putString("screen", screen)
            }
            firebaseAnalytics?.logEvent("ad_failed", bundle)
            Log.d(TAG, "Ad failed logged: format=$adFormat, unit=$adUnitId, error=$errorCode, screen=$screen")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log ad_failed", e)
        }
    }
}
