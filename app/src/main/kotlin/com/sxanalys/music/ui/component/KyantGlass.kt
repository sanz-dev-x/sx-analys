package com.sxanalys.music.ui.component

import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.sxanalys.music.ui.theme.GlassMode
import com.sxanalys.music.ui.theme.LocalGlassMode
import kotlin.math.max

/** Backdrop Kyant (Liquid Glass). Null di bawah Android 13, jadi pemanggil jatuh ke kaca lama. */
val LocalKyantBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Dasar yang digambar di belakang konten layar supaya kaca punya sesuatu untuk dibiaskan. */
@Composable
fun rememberKyantBackdrop(mode: GlassMode, screenColor: Color): LayerBackdrop {
    val currentMode by rememberUpdatedState(mode)
    val screen by rememberUpdatedState(screenColor)
    return rememberLayerBackdrop {
        when (currentMode) {
            GlassMode.DARK ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2B1F19), Color(0xFF17110E), Color(0xFF0D0A09))))
            GlassMode.BRIGHT ->
                drawRect(Brush.verticalGradient(listOf(Color(0xFFE9D6D3), Color(0xFFEFE5E3), Color(0xFFF7F3F1))))
            GlassMode.OFF -> drawRect(screen)
        }
        drawContent()
    }
}

/** Menandai konten yang direkam sebagai backdrop untuk kaca navbar dan mini player. */
@Composable
fun Modifier.kyantBackdropSource(): Modifier {
    val backdrop = LocalKyantBackdrop.current ?: return this
    return layerBackdrop(backdrop)
}

/** Ungu halus untuk pantulan krom di tepi bawah. */
internal val ChromePurple = Color(0xFF8E6BBF)

private class MeltDrip(val at: Float, val width: Float, val length: Float)

/** Posisi, lebar (dp), dan panjang relatif tetesan. Sengaja tidak simetris supaya terlihat organik. */
private val MeltDrips =
    listOf(
        MeltDrip(0.10f, 9f, 0.55f),
        MeltDrip(0.29f, 14f, 1.00f),
        MeltDrip(0.47f, 7f, 0.38f),
        MeltDrip(0.63f, 11f, 0.78f),
        MeltDrip(0.81f, 15f, 0.92f),
        MeltDrip(0.94f, 7f, 0.42f),
    )

/**
 * Formasi kaca cair yang meleleh ke bawah dari tepi bawah. Digambar SEBELUM permukaan kaca Kyant
 * (di belakangnya), jadi pangkalnya menyatu dengan badan kaca. Ini geometri nyata (path leher + bulir)
 * dengan material krom, bukan refraksi: refraksi latar hanya terjadi di badan utama lewat lens Kyant,
 * karena lens hanya jalan pada CornerBasedShape di dalam batas layout. Tidak mengubah ukuran layout.
 */
internal fun Modifier.liquidMelt(dark: Boolean, intense: Boolean, alpha: () -> Float): Modifier =
    drawWithCache {
        val w = size.width
        val h = size.height
        val maxLen = (if (intense) 12f else 8f).dp.toPx()
        val overlap = 4.dp.toPx()
        val margin = h * 0.5f
        val span = max(w - margin * 2f, 1f)
        val top = h - overlap
        val bottom = h + maxLen
        val body =
            Brush.verticalGradient(
                0f to ChromeChampagne.copy(alpha = 0f),
                0.22f to ChromeChampagne.copy(alpha = if (dark) 0.50f else 0.62f),
                0.52f to ChromeAmber.copy(alpha = 0.62f),
                0.78f to ChromeCopper.copy(alpha = 0.66f),
                1f to ChromePurple.copy(alpha = 0.50f),
                startY = top,
                endY = bottom,
            )
        val rim =
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.35f to ChromeWarmWhite.copy(alpha = 0.75f),
                1f to ChromeBrown.copy(alpha = if (dark) 0.55f else 0.40f),
                startY = top,
                endY = bottom,
            )
        val stroke = Stroke(width = 0.8.dp.toPx())
        class Built(val path: Path, val cx: Float, val bcY: Float, val r: Float)
        val built =
            MeltDrips.map { d ->
                val cx = margin + d.at * span
                val hw = d.width.dp.toPx() / 2f
                val len = maxLen * d.length
                val r = hw
                val neckY = h + len * 0.45f
                val bcY = max(neckY, h + len - r)
                val baseL = cx - hw * 2.2f
                val baseR = cx + hw * 2.2f
                val path =
                    Path().apply {
                        moveTo(baseL, top)
                        cubicTo(baseL + hw * 1.3f, h + len * 0.05f, cx - hw * 0.55f, h + len * 0.15f, cx - hw * 0.55f, neckY)
                        cubicTo(cx - hw * 0.55f, neckY + len * 0.1f, cx - r, bcY - r * 0.3f, cx - r, bcY)
                        cubicTo(cx - r, bcY + r * 0.55f, cx - r * 0.55f, bcY + r, cx, bcY + r)
                        cubicTo(cx + r * 0.55f, bcY + r, cx + r, bcY + r * 0.55f, cx + r, bcY)
                        cubicTo(cx + r, bcY - r * 0.3f, cx + hw * 0.55f, neckY + len * 0.1f, cx + hw * 0.55f, neckY)
                        cubicTo(cx + hw * 0.55f, h + len * 0.15f, baseR - hw * 1.3f, h + len * 0.05f, baseR, top)
                        close()
                    }
                Built(path, cx, bcY, r)
            }
        onDrawWithContent {
            val a = alpha().coerceIn(0f, 1f)
            if (a > 0.01f) {
                built.forEach { b ->
                    drawPath(b.path, body, alpha = a)
                    clipPath(b.path) {
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                                    center = Offset(b.cx - b.r * 0.35f, b.bcY - b.r * 0.4f),
                                    radius = b.r * 0.75f,
                                ),
                            radius = b.r * 0.75f,
                            center = Offset(b.cx - b.r * 0.35f, b.bcY - b.r * 0.4f),
                            alpha = a,
                        )
                    }
                    drawPath(b.path, rim, alpha = a, style = stroke)
                }
            }
            drawContent()
        }
    }

