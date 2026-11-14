@file:android.annotation.SuppressLint("NewApi")

package com.sxanalys.music.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

// Water-glass distortion: the image stays sharp in the middle and bends toward the rim.
// No highlights, no specular streaks, no light reflection is added.
private const val WATER_AGSL =
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
        float d = -sdf(p);
        if (d >= depth) {
            return content.eval(p);
        }
        float e = 1.0;
        float2 n = float2(sdf(p + float2(e, 0.0)) - sdf(p - float2(e, 0.0)),
                          sdf(p + float2(0.0, e)) - sdf(p - float2(0.0, e)));
        float len = length(n);
        n = len > 0.0001 ? n / len : float2(0.0, -1.0);
        float t = clamp(1.0 - d / depth, 0.0, 1.0);
        float bend = pow(t, 1.5) * depth * 1.2;
        float2 hi = size - float2(1.0);
        float2 ps = clamp(p - n * bend, float2(0.0), hi);
        float2 pr = clamp(p - n * bend * 1.10, float2(0.0), hi);
        float2 pb = clamp(p - n * bend * 0.90, float2(0.0), hi);
        half4 base = content.eval(ps);
        return half4(content.eval(pr).r, base.g, content.eval(pb).b, base.a);
    }
    """

/** Applies the water-glass refraction (API 33+) to whatever this modifier draws. No-op below API 33. */
fun Modifier.waterDistortion(
    cornerFraction: Float = 0.5f,
    depthFraction: Float = 0.34f,
): Modifier =
    composed {
        var box by remember { mutableStateOf(IntSize.Zero) }
        val shader = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { RuntimeShader(WATER_AGSL) }.getOrNull() else null }
        this
            .onSizeChanged { box = it }
            .graphicsLayer {
                val s = shader
                if (s != null && box.width > 0 && box.height > 0) {
                    val minDim = minOf(box.width, box.height).toFloat()
                    s.setFloatUniform("size", box.width.toFloat(), box.height.toFloat())
                    s.setFloatUniform("cr", (minDim * cornerFraction).coerceIn(0f, minDim / 2f))
                    s.setFloatUniform("depth", minOf(minDim * depthFraction, 30.dp.toPx()))
                    renderEffect = RenderEffect.createRuntimeShaderEffect(s, "content").asComposeRenderEffect()
                }
            }
    }

/** Sharp full-bleed image with water-glass distortion and a plain dark scrim (for text contrast only). */
@Composable
fun WaterGlassImage(
    model: Any?,
    shape: Shape,
    modifier: Modifier = Modifier,
    scrim: Float = 0.22f,
    cornerFraction: Float = 0.5f,
    blurRadius: androidx.compose.ui.unit.Dp = 0.dp,
    zoom: Float = 1f,
) {
    Box(modifier.clip(shape)) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .waterDistortion(cornerFraction)
                    .then(if (zoom != 1f) Modifier.graphicsLayer { scaleX = zoom; scaleY = zoom } else Modifier)
                    .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrim)))
    }
}
