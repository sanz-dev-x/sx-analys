package com.sxanalys.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import com.sxanalys.music.ui.theme.GlassMaterial
import com.sxanalys.music.ui.theme.GlassMode
import com.sxanalys.music.ui.theme.LocalGlassMode
import com.sxanalys.music.constants.GlassModeKey
import com.sxanalys.music.utils.rememberPreference
import com.sxanalys.music.ui.theme.LocalGlassAccent
import com.sxanalys.music.ui.theme.LocalGlassMaterial
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val GlassAccent = Color(0xFFFF8A4C)
val GlassPurple = Color(0xFF7C5CFA)

private class GlassSpec(
    val fill: Color,
    val sheen: Brush?,
    val border: Brush,
    val gloss: Float,
    val bevel: Float,
)

private fun glassSpec(
    material: GlassMaterial,
    accent: Color,
    surface: Color,
    ink: Color,
    base: Color,
): GlassSpec =
    when (material) {
        GlassMaterial.CLEAR ->
            GlassSpec(
                base,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.02f), accent.copy(alpha = 0.06f)),
                ),
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.04f), accent.copy(alpha = 0.30f)),
                ),
                gloss = 1f,
                bevel = 1f,
            )
        GlassMaterial.FROSTED ->
            GlassSpec(
                surface.copy(alpha = 0.82f),
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))),
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))),
                gloss = 0.6f,
                bevel = 0.8f,
            )
        GlassMaterial.TINTED ->
            GlassSpec(
                base,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.10f))),
                Brush.linearGradient(listOf(accent.copy(alpha = 0.70f), accent.copy(alpha = 0.20f))),
                gloss = 0.9f,
                bevel = 1f,
            )
        GlassMaterial.METAL ->
            GlassSpec(
                base,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f), Color.Black.copy(alpha = 0.12f)),
                ),
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.50f), ink.copy(alpha = 0.10f))),
                gloss = 1.25f,
                bevel = 1.2f,
            )
        GlassMaterial.MATTE ->
            GlassSpec(lerp(surface, ink, 0.08f), null, SolidColor(ink.copy(alpha = 0.10f)), gloss = 0.15f, bevel = 0.6f)
        GlassMaterial.OUTLINE ->
            GlassSpec(Color.Transparent, null, SolidColor(ink.copy(alpha = 0.30f)), gloss = 0f, bevel = 0f)
        GlassMaterial.INK ->
            GlassSpec(
                Color.Black.copy(alpha = 0.62f),
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)),
                SolidColor(Color.White.copy(alpha = 0.14f)),
                gloss = 0.5f,
                bevel = 1.1f,
            )
    }

internal fun Modifier.glassVolume(
    shape: Shape,
    gloss: Float,
    bevel: Float,
    caustic: Color,
    edge: Brush? = null,
    lite: Boolean = false,
): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val w = size.width
        val h = size.height
        val m = minOf(w, h).coerceAtLeast(1f)
        val edgeStroke = Stroke(width = 2.dp.toPx())
        val bevelStroke = Stroke(width = 8.dp.toPx())
        val bevelBrush =
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.30f * bevel),
                0.30f to Color.Transparent,
                0.70f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.30f * bevel),
                startY = 0f,
                endY = h,
            )
        val topSheen =
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.20f * gloss),
                0.45f to Color.Transparent,
                startY = 0f,
                endY = h,
            )
        val hotSpot =
            if (lite) null else Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.34f * gloss), Color.Transparent),
                center = Offset(w * 0.24f, h * 0.16f),
                radius = m * 0.9f,
            )
        val bounce =
            if (lite) null else Brush.radialGradient(
                colors = listOf(caustic.copy(alpha = 0.20f * gloss), Color.Transparent),
                center = Offset(w * 0.5f, h * 1.02f),
                radius = maxOf(m, w * 0.3f),
            )
        val rim =
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.75f * gloss),
                0.45f to Color.White.copy(alpha = 0.05f * gloss),
                1f to Color.White.copy(alpha = 0.30f * gloss),
                start = Offset.Zero,
                end = Offset(w, h),
            )
        onDrawWithContent {
            if (gloss > 0f) {
                drawRect(topSheen)
                if (hotSpot != null) drawRect(hotSpot)
                if (bounce != null) drawRect(bounce)
            }
            if (bevel > 0f && !lite) drawOutline(outline, bevelBrush, style = bevelStroke)
            drawContent()
            if (edge != null) drawOutline(outline, edge, style = edgeStroke)
            if (gloss > 0f) drawOutline(outline, rim, style = edgeStroke)
        }
    }

@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    fill: Color = Color.Transparent,
    depth: Dp = 0.dp,
): Modifier {
    val material = LocalGlassMaterial.current
    val accent = LocalGlassAccent.current
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val spec = remember(material, accent, surface, ink, fill) { glassSpec(material, accent, surface, ink, fill) }
    val sheen = spec.sheen
    val lite = LocalGlassQuality.current == GlassQuality.LOW
    return this
        .then(
            if (depth > 0.dp && material != GlassMaterial.OUTLINE && !lite) {
                Modifier.shadow(
                    elevation = depth,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.34f),
                )
            } else {
                Modifier
            },
        ).clip(shape)
        .background(spec.fill)
        .then(if (sheen != null) Modifier.background(brush = sheen, shape = shape) else Modifier)
        .glassVolume(shape, spec.gloss, spec.bevel, accent, spec.border, lite)
}

