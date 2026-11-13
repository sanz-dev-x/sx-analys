/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.sxanalys.music.constants.AppStyleKey
import com.sxanalys.music.constants.GlassMaterialKey
import com.sxanalys.music.constants.GlassModeKey
import com.sxanalys.music.constants.GlassRefractionKey
import com.sxanalys.music.constants.NavSizeKey
import com.sxanalys.music.ui.component.GlassIndication
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.sxanalys.music.utils.dataStore
import com.sxanalys.music.utils.rememberPreference
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

private val IosShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(14.dp),
    )

private fun ios(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Float = 0f) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = spacing.sp)

/** SF-style type scale: 17pt body, 34/28/22/20 titles, 13/12/11 captions. */
private val IosTypography =
    Typography(
        displayLarge = ios(34, 41, FontWeight.Bold, 0.4f),
        displayMedium = ios(34, 41, FontWeight.Bold, 0.4f),
        displaySmall = ios(28, 34, FontWeight.Bold, 0.36f),
        headlineLarge = ios(28, 34, FontWeight.Bold, 0.36f),
        headlineMedium = ios(24, 30, FontWeight.SemiBold),
        headlineSmall = ios(20, 25, FontWeight.SemiBold, 0.38f),
        titleLarge = ios(22, 28, FontWeight.SemiBold),
        titleMedium = ios(17, 22, FontWeight.SemiBold, -0.4f),
        titleSmall = ios(15, 20, FontWeight.SemiBold, -0.24f),
        bodyLarge = ios(17, 22, FontWeight.Normal, -0.4f),
        bodyMedium = ios(15, 20, FontWeight.Normal, -0.24f),
        bodySmall = ios(13, 18, FontWeight.Normal, -0.08f),
        labelLarge = ios(17, 22, FontWeight.Medium, -0.4f),
        labelMedium = ios(13, 18, FontWeight.Normal, -0.08f),
        labelSmall = ios(11, 13, FontWeight.Medium, 0.06f),
    )

val DefaultThemeColor = Color(0xFFED5564)
val BrandSeedColor = Color(0xFFF0904F)

@Composable
fun SxanalysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useSystemDynamicColor = false

    val baseColorScheme = if (useSystemDynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = if (themeColor == DefaultThemeColor) BrandSeedColor else themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    val stored = remember { runBlocking { context.dataStore.data.first() } }
    val styleId by rememberPreference(AppStyleKey, defaultValue = stored[AppStyleKey] ?: "ios_blue")
    val glassId by rememberPreference(GlassMaterialKey, defaultValue = stored[GlassMaterialKey] ?: "auto")
    val navSize by rememberPreference(NavSizeKey, defaultValue = stored[NavSizeKey] ?: "auto")
    val glassModeId by rememberPreference(GlassModeKey, defaultValue = stored[GlassModeKey] ?: "dark")
    val glassMode = GlassMode.byId(glassModeId)
    val refractionId by rememberPreference(GlassRefractionKey, defaultValue = "normal")
    val refraction =
        when (refractionId) {
            "off" -> 0f
            "soft" -> 0.6f
            "strong" -> 1.4f
            else -> 1f
        }
    val style =
        remember(styleId, glassMode) {
            when (glassMode) {
                GlassMode.DARK -> AppStyles.LiquidDark
                GlassMode.BRIGHT -> AppStyles.LiquidBright
                GlassMode.OFF -> AppStyles.byId(styleId)
            }
        }
    val glass =
        remember(glassId, style, glassMode) {
            if (glassMode != GlassMode.OFF) GlassMaterial.CLEAR else GlassMaterial.byId(glassId) ?: style.glass
        }
    val largeNav =
        if (glassMode != GlassMode.OFF) {
            false
        } else {
            when (navSize) {
                "large" -> true
                "normal" -> false
                else -> style.ios
            }
        }

    val blendedScheme = remember(baseColorScheme, darkTheme, style) {
        if (style.overrides) {
            style.scheme(baseColorScheme, darkTheme)
        } else if (darkTheme) {
            baseColorScheme.copy(
                tertiary = Color(0xFFFFB68A),
                onTertiary = Color(0xFF4E2300),
                tertiaryContainer = Color(0xFF703610),
                onTertiaryContainer = Color(0xFFFFDBC8),
            )
        } else {
            baseColorScheme.copy(
                tertiary = Color(0xFF9A4A1A),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFFFDBC8),
                onTertiaryContainer = Color(0xFF351000),
            )
        }
    }

    val colorScheme = remember(blendedScheme, pureBlack, darkTheme, glassMode) {
        if (darkTheme && pureBlack && glassMode == GlassMode.OFF) {
            blendedScheme.pureBlack(true)
        } else {
            blendedScheme
        }
    }

    val glassQuality = remember(context) { com.sxanalys.music.ui.component.detectGlassQuality(context) }

    val backdropLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val backdropState = remember(backdropLayer) { com.sxanalys.music.ui.component.GlassBackdropState(backdropLayer) }

    MaterialTheme(colorScheme = colorScheme, shapes = IosShapes, typography = IosTypography) {
        CompositionLocalProvider(
            com.sxanalys.music.ui.component.LocalGlassBackdrop provides backdropState,
            com.sxanalys.music.ui.component.LocalGlassRefraction provides refraction,
            LocalIndication provides GlassIndication,
            LocalGlassMaterial provides glass,
            LocalGlassAccent provides style.accent(darkTheme),
            LocalLargeNav provides largeNav,
            LocalGlassMode provides glassMode,
            com.sxanalys.music.ui.component.LocalGlassQuality provides glassQuality,
            content = content,
        )
    }
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
