package com.aicomp.ui.components

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The soft "breathing" ripple-and-glow animation used on splash/login
 * screens like Claude's — a mark that gently scales in place while 2-3
 * rings expand outward from it and fade, on a continuous stagger.
 *
 * Swap the center Icon for your own mark/monogram if you have one;
 * a heart is used here to match the app's theme.
 */
@Composable
fun CompanionPulseLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 96.dp,
    accentColor: Color = Color(0xFFFF6F91)
) {
    Box(
        modifier = modifier
            .size(size * 2.4f)
            .background(
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.30f), Color.Transparent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        PulseRings(maxRadiusDp = size * 1.2f, color = accentColor)

        val infiniteTransition = rememberInfiniteTransition(label = "markBreathe")
        val breathScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe"
        )

        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = breathScale
                    scaleY = breathScale
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Loading",
                tint = accentColor,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

/**
 * 3 rings, each looping the same expand-and-fade cycle on a staggered
 * delay so a new ring starts just as the previous one is fading out.
 */
@Composable
private fun PulseRings(maxRadiusDp: androidx.compose.ui.unit.Dp, color: Color) {
    val ringDelays = remember { listOf(0, 700, 1400) }
    val ringProgress = ringDelays.map { delay ->
        val infiniteTransition = rememberInfiniteTransition(label = "ring$delay")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2100, delayMillis = delay, easing = EaseOut),
                repeatMode = RepeatMode.Restart
            ),
            label = "ringProgress$delay"
        )
    }

    Canvas(modifier = Modifier.size(maxRadiusDp * 2)) {
        val maxRadiusPx = maxRadiusDp.toPx()
        ringProgress.forEach { progress ->
            val p = progress.value
            drawCircle(
                color = color.copy(alpha = (1f - p) * 0.35f),
                radius = maxRadiusPx * 0.35f + maxRadiusPx * 0.65f * p,
                center = Offset(size.width / 2f, size.height / 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )
        }
    }
}
