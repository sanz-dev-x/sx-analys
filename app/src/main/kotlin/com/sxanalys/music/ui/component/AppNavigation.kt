/**
 * Sx anlys Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.sxanalys.music.ui.theme.LocalLargeNav
import com.sxanalys.music.ui.theme.LocalGlassMode
import com.sxanalys.music.constants.GlassSprings
import com.sxanalys.music.ui.theme.GlassMaterial
import com.sxanalys.music.ui.theme.LocalGlassMaterial
import com.sxanalys.music.ui.theme.LocalGlassAccent
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.sxanalys.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

/** Ikon item navigasi; item "You" memakai foto akun bila sudah login. */
@Composable
private fun NavItemIcon(
    screen: Screens,
    iconRes: Int,
    title: String,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    accountImageUrl: String?,
) {
    if (screen == Screens.Settings && accountImageUrl != null) {
        AsyncImage(
            model = accountImageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
    accountImageUrl: String? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    NavItemIcon(
                        screen = screen,
                        iconRes = iconRes,
                        title = stringResource(screen.titleId),
                        tint = LocalContentColor.current,
                        size = 24.dp,
                        accountImageUrl = accountImageUrl,
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
    accountImageUrl: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(50)
    // Follow the app theme: Liquid Glass Dark -> dark navbar, Bright -> bright navbar, off -> standard (surface based).
    val navGlassMode = LocalGlassMode.current
    val lightSurface =
        when (navGlassMode) {
            com.sxanalys.music.ui.theme.GlassMode.DARK -> false
            com.sxanalys.music.ui.theme.GlassMode.BRIGHT -> true
            else -> MaterialTheme.colorScheme.surface.luminance() > 0.5f && !pureBlack
        }
    val glassBase =
        if (lightSurface) Color.White.copy(alpha = 0.78f) else if (navGlassMode == com.sxanalys.music.ui.theme.GlassMode.DARK) Color(0xFF2A2624).copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
    val idleColor =
        if (lightSurface) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.78f)
    val glassAccent = LocalGlassAccent.current
    val activeTint = if (lightSurface) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primary, Color.White, 0.45f)
    val large = LocalLargeNav.current
    val collapse = LocalNavCollapse.current
    val collapseFraction = collapse.fraction
    val horizontalMargin = 20.dp
    val homeIndex = navigationItems.indexOf(Screens.Home)

    val count = navigationItems.size.coerceAtLeast(1)
    val selectedIndex =
        navigationItems.indexOfFirst { isRouteSelected(currentRoute, it.route, navigationItems) }

    var hoverIndex by remember { mutableIntStateOf(-1) }
    var pendingIndex by remember { mutableIntStateOf(-1) }
    var dragging by remember { mutableStateOf(false) }
    var innerWidthPx by remember { mutableFloatStateOf(0f) }
    val itemWidthPx = innerWidthPx / count

    LaunchedEffect(selectedIndex) { pendingIndex = -1 }

    val shownIndex =
        when {
            hoverIndex >= 0 -> hoverIndex
            pendingIndex >= 0 -> pendingIndex
            else -> selectedIndex
        }

    val lensX = remember { Animatable(0f) }
    var lensPlaced by remember { mutableStateOf(false) }
    LaunchedEffect(shownIndex, itemWidthPx) {
        if (shownIndex < 0 || itemWidthPx <= 0f || dragging) return@LaunchedEffect
        val target = shownIndex * itemWidthPx
        if (!lensPlaced) {
            lensX.snapTo(target)
            lensPlaced = true
        } else {
            lensX.animateTo(target, GlassSprings.lens)
        }
    }
    val trailX = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { lensX.value }.collectLatest { target ->
            trailX.animateTo(target, spring(dampingRatio = 0.92f, stiffness = 70f))
        }
    }
    val dragAmount by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = GlassSprings.morph,
        label = "lensDrag",
    )

    val latestItems by rememberUpdatedState(navigationItems)
    val latestSelected by rememberUpdatedState(selectedIndex)
    val latestOnClick by rememberUpdatedState(onItemClick)
    val latestSearchLong by rememberUpdatedState(onSearchLongClick)
    val latestHomeHold by rememberUpdatedState(onHomeLongHold)
    val latestCollapse by rememberUpdatedState(collapseFraction)
    val latestHomeIndex by rememberUpdatedState(homeIndex)

    CompositionLocalProvider(LocalGlassMaterial provides GlassMaterial.CLEAR) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
        Box(
            modifier =
                Modifier
                    .offset { IntOffset((-(horizontalMargin - NavDockEdge).roundToPx() * latestCollapse()).roundToInt(), 0) }
                    .clearGlassNav(shape, lightSurface, meltAlpha = { (1f - latestCollapse()).coerceIn(0f, 1f) })
                    .clip(shape)
                    .collapsePill(
                        fraction = collapseFraction,
                        collapsedWidth = NavCollapsedCircleSize,
                        collapsedHeight = NavCollapsedCircleSize,
                    ).padding(5.dp)
                    .onSizeChanged { innerWidthPx = it.width.toFloat() }
                    .pointerInput(count) {
                        coroutineScope {
                            val scope = this
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val w = size.width.toFloat().coerceAtLeast(1f)

                                fun indexAt(x: Float): Int = ((x / w) * count).toInt().coerceIn(0, count - 1)

                                val isCollapsed = latestCollapse() > 0.5f && latestHomeIndex >= 0
                                val downIndex = if (isCollapsed) latestHomeIndex else indexAt(down.position.x)
                                val downItem = latestItems.getOrNull(downIndex)
                                val longAction: (() -> Unit)? =
                                    when {
                                        downItem == Screens.Search -> latestSearchLong
                                        downItem == Screens.Home -> latestHomeHold
                                        else -> null
                                    }
                                val longDelay =
                                    if (downItem == Screens.Home) 15_000L else viewConfiguration.longPressTimeoutMillis

                                var longFired = false
                                var dragged = false
                                var cancelled = false
                                var lastIndex = downIndex
                                hoverIndex = downIndex

                                val longJob =
                                    longAction?.let { action ->
                                        scope.launch {
                                            delay(longDelay)
                                            longFired = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            action()
                                        }
                                    }

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null) {
                                        cancelled = true
                                        break
                                    }
                                    if (change.changedToUpIgnoreConsumed()) {
                                        change.consume()
                                        break
                                    }
                                    if (!isCollapsed && !dragged && abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) {
                                        dragged = true
                                        dragging = true
                                        longJob?.cancel()
                                    }
                                    if (dragged) {
                                        val itemW = w / count
                                        val follow = (change.position.x - itemW / 2f).coerceIn(0f, (w - itemW).coerceAtLeast(0f))
                                        scope.launch {
                                            lensX.animateTo(follow, GlassSprings.lensDrag)
                                        }
                                        val idx = indexAt(change.position.x)
                                        if (idx != lastIndex) {
                                            lastIndex = idx
                                            hoverIndex = idx
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        change.consume()
                                    }
                                }

                                longJob?.cancel()
                                dragging = false
                                val finalIndex = if (dragged) lastIndex else downIndex
                                if (dragged) {
                                    scope.launch {
                                        lensX.animateTo(finalIndex * (w / count), GlassSprings.lens)
                                    }
                                }
                                if (!cancelled && !longFired) {
                                    pendingIndex = finalIndex
                                    if (!dragged && finalIndex != latestSelected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    latestItems.getOrNull(finalIndex)?.let { screen ->
                                        latestOnClick(screen, finalIndex == latestSelected)
                                    }
                                }
                                hoverIndex = -1
                            }
                        }
                    },
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = (1f - 2f * collapseFraction()).coerceIn(0f, 1f) }
                        .lensMagnify(
                            centerX = { lensX.value + itemWidthPx / 2f },
                            amount = { 0f },
                            radius = { itemWidthPx * 0.85f },
                        ),
            ) {
                navigationItems.forEachIndexed { index, screen ->
                    val active = index == shownIndex
                    val iconRes = if (active) screen.iconIdActive else screen.iconIdInactive
                    val fg by animateColorAsState(
                        targetValue = if (active) activeTint else idleColor,
                        label = "glassItemFg",
                    )
                    val title = stringResource(screen.titleId)
                    val itemScale by animateFloatAsState(
                        targetValue =
                            when {
                                index == hoverIndex && !dragging -> 0.9f
                                active -> 1.08f
                                else -> 1f
                            },
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
                        label = "navItemScale",
                    )
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Tab
                                    selected = index == selectedIndex
                                    onClick(label = title) {
                                        latestOnClick(screen, index == latestSelected)
                                        true
                                    }
                                }.padding(vertical = if (slimNav) 8.dp else 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        NavItemIcon(
                            screen = screen,
                            iconRes = iconRes,
                            title = title,
                            tint = fg,
                            size = if (slimNav) 26.dp else 23.dp,
                            accountImageUrl = accountImageUrl,
                        )
                        if (!slimNav) {
                            Text(
                                text = title,
                                color = fg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                    }
                }
            }
            if (homeIndex >= 0) {
                val homeActive = selectedIndex == homeIndex
                val homeTint by animateColorAsState(
                    targetValue = if (homeActive) activeTint else idleColor,
                    label = "collapsedHomeTint",
                )
                Box(
                    modifier =
                        Modifier
                            .width(NavCollapsedCircleSize - 12.dp)
                            .fillMaxHeight()
                            .graphicsLayer { alpha = (2f * collapseFraction() - 1f).coerceIn(0f, 1f) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (homeActive) Screens.Home.iconIdActive else Screens.Home.iconIdInactive),
                        contentDescription = stringResource(Screens.Home.titleId),
                        tint = homeTint,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        if (shownIndex >= 0 && itemWidthPx > 0f) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(5.dp)
                        .graphicsLayer { alpha = (1f - 2f * collapseFraction()).coerceIn(0f, 1f) },
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(with(density) { (itemWidthPx + abs(lensX.value - trailX.value)).toDp() })
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = minOf(lensX.value, trailX.value)
                                val gap = abs(lensX.value - trailX.value) / itemWidthPx.coerceAtLeast(1f)
                                alpha = (gap * 1.6f).coerceIn(0f, 0.85f)
                            }.padding(horizontal = 2.dp, vertical = 2.dp)
                            .trailPill(lightSurface),
                )
                Box(
                    modifier =
                        Modifier
                            .width(with(density) { itemWidthPx.toDp() })
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = trailX.value
                                alpha = (abs(lensX.value - trailX.value) / itemWidthPx.coerceAtLeast(1f) * 1.2f).coerceIn(0f, 0.5f)
                            }.padding(horizontal = 2.dp)
                            .trailPill(lightSurface),
                )
                Box(
                    modifier =
                        Modifier
                            .width(with(density) { itemWidthPx.toDp() })
                            .fillMaxHeight()
                            .graphicsLayer {
                                translationX = lensX.value
                                val stretch = 1f + (abs(lensX.velocity) / 9000f).coerceAtMost(0.16f)
                                scaleX = (1f + 0.16f * dragAmount) * stretch
                                scaleY = 1f + 0.06f * dragAmount
                            },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .padding(horizontal = 2.dp)
                                .graphicsLayer { alpha = 1f - dragAmount }
                                .restPill(glassAccent),
                    )
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .padding(horizontal = 2.dp)
                                .graphicsLayer { alpha = dragAmount }
                                .dragLens(lightSurface),
                    )
                }
            }
        }
        }
    }
    }
}


/** Jejak lengket di belakang lensa: kapsul kaca polos tanpa tepi. */
private fun Modifier.trailPill(light: Boolean): Modifier =
    this.clip(CircleShape).background(if (light) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f))

/** Penanda tab aktif: kapsul ungu translusen dengan tepi tipis. */
private fun Modifier.restPill(accent: Color): Modifier =
    this
        .clip(CircleShape)
        .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0.18f))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))), CircleShape)

/** Saat digeser: pil yang sama, sedikit lebih terang. */
@Composable
internal fun Modifier.dragLens(light: Boolean): Modifier =
    this
        .clip(CircleShape)
        .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.46f), MaterialTheme.colorScheme.primary.copy(alpha = 0.26f))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.10f))), CircleShape)
