package com.sxanalys.music.ui.component

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassQuality { LOW, MEDIUM, HIGH }

val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.MEDIUM }

fun detectGlassQuality(context: Context): GlassQuality {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return GlassQuality.MEDIUM
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val saver = pm?.isPowerSaveMode == true
    val sdk = Build.VERSION.SDK_INT
    val memClass = am.memoryClass
    return when {
        am.isLowRamDevice || saver || sdk < 29 || memClass < 128 -> GlassQuality.LOW
        sdk >= 33 && memClass >= 256 -> GlassQuality.HIGH
        else -> GlassQuality.MEDIUM
    }
}

@Composable
@ReadOnlyComposable
fun glassBlur(requested: Dp): Dp {
    val cap =
        when (LocalGlassQuality.current) {
            GlassQuality.HIGH -> 48.dp
            GlassQuality.MEDIUM -> 32.dp
            GlassQuality.LOW -> 20.dp
        }
    return if (requested > cap) cap else requested
}
