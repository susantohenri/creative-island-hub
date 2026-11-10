package com.henrisusanto.creativeislandhub.ui.components

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.henrisusanto.creativeislandhub.R
import com.henrisusanto.creativeislandhub.ads.AdManager

@Composable
fun NativeAdCard(
    adUnitId: String,
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadedNativeAd by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(adUnitId) {
        adManager.loadNativeAd(
            adUnitId = adUnitId,
            onLoaded = { ad ->
                loadedNativeAd = ad
            },
            onFailed = {
                loadedNativeAd = null
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            loadedNativeAd?.destroy()
        }
    }

    val nativeAd = loadedNativeAd ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(R.layout.item_native_ad, null)
                val nativeAdView = view.findViewById<NativeAdView>(R.id.native_ad_view)

                val headline = view.findViewById<TextView>(R.id.ad_headline)
                val body = view.findViewById<TextView>(R.id.ad_body)
                val callToAction = view.findViewById<Button>(R.id.ad_call_to_action)
                val appIcon = view.findViewById<ImageView>(R.id.ad_app_icon)
                val mediaView = view.findViewById<MediaView>(R.id.ad_media)
                val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)

                nativeAdView.headlineView = headline
                nativeAdView.bodyView = body
                nativeAdView.callToActionView = callToAction
                nativeAdView.mediaView = mediaView
                nativeAdView.iconView = appIcon
                nativeAdView.advertiserView = advertiser

                bindNativeAd(nativeAdView, nativeAd)
                nativeAdView
            },
            update = { nativeAdView ->
                bindNativeAd(nativeAdView, nativeAd)
            }
        )
    }
}

private fun bindNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

    if (nativeAd.body == null) {
        nativeAdView.bodyView?.visibility = View.GONE
    } else {
        nativeAdView.bodyView?.visibility = View.VISIBLE
        (nativeAdView.bodyView as? TextView)?.text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
        nativeAdView.callToActionView?.visibility = View.GONE
    } else {
        nativeAdView.callToActionView?.visibility = View.VISIBLE
        (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        nativeAdView.iconView?.visibility = View.GONE
    } else {
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        nativeAdView.iconView?.visibility = View.VISIBLE
    }

    if (nativeAd.mediaContent != null) {
        nativeAdView.mediaView?.visibility = View.VISIBLE
    } else {
        nativeAdView.mediaView?.visibility = View.GONE
    }

    if (nativeAd.advertiser == null) {
        nativeAdView.advertiserView?.visibility = View.GONE
    } else {
        (nativeAdView.advertiserView as? TextView)?.text = nativeAd.advertiser
        nativeAdView.advertiserView?.visibility = View.VISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}
