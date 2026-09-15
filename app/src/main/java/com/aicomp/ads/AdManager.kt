package com.aicomp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Single place that owns interstitial ad state for the whole app.
 * Uses the real "Call End Interstitial" ad unit created in AdMob.
 */
object AdManager {

    private const val TAG = "AdManager"

    // Google's public test interstitial unit — safe to ship until you swap it.
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-9566533923400201/9268934555"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun init(context: Context) {
        MobileAds.initialize(context.applicationContext) {
            preload(context)
        }
    }

    /** Loads the next interstitial in the background so it's ready to show instantly. */
    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Shows the interstitial if one is ready, then always calls [onDone] —
     * whether the ad showed, failed, or wasn't ready — so the caller's
     * navigation never gets stuck waiting on an ad.
     */
    fun showIfReady(activity: Activity, onDone: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            preload(activity)
            onDone()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preload(activity)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preload(activity)
                onDone()
            }
        }
        ad.show(activity)
    }
}
