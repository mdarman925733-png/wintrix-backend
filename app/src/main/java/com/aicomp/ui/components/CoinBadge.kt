package com.aicomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicomp.ads.RewardedAdManager
import com.aicomp.data.repository.AuthRepository
import com.aicomp.data.repository.CoinRepository
import kotlinx.coroutines.launch

/** Coins earned per rewarded ad watched. */
private const val COINS_PER_AD = 5L

@Composable
fun CoinBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uid = AuthRepository.uid
    val scope = rememberCoroutineScope()

    var coins by remember { mutableStateOf<Long?>(null) }
    var loadingAd by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid != null) {
            CoinRepository.observeCoins(uid).collect { coins = it }
        }
    }

    LaunchedEffect(Unit) {
        RewardedAdManager.preload(context)
    }

    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = !loadingAd && uid != null) {
                val activity = context as? android.app.Activity ?: return@clickable
                loadingAd = true
                RewardedAdManager.showIfReady(activity) { earned ->
                    loadingAd = false
                    if (earned) {
                        CoinRepository.addCoins(uid!!, COINS_PER_AD)
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "\uD83E\uDE99", fontSize = 15.sp) // 🪙
        if (loadingAd) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = coins?.toString() ?: "…",
                style = MaterialTheme.typography.labelLarge
            )
            Icon(Icons.Filled.Add, contentDescription = "Watch ad to earn coins", modifier = Modifier.size(16.dp))
        }
    }
}
