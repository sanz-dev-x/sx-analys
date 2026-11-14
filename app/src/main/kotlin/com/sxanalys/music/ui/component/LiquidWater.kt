package com.sxanalys.music.ui.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Mesin air bersama (satu listener sensor untuk semua komponen).
 * - Sensor: TYPE_GRAVITY, fallback TYPE_ACCELEROMETER (low-pass). Tanpa sensor: hanya riak idle.
 * - Sensor hanya aktif saat ada komponen yang tampil DAN aplikasi RESUMED.
 * - State fisika dihitung per frame di [step] (satu kali per frame untuk semua konsumen).
 */
internal object WaterEngine : SensorEventListener {
    // Input sensor mentah ternormalisasi (-1..1): +x = HP miring ke kanan, +y = miring ke depan.
    private var rawRoll = 0f
    private var rawPitch = 0f
    private var lpX = 0f
    private var lpY = 0f

    // Keadaan fisika
    var slosh = 0f; private set // posisi massa air horizontal (-1..1), lag + overshoot
    private var sloshV = 0f
    var pitch = 0f; private set
    private var pitchV = 0f
    var energy = 0f; private set // 0..1, memperbesar amplitudo saat bergerak
    var phase = 0f; private set
    var hasSensor = false; private set

    private var users = 0
    private var resumed = false
    private var registered = false
    private var sm: SensorManager? = null
    private var lastNanos = 0L
    private var lastStepFrame = -1L

    /** Tick di-clock dari frame; composable membaca ini agar draw ter-invalidate. */
    val tick = mutableLongStateOf(0L)

    fun acquire(context: Context) {
        if (sm == null) sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        users++
        update()
    }

    fun release() {
        users = (users - 1).coerceAtLeast(0)
        update()
    }

    fun setResumed(value: Boolean) {
        resumed = value
        update()
    }

    private fun update() {
        val want = users > 0 && resumed
        if (want && !registered) {
            val m = sm ?: return
            val s = m.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (s != null) {
                // SENSOR_DELAY_UI (~60 ms) cukup: interpolasi spring per-frame menghaluskan hasil.
                registered = m.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)
                hasSensor = registered
            }
            lastNanos = 0L
        } else if (!want && registered) {
            sm?.unregisterListener(this)
            registered = false
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        val g = SensorManager.GRAVITY_EARTH
        // Sumbu perangkat: +x kanan, +y atas layar. Gravitasi/akselerometer menunjuk "naik",
        // jadi miring kanan => x negatif, miring depan (atas turun) => y negatif.
        val roll = (-e.values[0] / g * 2.6f).coerceIn(-1f, 1f)
        val pit = (-e.values[1] / g * 2.0f).coerceIn(-1f, 1f)
        if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            lpX += 0.18f * (roll - lpX)
            lpY += 0.18f * (pit - lpY)
            rawRoll = lpX
            rawPitch = lpY
        } else {
            rawRoll = roll
            rawPitch = pit
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Integrasi fisika semi-implisit; aman dipanggil banyak kali per frame (hanya 1x yang jalan). */
    fun step(frameNanos: Long) {
        if (frameNanos == lastStepFrame) return
        lastStepFrame = frameNanos
        val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1e9f).coerceIn(0.001f, 0.05f)
        lastNanos = frameNanos

        // Spring-damper kurang teredam => air menyusul miring HP dengan sedikit overshoot (slosh).
        val k = 38f
        val c = 4.2f
        sloshV += (k * (rawRoll - slosh) - c * sloshV) * dt
        slosh += sloshV * dt
        pitchV += (k * 0.8f * (rawPitch - pitch) - c * 1.1f * pitchV) * dt
        pitch += pitchV * dt

        val target = (abs(sloshV) * 0.9f + abs(pitchV) * 0.7f + abs(rawRoll) * 0.25f).coerceIn(0f, 1f)
        // Naik cepat, turun perlahan => kembali tenang secara halus.
        val rate = if (target > energy) 10f else 1.6f
        energy += (target - energy) * (rate * dt).coerceAtMost(1f)

        // Fase gelombang: kecepatan dasar kecil + dorongan searah kemiringan.
        phase += (0.9f + slosh * 3.2f + sloshV * 0.6f) * dt
        tick.longValue = frameNanos
    }
}

