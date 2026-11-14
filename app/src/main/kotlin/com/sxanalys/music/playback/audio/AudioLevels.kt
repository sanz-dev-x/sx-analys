package com.sxanalys.music.playback.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/** Level audio 4 pita (bass ke treble) dari PCM yang sedang diputar, dibaca oleh Dynamic Island. */
object AudioLevels {
    const val BANDS = 4

    @Volatile
    var levels = FloatArray(BANDS)
        private set

    private val lp = FloatArray(3)
    private val smooth = FloatArray(BANDS)

    fun analyze(buf: ByteBuffer, channels: Int, sampleRate: Int) {
        if (channels <= 0 || sampleRate <= 0) return
        val b = buf.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val frames = b.remaining() / 2 / channels
        if (frames <= 0) return
        val step = maxOf(1, frames / 512)
        val a = floatArrayOf(coef(200f, sampleRate), coef(1200f, sampleRate), coef(5000f, sampleRate))
        val sum = FloatArray(BANDS)
        var n = 0
        var i = 0
        val base = b.position()
        while (i < frames) {
            var m = 0f
            for (c in 0 until channels) m += b.getShort(base + (i * channels + c) * 2) / 32768f
            m /= channels
            // Filter dijalankan pada tiap sampel terpilih; cukup untuk visual.
            lp[0] += a[0] * (m - lp[0])
            lp[1] += a[1] * (m - lp[1])
            lp[2] += a[2] * (m - lp[2])
            val bass = lp[0]
            val low = lp[1] - lp[0]
            val mid = lp[2] - lp[1]
            val high = m - lp[2]
            sum[0] += bass * bass
            sum[1] += low * low
            sum[2] += mid * mid
            sum[3] += high * high
            n++
            i += step
        }
        if (n == 0) return
        val gain = floatArrayOf(5.5f, 7f, 9f, 14f)
        val out = FloatArray(BANDS)
        for (k in 0 until BANDS) {
            val raw = (sqrt(sum[k] / n) * gain[k]).coerceIn(0f, 1f)
            val prev = smooth[k]
            smooth[k] = if (raw > prev) raw else prev * 0.82f + raw * 0.18f
            out[k] = smooth[k]
        }
        levels = out
    }

    fun reset() {
        smooth.fill(0f)
        lp.fill(0f)
        levels = FloatArray(BANDS)
    }

    private fun coef(fc: Float, sr: Int): Float {
        val x = 2f * Math.PI.toFloat() * fc / sr
        return x / (x + 1f)
    }
}
