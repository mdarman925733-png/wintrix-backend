package com.aicomp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * Draws a simple heart shape (four cubic beziers) centered at [center],
 * where [size] roughly controls the heart's overall height/width — pass the
 * same value you'd have used as a circle's radius for an equivalent visual size.
 */
private fun DrawScope.drawHeart(color: Color, center: Offset, size: Float) {
    val s = size * 1.6f
    val cx = center.x
    val cy = center.y - s * 0.3f
    val path = Path().apply {
        moveTo(cx, cy + s / 4f)
        cubicTo(cx, cy, cx - s / 2f, cy, cx - s / 2f, cy + s / 4f)
        cubicTo(cx - s / 2f, cy + s / 2f, cx, cy + s / 2f, cx, cy + s)
        cubicTo(cx, cy + s / 2f, cx + s / 2f, cy + s / 2f, cx + s / 2f, cy + s / 4f)
        cubicTo(cx + s / 2f, cy, cx, cy, cx, cy + s / 4f)
        close()
    }
    drawPath(path, color = color)
}

/**
 * The same soft pink "bokeh" glow used behind the Login screen's hero text —
 * reused here so Home, Chat, Settings, Call, and Profile all feel like the
 * same warm, romantic app instead of a generic Material screen.
 *
 * Meant to sit behind a TopAppBar / header, not the whole scrollable content
 * (too busy behind a long chat/list otherwise).
 */
@Composable
fun CompanionGlowHeader(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 180.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val glow = listOf(
            Triple(size.width * 0.15f, size.height * 0.25f, 60f),
            Triple(size.width * 0.80f, size.height * 0.15f, 40f),
            Triple(size.width * 0.55f, size.height * 0.55f, 75f),
            Triple(size.width * 0.90f, size.height * 0.65f, 28f),
            Triple(size.width * 0.25f, size.height * 0.70f, 22f)
        )
        glow.forEach { (x, y, r) ->
            drawHeart(
                color = Color(0xFFFF6F91).copy(alpha = 0.14f),
                size = r,
                center = Offset(x, y)
            )
            drawHeart(
                color = Color(0xFFFF6F91).copy(alpha = 0.30f),
                size = r * 0.35f,
                center = Offset(x, y)
            )
        }
    }
}
