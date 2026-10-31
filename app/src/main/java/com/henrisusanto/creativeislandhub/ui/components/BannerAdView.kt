package com.henrisusanto.creativeislandhub.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.henrisusanto.creativeislandhub.analytics.AnalyticsLogger

@Composable
fun BannerAdView(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            val screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId

                adListener = object : AdListener() {
                    override fun onAdClicked() {
                        super.onAdClicked()
                        AnalyticsLogger.logAdClick(
                            adUnitId = adUnitId,
                            adFormat = "BANNER",
                            screen = AnalyticsLogger.getCurrentScreen() ?: screen
                        )
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        AnalyticsLogger.logAdFailed(
                            adUnitId = adUnitId,
                            adFormat = "BANNER",
                            errorCode = loadAdError.code,
                            screen = AnalyticsLogger.getCurrentScreen() ?: screen
                        )
                    }
                }

                setOnPaidEventListener { adValue ->
                    val adSource = responseInfo?.loadedAdapterResponseInfo?.adSourceName
                        ?: responseInfo?.mediationAdapterClassName
                    AnalyticsLogger.logAdImpression(
                        adUnitId = adUnitId,
                        adFormat = "BANNER",
                        adSource = adSource,
                        screen = AnalyticsLogger.getCurrentScreen() ?: screen,
                        currency = adValue.currencyCode,
                        value = adValue.valueMicros / 1_000_000.0
                    )
                }

                AnalyticsLogger.logAdRequest(
                    adUnitId = adUnitId,
                    adFormat = "BANNER",
                    screen = screen
                )
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