/**
 * Liquid chrome glass memakai library Kyant: vibrancy, blur, dan lens (refraksi nyata, Android 13+).
 * Permukaan: rona oranye (Hue blend) lalu gradien chrome (pantulan champagne, horizon coklat, bounce amber)
 * dan sapuan spekular. Navbar halus; mini player ([intense]) lebih dalam dengan depthEffect dan glow amber.
 * Mengembalikan null bila backdrop belum tersedia, supaya pemanggil memakai fallback.
 */
@Composable
internal fun Modifier.kyantChrome(
    shape: Shape,
    light: Boolean,
    intense: Boolean,
    meltAlpha: (() -> Float)? = null,
): Modifier? {
    val backdrop = LocalKyantBackdrop.current
    val mode = LocalGlassMode.current
    val lowQuality = LocalGlassQuality.current == GlassQuality.LOW
    if (backdrop == null || Build.VERSION.SDK_INT < 33 || lowQuality) return null
    val dark =
        when (mode) {
            GlassMode.DARK -> true
            GlassMode.BRIGHT -> false
            GlassMode.OFF -> !light
        }
    val k = if (intense) 1f else 0.6f
    val refractable = shape is CornerBasedShape
    val melted = if (meltAlpha != null) liquidMelt(dark, intense, meltAlpha) else this
    return melted.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur((if (intense) 5f else 3f).dp.toPx())
            if (refractable) {
                lens(
                    refractionHeight = (if (intense) 18f else 10f).dp.toPx(),
                    refractionAmount = (if (intense) 36f else 20f).dp.toPx(),
                    depthEffect = intense,
                    chromaticAberration = true,
                )
            }
        },
        onDrawSurface = {
            drawRect(ChromeOrange, blendMode = BlendMode.Hue)
            drawRect(ChromeOrange.copy(alpha = (if (dark) 0.22f else 0.26f) * k))
            drawRect(
                Brush.verticalGradient(
                    0f to ChromeChampagne.copy(alpha = (if (dark) 0.30f else 0.44f) * k),
                    0.34f to ChromeAmber.copy(alpha = 0.30f * k),
                    0.50f to ChromeOrange.copy(alpha = 0.14f * k),
                    0.58f to ChromeBrown.copy(alpha = (if (dark) 0.34f else 0.16f) * k),
                    0.80f to ChromeCopper.copy(alpha = 0.26f * k),
                    1f to ChromeAmber.copy(alpha = 0.40f * k),
                ),
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, ChromeWarmWhite.copy(alpha = 0.20f * k), Color.Transparent),
                    start = Offset(size.width * 0.10f, 0f),
                    end = Offset(size.width * 0.34f, size.height),
                ),
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, ChromeChampagne.copy(alpha = 0.12f * k), Color.Transparent),
                    start = Offset(size.width * 0.64f, 0f),
                    end = Offset(size.width * 0.78f, size.height),
                ),
            )
            drawRect(
                Brush.verticalGradient(
                    0.55f to Color.Transparent,
                    1f to ChromePurple.copy(alpha = 0.16f * k),
                ),
            )
            if (intense) {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(ChromeAmber.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 1.05f),
                        radius = size.height * 2.2f,
                    ),
                )
            }
        },
    )
}
