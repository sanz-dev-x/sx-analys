package com.sxanalys.music.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.sxanalys.music.R

enum class GlassMaterial(
    val id: String,
    @StringRes val label: Int,
    @StringRes val note: Int,
) {
    CLEAR("clear", R.string.sx_glass_clear, R.string.sx_glass_clear_note),
    FROSTED("frosted", R.string.sx_glass_frosted, R.string.sx_glass_frosted_note),
    TINTED("tinted", R.string.sx_glass_tinted, R.string.sx_glass_tinted_note),
    METAL("metal", R.string.sx_glass_metal, R.string.sx_glass_metal_note),
    MATTE("matte", R.string.sx_glass_matte, R.string.sx_glass_matte_note),
    OUTLINE("outline", R.string.sx_glass_outline, R.string.sx_glass_outline_note),
    INK("ink", R.string.sx_glass_ink, R.string.sx_glass_ink_note),
    ;

    companion object {
        fun byId(id: String): GlassMaterial? = entries.firstOrNull { it.id == id }
    }
}

val DefaultGlassAccent = Color(0xFFF0904F)

val LocalGlassMaterial = staticCompositionLocalOf { GlassMaterial.CLEAR }
val LocalGlassAccent = staticCompositionLocalOf { DefaultGlassAccent }
val LocalLargeNav = staticCompositionLocalOf { false }

enum class GlassMode(
    val id: String,
) {
    OFF("off"),
    DARK("dark"),
    BRIGHT("bright"),
    ;

    companion object {
        fun byId(id: String?): GlassMode = entries.firstOrNull { it.id == id } ?: OFF
    }
}

val LocalGlassMode = staticCompositionLocalOf { GlassMode.OFF }

class AppStyle(
    val id: String,
    @StringRes val label: Int,
    @StringRes val note: Int,
    val ios: Boolean,
    val overrides: Boolean,
    val glass: GlassMaterial,
    val accentLight: Color,
    val accentDark: Color,
    val bgLight: Color,
    val cardLight: Color,
    val inkLight: Color,
    val bgDark: Color,
    val cardDark: Color,
    val inkDark: Color,
) {
    fun accent(dark: Boolean): Color = if (dark) accentDark else accentLight

    fun bg(dark: Boolean): Color = if (dark) bgDark else bgLight

    fun card(dark: Boolean): Color = if (dark) cardDark else cardLight

    fun ink(dark: Boolean): Color = if (dark) inkDark else inkLight

    fun scheme(
        base: ColorScheme,
        dark: Boolean,
    ): ColorScheme {
        if (!overrides) return base
        val bg = bg(dark)
        val card = card(dark)
        val ink = ink(dark)
        val accent = accent(dark)
        val onDarkBg = bg.luminance() < 0.4f
        val text = if (onDarkBg) Color(0xFFF2EDE7) else Color(0xFF1E1A17)
        val textMuted = if (onDarkBg) Color(0xFFA8A29B) else Color(0xFF6B645C)
        val onAccent = if (accent.luminance() > 0.55f) Color(0xFF111111) else Color.White
        val secondary = textMuted
        val onSecondary = if (secondary.luminance() > 0.5f) Color(0xFF111111) else Color.White
        val secondaryContainer = lerp(bg, accent, if (dark) 0.20f else 0.12f)
        return base.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = lerp(bg, accent, if (dark) 0.28f else 0.16f),
            onPrimaryContainer = if (dark) lerp(accent, Color.White, 0.55f) else lerp(accent, Color.Black, 0.45f),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = text,
            tertiary = secondary,
            onTertiary = onSecondary,
            tertiaryContainer = secondaryContainer,
            onTertiaryContainer = text,
            background = bg,
            onBackground = text,
            surface = bg,
            onSurface = text,
            surfaceVariant = lerp(bg, ink, 0.08f),
            onSurfaceVariant = textMuted,
            surfaceTint = accent,
            inverseSurface = text,
            inverseOnSurface = bg,
            outline = lerp(bg, ink, 0.45f),
            outlineVariant = lerp(bg, ink, 0.16f),
            surfaceDim = lerp(bg, Color.Black, 0.05f),
            surfaceBright = card,
            surfaceContainerLowest = if (dark) lerp(bg, Color.Black, 0.4f) else card,
            surfaceContainerLow = lerp(bg, card, 0.5f),
            surfaceContainer = card,
            surfaceContainerHigh = lerp(card, ink, 0.05f),
            surfaceContainerHighest = lerp(card, ink, 0.10f),
        )
    }
}

object AppStyles {
    private val iosBgLight = Color(0xFFF2F2F7)
    private val iosCardLight = Color(0xFFFFFFFF)
    private val iosInkLight = Color(0xFF1C1C1E)
    private val iosBgDark = Color(0xFF000000)
    private val iosCardDark = Color(0xFF1C1C1E)
    private val iosInkDark = Color(0xFFF2F2F7)

