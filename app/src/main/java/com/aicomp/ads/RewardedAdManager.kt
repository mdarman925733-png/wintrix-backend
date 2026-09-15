package com.aicomp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Real AdMob "Rewarded" ad unit — "rewards" (Earn Coins).
 */
object RewardedAdManager {

    private const val TAG = "RewardedAdManager"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-9566533923400201/9967984734"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Shows the rewarded ad if one is ready. [onResult] fires exactly once
     * with true only if the user watched to completion and earned the
     * reward — false if no ad was ready, it failed, or they closed early.
     */
    fun showIfReady(activity: Activity, onResult: (earned: Boolean) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            preload(activity)
            onResult(false)
            return
        }

        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload(activity)
                onResult(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                preload(activity)
                onResult(false)
            }
        }

        ad.show(activity) { _: RewardItem ->
            earned = true
        }
    }
}
