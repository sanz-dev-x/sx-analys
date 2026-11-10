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
            custom(
                "graphite", R.string.sx_style_graphite, R.string.sx_style_graphite_note, GlassMaterial.MATTE,
                0xFF3D5A80, 0xFF9DB7D5,
                0xFFEDEEF0, 0xFFFFFFFF, 0xFF17181A,
                0xFF101113, 0xFF1A1C1F, 0xFFE8E9EB,
            ),
            custom(
                "paper", R.string.sx_style_paper, R.string.sx_style_paper_note, GlassMaterial.FROSTED,
                0xFFB5532B, 0xFFE0845C,
                0xFFF4EFE6, 0xFFFBF8F2, 0xFF2B2622,
                0xFF1E1B18, 0xFF28241F, 0xFFEDE6DA,
            ),
            custom(
                "ink", R.string.sx_style_ink, R.string.sx_style_ink_note, GlassMaterial.TINTED,
                0xFF2F5FD0, 0xFF6C97F0,
                0xFFEEF1F7, 0xFFFFFFFF, 0xFF111827,
                0xFF0B1220, 0xFF121B2E, 0xFFE6EBF5,
            ),
            custom(
                "forest", R.string.sx_style_forest, R.string.sx_style_forest_note, GlassMaterial.FROSTED,
                0xFF2F6F4F, 0xFF6FB58B,
                0xFFEDF1EC, 0xFFFFFFFF, 0xFF16201A,
                0xFF0D1411, 0xFF151F1A, 0xFFE4EDE7,
            ),
            custom(
                "dusk", R.string.sx_style_dusk, R.string.sx_style_dusk_note, GlassMaterial.METAL,
                0xFFB26A1E, 0xFFE9A15B,
                0xFFF3EEF1, 0xFFFFFFFF, 0xFF241A1F,
                0xFF16121A, 0xFF1F1A25, 0xFFEFE8F2,
            ),
            custom(
                "ocean", R.string.sx_style_ocean, R.string.sx_style_ocean_note, GlassMaterial.CLEAR,
                0xFF0E7C86, 0xFF4FC3CC,
                0xFFEAF2F4, 0xFFFFFFFF, 0xFF0E2128,
                0xFF081418, 0xFF0F2026, 0xFFE0EEF1,
            ),
            custom(
                "sakura", R.string.sx_style_sakura, R.string.sx_style_sakura_note, GlassMaterial.TINTED,
                0xFFD6457A, 0xFFF28BB0,
                0xFFF9EEF2, 0xFFFFFFFF, 0xFF2A1820,
                0xFF1A0F14, 0xFF26161D, 0xFFF6E6EC,
            ),
            custom(
                "midnight", R.string.sx_style_midnight, R.string.sx_style_midnight_note, GlassMaterial.CLEAR,
                0xFF5B6CF0, 0xFF8F9BFF,
                0xFFEDEEF8, 0xFFFFFFFF, 0xFF14163A,
                0xFF07081A, 0xFF0E1030, 0xFFE7E9FF,
            ),
            custom(
                "aurora", R.string.sx_style_aurora, R.string.sx_style_aurora_note, GlassMaterial.CLEAR,
                0xFF1FA67A, 0xFF52E0B0,
                0xFFE9F5F1, 0xFFFFFFFF, 0xFF0F2A22,
                0xFF06161A, 0xFF0C2229, 0xFFDDF5EE,
            ),
            custom(
                "sunset", R.string.sx_style_sunset, R.string.sx_style_sunset_note, GlassMaterial.METAL,
                0xFFD9531E, 0xFFFF8A5B,
                0xFFFBEFE8, 0xFFFFFFFF, 0xFF2A1710,
                0xFF1B0F0B, 0xFF2A1812, 0xFFF8E8DF,
            ),
            custom(
                "lavender", R.string.sx_style_lavender, R.string.sx_style_lavender_note, GlassMaterial.FROSTED,
                0xFF7E57C2, 0xFFB39DDB,
                0xFFF1ECF8, 0xFFFFFFFF, 0xFF21163A,
                0xFF120E1D, 0xFF1C1530, 0xFFECE6F8,
            ),
            custom(
                "mono", R.string.sx_style_mono, R.string.sx_style_mono_note, GlassMaterial.MATTE,
                0xFF222222, 0xFFEDEDED,
                0xFFF2F2F2, 0xFFFFFFFF, 0xFF111111,
                0xFF0A0A0A, 0xFF161616, 0xFFF0F0F0,
            ),
            custom(
                "neon", R.string.sx_style_neon, R.string.sx_style_neon_note, GlassMaterial.INK,
                0xFF00A8C6, 0xFF22E5FF,
                0xFFEAF6F9, 0xFFFFFFFF, 0xFF0A1A20,
                0xFF07070F, 0xFF11111F, 0xFFE9F9FF,
            ),
            custom(
                "coffee", R.string.sx_style_coffee, R.string.sx_style_coffee_note, GlassMaterial.MATTE,
                0xFF8B5A3C, 0xFFC8946D,
                0xFFF3ECE4, 0xFFFFFFFF, 0xFF2B1E16,
                0xFF17100B, 0xFF21170F, 0xFFEFE4D8,
            ),
            custom(
                "rosegold", R.string.sx_style_rosegold, R.string.sx_style_rosegold_note, GlassMaterial.METAL,
                0xFFB76E79, 0xFFE3A3AC,
                0xFFF7ECEC, 0xFFFFFFFF, 0xFF2B1A1D,
                0xFF1C1214, 0xFF281A1D, 0xFFF5E6E8,
            ),
            custom(
                "arctic", R.string.sx_style_arctic, R.string.sx_style_arctic_note, GlassMaterial.CLEAR,
                0xFF2A7FBA, 0xFF7CC4F2,
                0xFFEDF4F9, 0xFFFFFFFF, 0xFF10222F,
                0xFF0A141B, 0xFF11212B, 0xFFE3F0F8,
            ),
            custom(
                "volcano", R.string.sx_style_volcano, R.string.sx_style_volcano_note, GlassMaterial.INK,
                0xFFD1342A, 0xFFFF6B5E,
                0xFFF8ECEA, 0xFFFFFFFF, 0xFF2A1412,
                0xFF140A09, 0xFF211210, 0xFFF7E4E1,
            ),
            custom(
                "oled", R.string.sx_style_oled, R.string.sx_style_oled_note, GlassMaterial.INK,
                0xFFFF8A4C, 0xFFFF8A4C,
                0xFFFFFFFF, 0xFFF4F4F4, 0xFF111111,
                0xFF000000, 0xFF0D0D0D, 0xFFFFFFFF,
            ),
            custom(
                "mint", R.string.sx_style_mint, R.string.sx_style_mint_note, GlassMaterial.FROSTED,
                0xFF2BA58B, 0xFF6FD6BE,
                0xFFEAF7F3, 0xFFFFFFFF, 0xFF0E2A24,
                0xFF0A1714, 0xFF11211D, 0xFFE0F5EF,
            ),
            custom(
                "steel", R.string.sx_style_steel, R.string.sx_style_steel_note, GlassMaterial.METAL,
                0xFF4A5A6A, 0xFFAFC0D0,
                0xFFEEF1F4, 0xFFFFFFFF, 0xFF17202A,
                0xFF0E1318, 0xFF171E25, 0xFFE6ECF2,
            ),
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