@Composable
fun Modifier.glassPress(
    interactionSource: InteractionSource,
    shape: Shape = CircleShape,
    pressedScale: Float = 0.94f,
): Modifier {
    val accent = LocalGlassAccent.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "glassPressScale",
    )
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glassPressGlow",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .drawWithContent {
            drawContent()
            val g = glow.coerceIn(0f, 1f)
            if (g > 0f) {
                drawRect(color = accent.copy(alpha = 0.18f * g))
            }
        }
}

@Composable
fun Modifier.glassChip(selected: Boolean, shape: Shape): Modifier {
    val accent = LocalGlassAccent.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fill = if (selected) accent.copy(alpha = 0.20f) else onSurface.copy(alpha = 0.06f)
    val line = if (selected) accent.copy(alpha = 0.60f) else onSurface.copy(alpha = 0.16f)
    return this
        .clip(shape)
        .background(fill)
        .border(1.dp, line, shape)
}

@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(if (small) 14.dp else 20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(if (small) 40.dp else 56.dp)
            .shadow(
                elevation = if (small) 6.dp else 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.38f),
            ).glassPress(interactionSource, shape)
            .liquidGlass(shape, fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            content = content,
        )
    }
}

object GlassIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        GlassIndicationNode(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other === this
}

private class GlassIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val progress = Animatable(0f)
    private var pressPosition: Offset = Offset.Unspecified

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressPosition = interaction.pressPosition
                        launch {
                            progress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh))
                        }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        launch {
                            progress.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val p = progress.value
        if (p <= 0f) {
            drawContent()
            return
        }
        val large = size.width > 280.dp.toPx() || size.height > 280.dp.toPx()
        val s = 1f - (if (large) 0.012f else 0.04f) * p
        val contentScope = this
        scale(s, s) { contentScope.drawContent() }
        val g = p.coerceIn(0f, 1f)
        if (g > 0f) {
            drawRect(color = currentValueOf(LocalContentColor).copy(alpha = 0.10f * g))
        }
    }
}


/**
 * Tombol kaca 3D: bayangan berlapis berwarna, badan kaca dengan bevel dan kilau spekular
 * dari liquidGlass, tanpa blur sehingga ringan.
 */
@Composable
fun Modifier.glassButton3D(
    shape: Shape,
    tint: Color,
    tintAlpha: Float = 0.88f,
): Modifier =
    this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.35f),
            spotColor = tint.copy(alpha = 0.45f),
        ).liquidGlass(shape, fill = tint.copy(alpha = tintAlpha))

/**
 * Kartu sampul 3D: miring tipis dengan perspektif, bayangan dalam, tepi kaca,
 * dan kilau diagonal. Tanpa blur sehingga ringan.
 */
@Composable
fun Modifier.artwork3D(
    shape: Shape,
    tiltX: Float = 4f,
    tiltY: Float = -6f,
): Modifier =
    this
        .graphicsLayer {
            rotationX = tiltX
            rotationY = tiltY
            cameraDistance = 14f * density
        }.shadow(
            elevation = 26.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.5f),
            spotColor = Color.Black.copy(alpha = 0.6f),
        ).clip(shape)
        .drawWithContent {
            drawContent()
            drawRect(
                brush =
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.22f),
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.18f),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
            )
        }.border(
            width = 1.2.dp,
            brush =
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.3f)),
                ),
            shape = shape,
        )


/** Warna dasar kaca navbar/mini player: selalu gelap pada mode Liquid Glass, cukup pekat agar konten di belakang tidak tembus. */
val LiquidGlassDarkBase = Color(0xFF090C16)

/**
 * Latar belakang Liquid Glass: gradien dasar dengan tiga cahaya lembut.
 * Statis (digambar sekali per ukuran), jadi tidak membebani render.
 */
fun Modifier.liquidBackdrop(mode: GlassMode): Modifier {
    if (mode == GlassMode.OFF) return this
    val dark = mode == GlassMode.DARK
    return drawWithCache {
        val w = size.width
        val h = size.height
        val base =
            if (dark) {
                Brush.verticalGradient(listOf(Color(0xFF0B1022), Color(0xFF080B16), Color(0xFF0A0A14)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFF3F7FE), Color(0xFFE6EEFA), Color(0xFFEEF0FB)))
            }
        val a = if (dark) 0.30f else 0.34f
        val blobA =
            Brush.radialGradient(
                listOf((if (dark) Color(0xFF3F6BFF) else Color(0xFF7FB0FF)).copy(alpha = a), Color.Transparent),
                center = Offset(w * 0.12f, h * 0.10f),
                radius = w * 0.95f,
            )
        val blobB =
            Brush.radialGradient(
                listOf((if (dark) Color(0xFF8B4DFF) else Color(0xFFC9A6FF)).copy(alpha = a * 0.85f), Color.Transparent),
                center = Offset(w * 0.95f, h * 0.45f),
                radius = w * 0.9f,
            )
        val blobC =
            Brush.radialGradient(
                listOf((if (dark) Color(0xFF00B8C8) else Color(0xFF8FE3EC)).copy(alpha = a * 0.7f), Color.Transparent),
                center = Offset(w * 0.2f, h * 0.95f),
                radius = w * 1.0f,
            )
        onDrawBehind {
            drawRect(base)
            drawRect(blobA)
            drawRect(blobB)
            drawRect(blobC)
        }
    }
}


/** Mode Liquid Glass aktif, dibaca dari LocalGlassMode yang disediakan tema (tanpa collector DataStore per pemanggil). */
@Composable
fun rememberGlassMode(): GlassMode = LocalGlassMode.current

/** Warna latar layar: transparan di mode Liquid Glass agar backdrop global terlihat di semua halaman. */
@Composable
fun screenBackground(): Color =
    if (rememberGlassMode() != GlassMode.OFF) Color.Transparent else MaterialTheme.colorScheme.background
