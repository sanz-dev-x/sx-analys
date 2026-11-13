package com.sxanalys.music.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Display
import android.view.Window
import kotlin.math.abs
import kotlin.math.roundToInt

tailrec fun Context.findHostActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findHostActivity()
        else -> null
    }

@Suppress("DEPRECATION")
private fun Window.sameResolutionModes(): List<Display.Mode> {
    val display = windowManager.defaultDisplay
    val current = display.mode
    return display.supportedModes.filter {
        it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
    }
}

fun Window.supportedRefreshRates(): List<Int> =
    sameResolutionModes()
        .map { it.refreshRate.roundToInt() }
        .filter { it >= 60 }
        .distinct()
        .sorted()

fun Window.requestRefreshRate(rate: Int) {
    val modes = sameResolutionModes()
    val target =
        if (rate == Int.MAX_VALUE) {
            modes.maxByOrNull { it.refreshRate }
        } else {
            modes.minByOrNull { abs(it.refreshRate - rate) }
        } ?: return
    val params = attributes
    params.preferredDisplayModeId = target.modeId
    params.preferredRefreshRate = target.refreshRate
    attributes = params
}
