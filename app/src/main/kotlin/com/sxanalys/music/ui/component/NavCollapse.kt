package com.sxanalys.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val NavCollapsedCircleSize = 64.dp
val NavCollapsedGap = 8.dp
private val DockEdgePadding = 12.dp

/** Collapse progress (0 = full navbar, 1 = home circle + docked mini player) and the distance the mini player drops. */
@Stable
class NavCollapseSpec(
    val fraction: () -> Float,
    val dropY: Dp,
)

val LocalNavCollapse = staticCompositionLocalOf { NavCollapseSpec(fraction = { 0f }, dropY = 0.dp) }

@Stable
class NavCollapseState {
    var collapsed by mutableStateOf(false)
}

@Composable
fun rememberNavCollapseFraction(state: NavCollapseState): () -> Float {
    val fraction by animateFloatAsState(
        targetValue = if (state.collapsed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "navCollapse",
    )
    return remember { { fraction } }
}

class NavCollapseConnection(
    private val state: NavCollapseState,
    private val thresholdPx: Float,
    private val enabled: () -> Boolean,
) : NestedScrollConnection {
    private var travelled = 0f

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val dy = available.y
        if (dy == 0f || !enabled()) return Offset.Zero
        if (!state.collapsed && travelled <= -thresholdPx) travelled = 0f
        if ((dy < 0f) != (travelled < 0f)) travelled = 0f
        travelled += dy
        if (travelled <= -thresholdPx) {
            state.collapsed = true
            travelled = -thresholdPx
        } else if (travelled >= thresholdPx) {
            state.collapsed = false
            travelled = thresholdPx
        }
        return Offset.Zero
    }
}

/** Shrinks the host to [collapsedWidth] x [collapsedHeight] while its content keeps its full-width layout. */
fun Modifier.collapsePill(
    fraction: () -> Float,
    collapsedWidth: Dp,
    collapsedHeight: Dp,
): Modifier =
    layout { measurable, constraints ->
        val f = fraction()
        val fullWidth = constraints.maxWidth
        val placeable = measurable.measure(constraints.copy(minWidth = fullWidth, maxWidth = fullWidth))
        val w = lerpInt(fullWidth, collapsedWidth.roundToPx(), f)
        val h = lerpInt(placeable.height, collapsedHeight.roundToPx(), f)
        layout(w, h) { placeable.placeRelative(0, (h - placeable.height) / 2) }
    }

/** Moves the mini player next to the home circle: narrower from the left and dropped into the navbar row. */
fun Modifier.dockBesideHome(spec: NavCollapseSpec): Modifier =
    layout { measurable, constraints ->
        val shift = ((NavCollapsedCircleSize + NavCollapsedGap).toPx() * spec.fraction()).roundToInt()
        val c =
            if (constraints.hasBoundedWidth) {
                constraints.copy(
                    minWidth = (constraints.minWidth - shift).coerceAtLeast(0),
                    maxWidth = (constraints.maxWidth - shift).coerceAtLeast(0),
                )
            } else {
                constraints
            }
        val placeable = measurable.measure(c)
        layout(placeable.width + shift, placeable.height) { placeable.placeRelative(shift, 0) }
    }.graphicsLayer { translationY = spec.dropY.toPx() * spec.fraction() }

/** Left edge shared by the home circle and the mini player so both sit on one line. */
val NavDockEdge = DockEdgePadding

private fun lerpInt(
    a: Int,
    b: Int,
    f: Float,
): Int = (a + (b - a) * f).roundToInt()
