package com.sxanalys.music.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun Modifier.wetGlass(
    shape: Shape,
    intensity: Float = 1f,
): Modifier =
    drawWithCache {
        val w = size.width
        val h = size.height
        val d = density
        val outline = shape.createOutline(size, layoutDirection, this)
        val rim = Stroke(width = 1.4f * d)
        val cool =
            Brush.linearGradient(
                0f to Color(0xFFFFFFFF).copy(alpha = 0.55f * intensity),
                0.5f to Color.Transparent,
                1f to Color(0xFFFFFFFF).copy(alpha = 0.14f * intensity),
                start = Offset.Zero,
                end = Offset(w, h),
            )
        onDrawWithContent {
            drawContent()
            drawOutline(outline, cool, style = rim)
        }
    }
