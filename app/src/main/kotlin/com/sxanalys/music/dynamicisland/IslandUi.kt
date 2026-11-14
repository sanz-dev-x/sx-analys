package com.sxanalys.music.dynamicisland

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.sxanalys.music.R
import com.sxanalys.music.constants.DynamicIslandStyle
import com.sxanalys.music.ui.component.LocalGlassFrostBlur
import com.sxanalys.music.ui.component.WaterGlassImage
import com.sxanalys.music.ui.component.glassVolume
import com.sxanalys.music.ui.utils.resize
import kotlin.math.max
import kotlin.math.min

data class IslandTrack(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val progress: Float,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

class IslandCallbacks(
    val onToggleExpand: () -> Unit = {},
    val onOpenApp: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
)

const val IslandDefaultWidthDp = 176
const val IslandDefaultHeightDp = 38
private val ExpandedHeight = 88.dp
private val ExpandedMaxRadius = 36.dp
private val ExpandedMaxWidth = 336.dp
private val AccentBlue = Color(0xFFF0904F)

/**
 * Continuous-corner (squircle-like) outline. The radius follows the live height, so the pill
 * morphs into a rounded panel instead of staying a capsule. [curvature] 0..1 scales the corners.
 */
private class IslandShape(
    private val curvature: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val maxR = with(density) { ExpandedMaxRadius.toPx() }
        val r = min(size.minDimension / 2f, maxR) * curvature.coerceIn(0f, 1f)
        val e = min(r * 1.28f, size.minDimension / 2f)
        val c = e * 0.38f
        val path =
            Path().apply {
                moveTo(e, 0f)
                lineTo(w - e, 0f)
                cubicTo(w - c, 0f, w, c, w, e)
                lineTo(w, h - e)
                cubicTo(w, h - c, w - c, h, w - e, h)
                lineTo(e, h)
                cubicTo(c, h, 0f, h - c, 0f, h - e)
                lineTo(0f, e)
                cubicTo(0f, c, c, 0f, e, 0f)
                close()
            }
        return Outline.Generic(path)
    }
}

@Composable
fun DynamicIslandContent(
    track: IslandTrack,
    expanded: Boolean,
    style: DynamicIslandStyle,
    curvaturePercent: Int,
    callbacks: IslandCallbacks,
    modifier: Modifier = Modifier,
    compactWidthDp: Int = IslandDefaultWidthDp,
    compactHeightDp: Int = IslandDefaultHeightDp,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val compactWidth = compactWidthDp.dp
    val compactHeight = compactHeightDp.dp
    val expandedWidth = max(compactWidth.value, min(ExpandedMaxWidth.value, (screenWidth - 32.dp).value)).dp
    val expandedHeight = max(ExpandedHeight.value, compactHeight.value + 24f).dp
    val targetWidth = if (expanded) expandedWidth else compactWidth
    val targetHeight = if (expanded) expandedHeight else compactHeight

    val spec = spring<Dp>(dampingRatio = 0.86f, stiffness = Spring.StiffnessMedium)
    val width by animateDpAsState(targetWidth, spec, label = "islandW")
    val height by animateDpAsState(targetHeight, spec, label = "islandH")
    val shape = remember(curvaturePercent) { IslandShape(curvaturePercent / 100f) }
    val glassTarget =
        when {
            style == DynamicIslandStyle.LIQUID_GLASS -> 0.55f
            else -> 0f
        }
    val glass by animateFloatAsState(glassTarget, tween(280), label = "islandGlass")
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .width(width)
                .height(height)
                .islandSurface(glass, shape)
                .clip(shape)
                .clickable(interactionSource = interaction, indication = null) {
                    if (expanded) callbacks.onOpenApp() else callbacks.onToggleExpand()
                },
    ) {
        val compactAlpha by animateFloatAsStateLocal(if (expanded) 0f else 1f)
        val expandedAlpha by animateFloatAsStateLocal(if (expanded) 1f else 0f)

        if (glass > 0.01f && !track.artworkUrl.isNullOrBlank()) {
            // Liquid glass: sharp HD cover with water-glass distortion, no light reflection
            WaterGlassImage(
                model = track.artworkUrl.resize(720, 720),
                shape = shape,
                scrim = 0.24f,
                blurRadius = LocalGlassFrostBlur.current,
                cornerFraction = (0.25f + curvaturePercent / 200f).coerceIn(0.2f, 0.5f),
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = (glass / 0.55f).coerceIn(0f, 1f) },
            )
        }
        if (compactAlpha > 0.01f) {
            CompactRow(track, compactHeightDp, glass > 0.01f, Modifier.graphicsLayer { alpha = compactAlpha })
        }
        if (expandedAlpha > 0.01f) {
            ExpandedColumn(track, callbacks, Modifier.graphicsLayer { alpha = expandedAlpha })
        }
    }
}

