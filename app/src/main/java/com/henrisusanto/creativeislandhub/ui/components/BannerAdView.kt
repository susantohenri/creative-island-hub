package com.henrisusanto.creativeislandhub.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val screenWidthDp = configuration.screenWidthDp

    // Calculate Anchored Adaptive Banner size matching screen width
    val adSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    // Pre-reserve banner height to prevent Cumulative Layout Shift (CLS)
    val bannerHeight = if (adSize.height > 0) adSize.height.dp else 50.dp

    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            adViewRef?.destroy()
            adViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                val screen = AnalyticsLogger.getCurrentScreen() ?: "unknown"
                AdView(ctx).apply {
                    setAdSize(adSize)
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
                    adViewRef = this
                }
            }
        )
    }
}
