package com.aicomp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Shows the companion's photo, in priority order:
 *  1. [avatarUrl] — a remote photo uploaded via the admin panel (Firebase
 *     Storage URL), loaded with Coil.
 *  2. [avatarAsset] — a bundled local drawable (e.g. res/drawable/companion_priya.jpg).
 *  3. [emoji] — a colored emoji circle fallback if neither photo is available.
 */
@Composable
fun CompanionAvatar(
    emoji: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    avatarAsset: String? = null,
    avatarUrl: String? = null
) {
    val context = LocalContext.current

    val resId = remember(avatarAsset) {
        if (!avatarAsset.isNullOrBlank())
            context.resources.getIdentifier(avatarAsset, "drawable", context.packageName)
        else 0
    }

    when {
        !avatarUrl.isNullOrBlank() -> {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
        resId != 0 -> {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
        else -> {
            Box(
                modifier = modifier
                    .size(size)
                    .background(accentColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}