private const val TWO_PI = (2.0 * PI).toFloat()
private const val POINTS = 36

/**
 * Permukaan air di dalam komponen kaca. Komponen tidak bergerak; hanya air yang bergerak.
 *
 * @param front true = digambar di atas konten (mis. Mini Player yang latarnya anak composable)
 *              dengan alpha rendah agar kontrol tetap terbaca. false = di belakang konten.
 */
@Composable
fun Modifier.liquidWater(
    shape: Shape,
    tint: Color = Color(0xFF5CC8FF),
    front: Boolean = false,
): Modifier {
    if (LocalGlassQuality.current == GlassQuality.LOW) return this
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tintState by rememberUpdatedState(tint)

    DisposableEffect(lifecycleOwner) {
        WaterEngine.acquire(context)
        WaterEngine.setResumed(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> WaterEngine.setResumed(true)
                Lifecycle.Event.ON_PAUSE -> WaterEngine.setResumed(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            WaterEngine.release()
        }
    }

    // Clock frame hanya berjalan saat RESUMED. Saat sudah tenang, turun ke ~30 fps.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var skip = false
            while (true) {
                withFrameNanos { t ->
                    val calm = WaterEngine.energy < 0.03f
                    if (calm && skip) {
                        skip = false
                    } else {
                        skip = calm
                        WaterEngine.step(t)
                    }
                }
            }
        }
    }

    val surface = remember { Path() }
    val fillPath = remember { Path() }
    val body = remember { Path() }

    return this.drawWithContent {
        if (!front) drawWater(shape, tintState, surface, fillPath, body, 1f)
        drawContent()
        if (front) drawWater(shape, tintState, surface, fillPath, body, 0.6f)
    }
}

