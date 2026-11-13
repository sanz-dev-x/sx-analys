package com.sxanalys.music.ui.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.sxanalys.music.ui.theme.GlassMode
import com.sxanalys.music.ui.theme.LocalGlassAccent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
}

private fun shiftHue(color: Color, degrees: Float, saturationScale: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    hsv[1] = (hsv[1] * saturationScale).coerceIn(0f, 1f)
    hsv[2] = hsv[2].coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun LiquidBackdropLayer(
    mode: GlassMode,
    modifier: Modifier = Modifier,
) {
    if (mode == GlassMode.OFF) return
    val accent = LocalGlassAccent.current
    val animated = LocalGlassQuality.current != GlassQuality.LOW && rememberMotionEnabled()
    val dark = mode == GlassMode.DARK
    val tones = remember(accent) {
        listOf(accent, shiftHue(accent, -38f, 0.85f), shiftHue(accent, 42f, 0.7f))
    }
    val phaseAnim = remember { Animatable(0f) }
    LaunchedEffect(animated) {
        if (animated) {
            phaseAnim.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(48000, easing = LinearEasing), RepeatMode.Restart),
            )
        }
    }
    val phase: State<Float> = phaseAnim.asState()
    val tau = (2.0 * Math.PI).toFloat()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val t = phase.value * tau
                    val base = if (dark) 0.36f else 0.32f
                    fun blob(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
                        drawRect(
                            Brush.radialGradient(
                                listOf(color.copy(alpha = alpha), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = radius,
                            ),
                        )
                    }
                    blob(
                        tones[0],
                        w * (0.20f + 0.18f * sin(t)),
                        h * (0.14f + 0.10f * cos(t)),
                        w * 1.05f,
                        base,
                    )
                    blob(
                        tones[1],
                        w * (0.86f + 0.12f * cos(t * 2f)),
                        h * (0.50f + 0.16f * sin(t)),
                        w * 0.95f,
                        base * 0.7f,
                    )
                    blob(
                        tones[2],
                        w * (0.30f + 0.20f * sin(t * 2f + 1.7f)),
                        h * (0.90f + 0.07f * cos(t * 3f)),
                        w * 1.0f,
                        base * 0.6f,
                    )
                },
    )
}