@Composable
private fun animateFloatAsStateLocal(target: Float) =
    androidx.compose.animation.core.animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (target > 0f) 180 else 90, delayMillis = if (target > 0f) 70 else 0),
        label = "islandFade",
    )

private fun Modifier.islandSurface(
    glass: Float,
    shape: Shape,
): Modifier =
    this
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = lerp(1f, 0.40f, glass)),
                    Color.Black.copy(alpha = lerp(1f, 0.22f, glass)),
                ),
            ),
            shape,
        ).clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val innerShade = Stroke(width = 3.dp.toPx())
            onDrawBehind {
                if (glass > 0.01f) {
                    drawOutline(outline, Color.Black.copy(alpha = 0.28f), alpha = glass, style = innerShade)
                }
            }
        }.glassVolume(
            shape = shape,
            gloss = glass,
            bevel = 0f,
            caustic = AccentBlue,
            edge = null,
        )

@Composable
private fun CompactRow(
    track: IslandTrack,
    heightDp: Int,
    light: Boolean,
    modifier: Modifier = Modifier,
) {
    val art = (heightDp - 16).coerceIn(14, 44)
    Row(
        modifier = modifier.fillMaxSize().padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Artwork(track.artworkUrl, Modifier.size(art.dp), radius = (art / 3).dp)
        Equalizer(playing = track.isPlaying, light = light, modifier = Modifier.size(width = 18.dp, height = 14.dp))
    }
}

@Composable
private fun ExpandedColumn(
    track: IslandTrack,
    callbacks: IslandCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(track.artworkUrl, Modifier.size(44.dp), radius = 11.dp)
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ControlButton(R.drawable.skip_previous, callbacks.onPrevious, 34.dp, 40.dp)
            ControlButton(if (track.isPlaying) R.drawable.ios_pause else R.drawable.ios_play, callbacks.onPlayPause, 36.dp, 40.dp)
            ControlButton(R.drawable.ios_forward, callbacks.onNext, 34.dp, 40.dp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(formatTime(track.positionMs), color = Color.White.copy(alpha = 0.50f), fontSize = 10.sp)
            Box(Modifier.weight(1f)) { ProgressLine(track.progress) }
            Text(
                if (track.durationMs > 0) formatTime(track.durationMs) else "--:--",
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 10.sp,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun ControlButton(
    icon: Int,
    onClick: () -> Unit,
    iconSize: Dp,
    boxSize: Dp = 38.dp,
) {
    Box(
        modifier =
            Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize - 6.dp),
        )
    }
}

@Composable
private fun ProgressLine(progress: Float) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(Color.White),
        )
    }
}

@Composable
private fun Artwork(
    url: String?,
    modifier: Modifier,
    radius: Dp,
) {
    val shape = RoundedCornerShape(radius)
    if (url.isNullOrBlank()) {
        Box(modifier.clip(shape).background(Color.White.copy(alpha = 0.14f)))
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    }
}

@Composable
private fun Equalizer(
    playing: Boolean,
    light: Boolean,
    modifier: Modifier = Modifier,
) {
    val levels = remember { androidx.compose.runtime.mutableStateOf(FloatArray(4)) }
    LaunchedEffect(playing) {
        if (!playing) {
            levels.value = FloatArray(4)
            return@LaunchedEffect
        }
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            levels.value = com.sxanalys.music.playback.audio.AudioLevels.levels.copyOf()
        }
    }
    val phases = (0 until 4).map { i -> androidx.compose.runtime.derivedStateOf { levels.value.getOrElse(i) { 0f } } }
    Canvas(modifier) {
        val gap = 2.5.dp.toPx()
        val barW = (size.width - gap * (phases.size - 1)) / phases.size
        phases.forEachIndexed { i, state ->
            val level = if (playing) (0.18f + 0.82f * state.value) else 0.22f
            val h = max(size.height * level, barW)
            drawRoundRect(
                color = if (playing) (if (light) Color.White.copy(alpha = 0.92f) else AccentBlue) else Color.White.copy(alpha = 0.5f),
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}