private fun DrawScope.drawWater(
    shape: Shape,
    tint: Color,
    surface: Path,
    fillPath: Path,
    body: Path,
    strength: Float,
) {
    @Suppress("UNUSED_EXPRESSION")
    WaterEngine.tick.longValue // subscribe: draw ter-invalidate per frame tanpa recomposition
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return

    val slosh = WaterEngine.slosh
    val pitch = WaterEngine.pitch
    val energy = WaterEngine.energy
    val phase = WaterEngine.phase
    val full = strength >= 0.99f // detail (kaustik, gelembung) hanya saat tidak menimpa teks

    val level = h * (0.52f - pitch * 0.14f).coerceIn(0.30f, 0.72f)
    val slope = -slosh * h * 1.7f
    val amp = h * 0.035f + h * 0.085f * energy

    fun yAt(layer: Int, u: Float): Float {
        val lp = phase * (0.8f + 0.35f * layer) + layer * 1.7f
        val freq = 1.15f + 0.55f * layer
        val a = amp * (0.75f + 0.25f * layer)
        val offY = level + (2 - layer) * h * 0.035f * (1f + pitch.coerceIn(-1f, 1f) * 0.6f)
        return offY + slope * (u - 0.5f) +
            a * sin(TWO_PI * freq * u + lp) +
            a * 0.25f * sin(TWO_PI * freq * 1.9f * u - lp * 1.35f)
    }

    val outline = shape.createOutline(size, layoutDirection, this)
    val clip = Path().apply {
        when (outline) {
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
            is Outline.Rectangle -> addRect(outline.rect)
        }
    }

    val shallow = Color(0xFF9BE7FF)
    val mid = Color(0xFF2AA3DC)
    val deep = Color(0xFF0A4A86)

    clipPath(clip) {
        for (layer in 0 until 3) {
            val depth = layer / 2f
            val topY = yAt(layer, 0.5f)
            fillPath.reset()
            for (i in 0..POINTS) {
                val u = i / POINTS.toFloat()
                val y = yAt(layer, u)
                if (i == 0) fillPath.moveTo(0f, y) else fillPath.lineTo(u * w, y)
            }
            body.reset()
            body.addPath(fillPath)
            body.lineTo(w, h)
            body.lineTo(0f, h)
            body.close()

            // Badan air tembus pandang: terang di permukaan, makin biru pekat ke dasar.
            val k = strength * (0.55f + 0.45f * depth)
            drawPath(
                body,
                Brush.verticalGradient(
                    0f to shallow.copy(alpha = 0.16f * k),
                    0.35f to mid.copy(alpha = 0.22f * k),
                    1f to deep.copy(alpha = 0.40f * k),
                    startY = topY - h * 0.1f,
                    endY = h,
                ),
            )
            // Meniscus: pita cahaya lembut tepat di bawah permukaan.
            drawPath(fillPath, shallow.copy(alpha = 0.20f * k), style = Stroke(width = 6.dp.toPx()))
            drawPath(
                fillPath,
                Color.White.copy(alpha = (0.30f + 0.35f * depth) * strength),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        if (full) {
            // Kaustik: pita cahaya tipis yang bergerak lambat di dalam air.
            for (c in 0 until 3) {
                surface.reset()
                val base = level + h * (0.16f + 0.11f * c)
                for (i in 0..POINTS) {
                    val u = i / POINTS.toFloat()
                    val y = base +
                        h * 0.05f * sin(TWO_PI * (1.4f + 0.5f * c) * u + phase * (0.9f + 0.3f * c)) +
                        h * 0.025f * sin(TWO_PI * (3.1f + c) * u - phase * 1.4f)
                    if (i == 0) surface.moveTo(0f, y) else surface.lineTo(u * w, y)
                }
                drawPath(surface, Color.White.copy(alpha = 0.10f), style = Stroke(width = 1.4.dp.toPx()))
            }

            // Gelembung kecil naik ke permukaan.
            for (i in 0 until 7) {
                val u = (i * 0.619f + 0.1f) % 1f
                val t = (phase * 0.05f * (0.6f + 0.12f * i) + i * 0.137f * 7f) % 1f
                val x = u * w + sin(phase * 1.3f + i) * w * 0.008f
                val ys = yAt(2, u)
                val y = h - t * (h - ys)
                val r = (1.2f + (i % 3) * 0.7f).dp.toPx()
                val fade = (1f - t).coerceIn(0f, 1f) * 0.6f
                drawCircle(Color.White.copy(alpha = 0.45f * fade), r, Offset(x, y), style = Stroke(width = 0.8.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.7f * fade), r * 0.3f, Offset(x - r * 0.3f, y - r * 0.3f))
            }

            // Kilau spekular di puncak gelombang (berkedip karena sudut cahaya berubah).
            for (i in 0 until 3) {
                val u = (0.2f + 0.33f * i + phase * 0.03f * (1f + 0.3f * i) + slosh * 0.1f).mod(1f)
                val y = yAt(2, u)
                val tw = 0.5f + 0.5f * sin(phase * 2.2f + i * 2f)
                val half = w * (0.012f + 0.01f * tw)
                drawLine(
                    Color.White.copy(alpha = 0.35f + 0.4f * tw),
                    Offset(u * w - half, y),
                    Offset(u * w + half, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }

        // Pantulan cahaya yang bergeser mengikuti kemiringan.
        val cx = w * (0.5f + slosh * 0.35f) + sin(phase * 0.7f) * w * 0.04f
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = (0.10f + 0.08f * energy) * strength), Color.Transparent),
                center = Offset(cx, level),
                radius = w * 0.3f,
            ),
        )
    }
}
