package com.aicomp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Reusable banner ad. Drop this at the bottom of a screen (e.g. below the
 * companion grid on Home, or above the message input on Chat).
 * Uses the real "home banners" ad unit created in AdMob.
 */
private const val TEST_BANNER_ID = "ca-app-pub-9566533923400201/3489994268"

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = TEST_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
