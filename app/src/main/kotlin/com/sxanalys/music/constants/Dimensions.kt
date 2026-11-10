/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.constants

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val CONTENT_TYPE_HEADER = 0
const val CONTENT_TYPE_LIST = 1
const val CONTENT_TYPE_SONG = 2
const val CONTENT_TYPE_ARTIST = 3
const val CONTENT_TYPE_ALBUM = 4
const val CONTENT_TYPE_PLAYLIST = 5

val NavigationBarHeight = 70.dp
val SlimNavBarHeight = 58.dp
val MiniPlayerHeight = 64.dp
val MinMiniPlayerHeight = 16.dp
val MiniPlayerBottomSpacing = 8.dp // Space between MiniPlayer and NavigationBar
val QueuePeekHeight = 64.dp
val AppBarHeight = 64.dp

val ListItemHeight = 64.dp
val SuggestionItemHeight = 56.dp
val SearchFilterHeight = 48.dp
val ListThumbnailSize = 48.dp
val SmallGridThumbnailHeight = 104.dp
val GridThumbnailHeight = 128.dp
val AlbumThumbnailSize = 144.dp

val ThumbnailCornerRadius = 3.dp

val PlayerHorizontalPadding = 32.dp

val NavigationBarAnimationSpec = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val BottomSheetAnimationSpec = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val BottomSheetSoftAnimationSpec = spring<Dp>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

/** Transisi tab: bergerak cepat lalu mengendap halus, tanpa terasa menunggu. */
const val GlassMotionMillis = 460

val GlassEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

fun <T> glassTween(): FiniteAnimationSpec<T> = tween(GlassMotionMillis, easing = GlassEasing)

fun <T> glassExitTween(): FiniteAnimationSpec<T> = tween(180, easing = FastOutLinearInEasing)

@Volatile
var pageTransitionsEnabled = true

@Volatile
var pageTransitionStyle = 0

val IosSpringEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val IosExitEasing = CubicBezierEasing(0.4f, 0f, 0.9f, 0.4f)

object GlassSprings {
    val press = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)
    val release = spring<Float>(dampingRatio = 0.62f, stiffness = 520f)
    val lens = spring<Float>(dampingRatio = 0.8f, stiffness = 360f)
    val lensDrag = spring<Float>(dampingRatio = 0.86f, stiffness = 520f)
    val settle = spring<Float>(dampingRatio = 0.9f, stiffness = 300f)
    val morph = spring<Float>(dampingRatio = 0.78f, stiffness = 240f)
}

fun navEnter(forward: Boolean, tab: Boolean = false): EnterTransition {
    if (!pageTransitionsEnabled) return EnterTransition.None
    val dir = if (forward) 1 else -1
    return when {
        pageTransitionStyle == 1 ->
            fadeIn(tween(260, delayMillis = 40, easing = LinearOutSlowInEasing)) +
                scaleIn(tween(420, easing = IosSpringEasing), initialScale = 0.96f)
        pageTransitionStyle == 2 ->
            slideInHorizontally(tween(460, easing = IosSpringEasing)) { dir * it / 2 } +
                fadeIn(tween(220, easing = LinearOutSlowInEasing))
        tab ->
            slideInHorizontally(tween(420, easing = IosSpringEasing)) { dir * it / 10 } +
                fadeIn(tween(240, delayMillis = 60, easing = LinearOutSlowInEasing)) +
                scaleIn(tween(420, easing = IosSpringEasing), initialScale = 0.985f)
        else ->
            slideInHorizontally(tween(480, easing = IosSpringEasing)) { dir * it } +
                fadeIn(tween(160, easing = LinearOutSlowInEasing))
    }
}

fun navExit(forward: Boolean, tab: Boolean = false): ExitTransition {
    if (!pageTransitionsEnabled) return ExitTransition.None
    val dir = if (forward) 1 else -1
    return when {
        pageTransitionStyle == 1 ->
            fadeOut(tween(140, easing = FastOutLinearInEasing)) +
                scaleOut(tween(260, easing = IosExitEasing), targetScale = 0.98f)
        pageTransitionStyle == 2 ->
            slideOutHorizontally(tween(420, easing = IosSpringEasing)) { -dir * it / 3 } +
                fadeOut(tween(200, easing = FastOutLinearInEasing))
        tab ->
            slideOutHorizontally(tween(260, easing = IosExitEasing)) { -dir * it / 10 } +
                fadeOut(tween(140, easing = FastOutLinearInEasing)) +
                scaleOut(tween(260, easing = IosExitEasing), targetScale = 0.985f)
        else ->
            slideOutHorizontally(tween(480, easing = IosSpringEasing)) { -dir * it / 3 } +
                fadeOut(tween(300, delayMillis = 60, easing = FastOutLinearInEasing))
    }
}
