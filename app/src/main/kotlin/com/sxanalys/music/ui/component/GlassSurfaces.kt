package com.sxanalys.music.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import com.sxanalys.music.ui.theme.GlassMode
import com.sxanalys.music.ui.theme.LocalGlassAccent
import com.sxanalys.music.ui.theme.LocalGlassMode

internal val ChromeOrange = Color(0xFFD97757)
internal val ChromeAmber = Color(0xFFF0A04B)
internal val ChromeCopper = Color(0xFFB5622F)
internal val ChromeChampagne = Color(0xFFFFE9CC)
internal val ChromeWarmWhite = Color(0xFFFFF7EE)
internal val ChromeBrown = Color(0xFF2A1509)
internal val ChromeActiveLight = Color(0xFFB4491F)

/** Intensitas 3D liquid glass. Navbar tipis dan ringan, mini player lebih tebal dan dalam. */
private class LiquidSpec(
    val tint: Float,
    val ring: Float,
    val gloss: Float,
    val caustic: Float,
    val refract: Float,
    val elevation: Dp,
)

private val NavLiquid = LiquidSpec(tint = 0.14f, ring = 4f, gloss = 0.26f, caustic = 0.22f, refract = 0.7f, elevation = 9.dp)
private val PlayerLiquid = LiquidSpec(tint = 0.24f, ring = 8f, gloss = 0.36f, caustic = 0.40f, refract = 1f, elevation = 18.dp)

/**
 * 3D liquid glass. Warna mengikuti tema: Liquid Glass Dark -> kaca gelap, Bright -> kaca terang,
 * standar -> turunan warna skema Material. Lapisan: refraksi latar (API 33+, kalau tidak ada
 * jatuh ke veil pekat), tint aksen, cincin tebal bagian dalam (kesan ketebalan kaca), pantulan
 * gloss melengkung di atas, caustic di bawah, titik spekular, lalu tepi tajam. Semua di belakang
 * konten kecuali tepi tipis. Brush di-cache per ukuran.
 */
@Composable
private fun Modifier.liquid3D(shape: Shape, light: Boolean, spec: LiquidSpec): Modifier {
    val mode = LocalGlassMode.current
    val scheme = MaterialTheme.colorScheme
    val dark =
        when (mode) {
            GlassMode.DARK -> true
            GlassMode.BRIGHT -> false
            GlassMode.OFF -> !light
        }
    val accent = if (mode == GlassMode.OFF) scheme.primary else LocalGlassAccent.current
    val optical = rememberOpticalBackdrop()
    val hasBlur = optical != null
    val base =
        when (mode) {
            GlassMode.DARK -> Color(0xFF0E0C0B)
            GlassMode.BRIGHT -> Color(0xFFFFFFFF)
            GlassMode.OFF -> scheme.surfaceContainerHigh
        }
    val veil = base.copy(alpha = if (hasBlur) (if (dark) 0.34f else 0.30f) else (if (dark) 0.88f else 0.90f))
    val hi = lerp(accent, Color.White, 0.9f)
    val shade = if (dark) Color.Black else lerp(accent, Color.Black, 0.55f)
    val strength = LocalGlassRefraction.current * spec.refract
    return this
        .shadow(
            elevation = spec.elevation,
            shape = shape,
            ambientColor = shade.copy(alpha = if (dark) 0.40f else 0.18f),
            spotColor = (if (dark) accent else shade).copy(alpha = if (dark) 0.30f else 0.30f),
        ).clip(shape)
        .then(if (optical != null) Modifier.opticalRefraction(optical, shape, strength, true, 1f) else Modifier)
        .background(veil)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val outline = shape.createOutline(size, layoutDirection, this)
            val tintK = if (dark) 1f else 0.75f
            val body =
                Brush.verticalGradient(
                    0f to accent.copy(alpha = spec.tint * 1.2f * tintK),
                    0.55f to accent.copy(alpha = spec.tint * 0.45f * tintK),
                    1f to accent.copy(alpha = spec.tint * 1.0f * tintK),
                )
            val ringW = spec.ring.dp.toPx()
            val ringBrush =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (dark) 0.40f else 0.70f),
                    0.45f to shade.copy(alpha = if (dark) 0.34f else 0.16f),
                    0.80f to shade.copy(alpha = if (dark) 0.20f else 0.10f),
                    1f to hi.copy(alpha = if (dark) 0.55f else 0.80f),
                )
            val ring = Stroke(width = ringW * 2f)
            val glossBrush =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = spec.gloss),
                    1f to Color.White.copy(alpha = 0.02f),
                )
            val causticBrush =
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = spec.caustic), Color.Transparent),
                    center = Offset(w * 0.3f, h * 1.05f),
                    radius = h * 2.0f,
                )
            val spot =
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.14f, h * 0.2f),
                    radius = h * 0.55f,
                )
            val sideSheen =
                Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.18f to Color.Transparent,
                    0.82f to Color.Transparent,
                    1f to Color.White.copy(alpha = 0.10f),
                )
            val rim =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.95f),
                    0.5f to hi.copy(alpha = if (dark) 0.18f else 0.40f),
                    1f to hi.copy(alpha = 0.70f),
                )
            val outer = Stroke(width = 1.2.dp.toPx())
            val innerEdge = Stroke(width = 0.8.dp.toPx())
            val innerBrush =
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.35f),
                    0.5f to Color.Transparent,
                    1f to Color.White.copy(alpha = 0.30f),
                )
            val sx = ((w - ringW * 1.6f) / w).coerceIn(0.5f, 1f)
            val sy = ((h - ringW * 1.6f) / h).coerceIn(0.5f, 1f)
            val center = Offset(w / 2f, h / 2f)
            val topPivot = Offset(w / 2f, h * 0.06f)
            onDrawWithContent {
                drawOutline(outline, body)
                drawOutline(outline, sideSheen)
                drawOutline(outline, ringBrush, style = ring)
                withTransform({ scale(sx, sy, center) }) {
                    drawOutline(outline, innerBrush, style = innerEdge)
                }
                withTransform({ scale(0.93f, 0.42f, topPivot) }) {
                    drawOutline(outline, glossBrush)
                }
                drawOutline(outline, causticBrush)
                drawOutline(outline, spot)
                drawContent()
                drawOutline(outline, rim, style = outer)
            }
        }
}

/** Navbar: 3D liquid glass ringan, warna mengikuti mode tema. */
@Composable
fun Modifier.clearGlassNav(shape: Shape, light: Boolean, meltAlpha: (() -> Float)? = null): Modifier =
    kyantChrome(shape, light, intense = false, meltAlpha = meltAlpha) ?: liquid3D(shape, light, NavLiquid)

/** Mini player: material sama dengan navbar, lebih tebal dengan refleksi dan kedalaman lebih kuat. */
@Composable
fun Modifier.tintedGlass3D(shape: Shape, light: Boolean, meltAlpha: (() -> Float)? = { 1f }): Modifier =
    kyantChrome(shape, light, intense = true, meltAlpha = meltAlpha) ?: liquid3D(shape, light, PlayerLiquid)
