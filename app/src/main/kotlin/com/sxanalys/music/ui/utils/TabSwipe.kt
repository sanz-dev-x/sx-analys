package com.sxanalys.music.ui.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Mendeteksi geser horizontal satu jari yang tidak diambil oleh anak (list, carousel, slider).
 * [onSwipe] menerima +1 untuk geser ke kiri (tab berikutnya) dan -1 untuk geser ke kanan.
 */
fun Modifier.tabSwipe(
    enabled: Boolean,
    onSwipe: (Int) -> Unit,
): Modifier =
    if (!enabled) {
        this
    } else {
        composed {
            val latest by rememberUpdatedState(onSwipe)
            pointerInput(Unit) {
                val triggerPx = 72.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Final)
                    var dx = 0f
                    var dy = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.changes.size > 1) return@awaitEachGesture
                        val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                        if (change.isConsumed) return@awaitEachGesture
                        dx += change.position.x - change.previousPosition.x
                        dy += change.position.y - change.previousPosition.y
                        if (!change.pressed) break
                    }
                    if (abs(dx) >= triggerPx && abs(dx) > abs(dy) * 2f) {
                        latest(if (dx < 0f) 1 else -1)
                    }
                }
            }
        }
    }
