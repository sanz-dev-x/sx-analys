package com.sxanalys.music.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

// Light reflections removed: glass is pure refraction of the artwork backdrop.
@Suppress("UNUSED_PARAMETER")
internal fun Modifier.wetGlass(
    shape: Shape,
    intensity: Float = 1f,
): Modifier = this
