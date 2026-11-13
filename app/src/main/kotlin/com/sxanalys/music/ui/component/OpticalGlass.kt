@file:android.annotation.SuppressLint("NewApi")

package com.sxanalys.music.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sxanalys.music.ui.theme.GlassMode
import com.sxanalys.music.ui.theme.LocalGlassMode

class GlassBackdropState(
    val layer: GraphicsLayer,
) {
    var origin: Offset = Offset.Zero
    var ready: Boolean = false
    var view: View? = null
}

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdropState?> { null }

val LocalGlassRefraction = staticCompositionLocalOf { 1f }

private const val REFRACT_AGSL =
    """
    uniform shader content;
    uniform float2 size;
    uniform float cr;
    uniform float depth;

    float sdf(float2 p) {
        float2 c = size * 0.5;
        float2 q = abs(p - c) - (c - float2(cr));
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - cr;
    }

    half4 main(float2 p) {
        float sd = sdf(p);
        float d = -sd;
        if (d >= depth) {
            return content.eval(p);
        }
        float e = 1.0;
        float2 n = float2(sdf(p + float2(e, 0.0)) - sdf(p - float2(e, 0.0)),
                          sdf(p + float2(0.0, e)) - sdf(p - float2(0.0, e)));
        float len = length(n);
        n = len > 0.0001 ? n / len : float2(0.0, -1.0);
        float t = clamp(1.0 - d / depth, 0.0, 1.0);
        float bend = pow(t, 1.6) * depth * 1.15;
        float2 hi = size - float2(1.0);
        float2 ps = clamp(p - n * bend, float2(0.0), hi);
        float2 pr = clamp(p - n * bend * 1.14, float2(0.0), hi);
        float2 pb = clamp(p - n * bend * 0.86, float2(0.0), hi);
        half4 base = content.eval(ps);
        half r = content.eval(pr).r;
        half b = content.eval(pb).b;
        half3 col = half3(r, base.g, b);
        float light = dot(n, normalize(float2(-0.55, -0.85))) * 0.5 + 0.5;
        float fres = pow(t, 3.0);
        col += half3((0.05 + 0.34 * light) * fres);
        col *= half(1.0 - 0.22 * pow(t, 2.0) * (1.0 - light));
        return half4(col, 1.0);
    }
    """

private class RefractionHolder {
    var layer: GraphicsLayer? = null
    var shader: RuntimeShader? = null
    var w = -1f
    var h = -1f
    var cr = -1f
    var depth = -1f

    fun obtain(context: GraphicsContext): GraphicsLayer = layer ?: context.createGraphicsLayer().also { layer = it }

    fun configure(
        l: GraphicsLayer,
        size: Size,
        corner: Float,
        depthPx: Float,
    ) {
        if (shader == null) shader = runCatching { RuntimeShader(REFRACT_AGSL) }.getOrNull()
        val s = shader ?: return
        if (size.width == w && size.height == h && corner == cr && depthPx == depth) return
        s.setFloatUniform("size", size.width, size.height)
        s.setFloatUniform("cr", corner)
        s.setFloatUniform("depth", depthPx)
        w = size.width
        h = size.height
        cr = corner
        depth = depthPx
        l.renderEffect = RenderEffect.createRuntimeShaderEffect(s, "content").asComposeRenderEffect()
    }

    fun release(context: GraphicsContext) {
        layer?.let { context.releaseGraphicsLayer(it) }
        layer = null
    }
}

@Composable
fun GlassBackdropSource(
    mode: GlassMode,
    playerConnection: com.sxanalys.music.playback.PlayerConnection?,
) {
    val state = LocalGlassBackdrop.current
    val view = LocalView.current
    if (state == null || mode == GlassMode.OFF || Build.VERSION.SDK_INT < 33) {
        LiquidBackdropLayer(mode)
        GlassArtworkBackdrop(playerConnection = playerConnection)
        return
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    state.origin = it.positionInWindow()
                    state.view = view
                }.drawWithContent {
                    state.layer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                        this@drawWithContent.drawContent()
                    }
                    state.ready = true
                    drawLayer(state.layer)
                }.liquidBackdrop(mode),
    ) {
        LiquidBackdropLayer(mode)
        GlassArtworkBackdrop(playerConnection = playerConnection)
    }
}

@Composable
internal fun rememberOpticalBackdrop(): GlassBackdropState? {
    val state = LocalGlassBackdrop.current ?: return null
    if (Build.VERSION.SDK_INT < 33) return null
    if (LocalGlassRefraction.current <= 0f) return null
    if (LocalGlassQuality.current == GlassQuality.LOW) return null
    if (LocalGlassMode.current == GlassMode.OFF) return null
    if (state.view != null && state.view !== LocalView.current) return null
    return state
}

@Composable
internal fun Modifier.opticalRefraction(
    backdrop: GlassBackdropState,
    shape: Shape,
    strength: Float = 1f,
): Modifier {
    val context = LocalGraphicsContext.current
    val holder = remember { RefractionHolder() }
    var pos by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(holder) { onDispose { holder.release(context) } }
    return this
        .onGloballyPositioned { pos = it.positionInWindow() }
        .drawWithContent {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            if (backdrop.ready && minDim >= 24.dp.toPx()) {
                val outline = shape.createOutline(size, layoutDirection, this)
                val corner =
                    when (outline) {
                        is Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
                        else -> minDim / 2f
                    }.coerceIn(0f, minDim / 2f)
                val depthPx = (minOf(minDim * 0.34f, 26.dp.toPx()) * strength.coerceIn(0.3f, 1.5f)).coerceAtMost(minDim * 0.5f)
                val layer = holder.obtain(context)
                holder.configure(layer, size, corner, depthPx)
                layer.record(this, layoutDirection, IntSize(w.toInt(), h.toInt())) {
                    translate(backdrop.origin.x - pos.x, backdrop.origin.y - pos.y) {
                        drawLayer(backdrop.layer)
                    }
                }
                drawLayer(layer)
            }
            drawContent()
        }
}