    private fun ios(
        id: String,
        @StringRes label: Int,
        @StringRes note: Int,
        light: Long,
        dark: Long,
    ) = AppStyle(
        id = id,
        label = label,
        note = note,
        ios = true,
        overrides = true,
        glass = GlassMaterial.CLEAR,
        accentLight = Color(light),
        accentDark = Color(dark),
        bgLight = iosBgLight,
        cardLight = iosCardLight,
        inkLight = iosInkLight,
        bgDark = iosBgDark,
        cardDark = iosCardDark,
        inkDark = iosInkDark,
    )

    private fun custom(
        id: String,
        @StringRes label: Int,
        @StringRes note: Int,
        glass: GlassMaterial,
        accentLight: Long,
        accentDark: Long,
        bgLight: Long,
        cardLight: Long,
        inkLight: Long,
        bgDark: Long,
        cardDark: Long,
        inkDark: Long,
    ) = AppStyle(
        id = id,
        label = label,
        note = note,
        ios = false,
        overrides = true,
        glass = glass,
        accentLight = Color(accentLight),
        accentDark = Color(accentDark),
        bgLight = Color(bgLight),
        cardLight = Color(cardLight),
        inkLight = Color(inkLight),
        bgDark = Color(bgDark),
        cardDark = Color(cardDark),
        inkDark = Color(inkDark),
    )

    val Default =
        AppStyle(
            id = "default",
            label = R.string.sx_style_default,
            note = R.string.sx_style_default_note,
            ios = false,
            overrides = false,
            glass = GlassMaterial.CLEAR,
            accentLight = DefaultGlassAccent,
            accentDark = DefaultGlassAccent,
            bgLight = Color(0xFFF3EEE8),
            cardLight = Color(0xFFFFFFFF),
            inkLight = Color(0xFF1E1A17),
            bgDark = Color(0xFF0E0C0B),
            cardDark = Color(0xFF1B1816),
            inkDark = Color(0xFFF2EDE7),
        )

    val all: List<AppStyle> =
        listOf(
            Default,
            ios("ios_blue", R.string.sx_style_ios_blue, R.string.sx_style_ios_blue_note, 0xFF007AFF, 0xFF0A84FF),
            ios("ios_green", R.string.sx_style_ios_green, R.string.sx_style_ios_green_note, 0xFF34C759, 0xFF30D158),
            ios("ios_indigo", R.string.sx_style_ios_indigo, R.string.sx_style_ios_indigo_note, 0xFF5856D6, 0xFF5E5CE6),
            ios("ios_orange", R.string.sx_style_ios_orange, R.string.sx_style_ios_orange_note, 0xFFFF9500, 0xFFFF9F0A),
            ios("ios_pink", R.string.sx_style_ios_pink, R.string.sx_style_ios_pink_note, 0xFFFF2D55, 0xFFFF375F),
            ios("ios_purple", R.string.sx_style_ios_purple, R.string.sx_style_ios_purple_note, 0xFFAF52DE, 0xFFBF5AF2),
            ios("ios_teal", R.string.sx_style_ios_teal, R.string.sx_style_ios_teal_note, 0xFF30B0C7, 0xFF40C8E0),
            ios("ios_red", R.string.sx_style_ios_red, R.string.sx_style_ios_red_note, 0xFFFF3B30, 0xFFFF453A),
        )

    val LiquidDark =
        AppStyle(
            id = "liquid_dark",
            label = R.string.sx_style_default,
            note = R.string.sx_style_default_note,
            ios = false,
            overrides = true,
            glass = GlassMaterial.CLEAR,
            accentLight = Color(0xFFF0904F),
            accentDark = Color(0xFFF0904F),
            bgLight = Color(0xFF0E0C0B),
            cardLight = Color(0xFF1B1816),
            inkLight = Color(0xFFF2EDE7),
            bgDark = Color(0xFF0E0C0B),
            cardDark = Color(0xFF1B1816),
            inkDark = Color(0xFFF2EDE7),
        )

    val LiquidBright =
        AppStyle(
            id = "liquid_bright",
            label = R.string.sx_style_default,
            note = R.string.sx_style_default_note,
            ios = false,
            overrides = true,
            glass = GlassMaterial.CLEAR,
            accentLight = Color(0xFFAD4A12),
            accentDark = Color(0xFFAD4A12),
            bgLight = Color(0xFFF3EEE8),
            cardLight = Color(0xFFFFFFFF),
            inkLight = Color(0xFF1E1A17),
            bgDark = Color(0xFFF3EEE8),
            cardDark = Color(0xFFFFFFFF),
            inkDark = Color(0xFF1E1A17),
        )

    fun byId(id: String): AppStyle = all.firstOrNull { it.id == id } ?: Default
}
