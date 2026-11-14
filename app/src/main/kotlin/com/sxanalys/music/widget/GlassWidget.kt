package com.sxanalys.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import com.sxanalys.music.MainActivity
import com.sxanalys.music.R
import com.sxanalys.music.utils.dataStore
import kotlinx.coroutines.flow.first
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Liquid glass player widget. The last played song (title, artist, cover, position) is stored on disk,
 * so the widget keeps showing the cover even after playback ends or the phone restarts.
 */
object GlassWidget {
    private const val PREFS = "glass_widget"
    private const val ART_FILE = "glass_widget_art.png"
    private const val FULL_MIN_HEIGHT_DP = 100

    @Volatile private var memArt: Bitmap? = null
    @Volatile private var memArtUri: String? = null

    private class State(
        val title: String,
        val artist: String,
        val playing: Boolean,
        val liked: Boolean,
        val duration: Long,
        val position: Long,
    )

    fun update(
        context: Context,
        title: String,
        artist: String,
        artworkUri: String?,
        art: Bitmap?,
        isPlaying: Boolean,
        isLiked: Boolean,
        duration: Long,
        position: Long,
    ) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (artworkUri == null && art == null) {
            // Nothing is loaded: keep the last played song on the widget, just show it as paused.
            p.edit().putBoolean("playing", false).apply()
        } else {
            if (art != null && p.getString("art_uri", null) != artworkUri) {
                saveArt(context, art)
                memArt = art
                memArtUri = artworkUri
                p.edit().putString("art_uri", artworkUri).apply()
            }
            p.edit()
                .putString("title", title)
                .putString("artist", artist)
                .putBoolean("playing", isPlaying)
                .putBoolean("liked", isLiked)
                .putLong("duration", duration)
                .putLong("position", position)
                .apply()
        }
        render(context)
    }

    /** Draw from stored data only (used after a restart, when the player service is not running). */
    fun render(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, BubbleWidgetReceiver::class.java))
        if (ids.isEmpty()) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state =
            State(
                title = p.getString("title", null) ?: context.getString(R.string.no_song_playing),
                artist = p.getString("artist", null) ?: context.getString(R.string.tap_to_open),
                playing = p.getBoolean("playing", false),
                liked = p.getBoolean("liked", false),
                duration = p.getLong("duration", 0),
                position = p.getLong("position", 0),
            )
        val art = loadArt(context)
        ids.forEach { id ->
            try {
                mgr.updateAppWidget(id, build(context, state, art, mgr.getAppWidgetOptions(id)))
            } catch (e: Exception) {
                // Bitmap too large for the launcher or widget gone: skip this one
            }
        }
    }

    fun updateProgress(context: Context, duration: Long, position: Long) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids =
            mgr.getAppWidgetIds(ComponentName(context, BubbleWidgetReceiver::class.java))
                .filter { mgr.getAppWidgetOptions(it).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) >= FULL_MIN_HEIGHT_DP }
                .toIntArray()
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_glass_full)
        val max = if (duration > 0) 1000 else 0
        val progress = if (duration > 0) ((position.toDouble() / duration) * 1000).toInt().coerceIn(0, 1000) else 0
        views.setProgressBar(R.id.gf_progress, max, progress, false)
        views.setTextViewText(R.id.gf_cur, formatTime(position))
        mgr.partiallyUpdateAppWidget(ids, views)
    }

    private fun build(context: Context, s: State, art: Bitmap?, options: android.os.Bundle): RemoteViews {
        val density = context.resources.displayMetrics.density
        val wDp = max(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), 110)
        val hDp = max(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT), 40)
        val full = hDp >= FULL_MIN_HEIGHT_DP

        var scale = density
        val longest = max(wDp, hDp) * scale
        if (longest > 1200f) scale *= 1200f / longest
        val wPx = (wDp * scale).toInt()
        val hPx = (hDp * scale).toInt()
        val radius = if (full) min(hPx * 0.3f, wPx / 2f) else hPx / 2f

        val views = RemoteViews(context.packageName, if (full) R.layout.widget_glass_full else R.layout.widget_glass_pill)
        val pre = if (full) "gf" else "gp"
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            views.setViewOutlinePreferredRadius(
                if (full) R.id.gf_root else R.id.gp_root,
                radius,
                android.util.TypedValue.COMPLEX_UNIT_PX,
            )
        }
        fun id(name: String) = context.resources.getIdentifier("${pre}_$name", "id", context.packageName)

        views.setImageViewBitmap(id("bg"), glassBackground(art, wPx, hPx, radius, scale, frostLevel(context)))
        views.setImageViewBitmap(id("art"), roundedSquare(art, ((if (full) 96f else 40f) * density).toInt(), (if (full) 8f else 20f) * density))
        views.setTextViewText(id("title"), s.title)
        views.setTextViewText(id("artist"), s.artist)
        views.setImageViewResource(id("play"), if (s.playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setInt(id("play"), "setColorFilter", if (full) 0xFF101114.toInt() else Color.WHITE)
        views.setInt(id("prev"), "setColorFilter", Color.WHITE)
        views.setInt(id("next"), "setColorFilter", Color.WHITE)

        views.setOnClickPendingIntent(id("play"), action(context, MusicWidgetReceiver.ACTION_PLAY_PAUSE, 11))
        views.setOnClickPendingIntent(id("prev"), action(context, MusicWidgetReceiver.ACTION_PREVIOUS, 12))
        views.setOnClickPendingIntent(id("next"), action(context, MusicWidgetReceiver.ACTION_NEXT, 13))
        views.setOnClickPendingIntent(id("art"), openApp(context))
        views.setOnClickPendingIntent(id("title"), openApp(context))

        if (full) {
            views.setImageViewResource(R.id.gf_like, if (s.liked) R.drawable.ic_widget_heart_nav else R.drawable.ic_widget_heart_outline_nav)
            views.setInt(R.id.gf_like, "setColorFilter", if (s.liked) 0xFFFF5C7A.toInt() else Color.WHITE)
            views.setOnClickPendingIntent(R.id.gf_like, action(context, MusicWidgetReceiver.ACTION_LIKE, 14))
            val max = if (s.duration > 0) 1000 else 0
            val progress = if (s.duration > 0) ((s.position.toDouble() / s.duration) * 1000).toInt().coerceIn(0, 1000) else 0
            views.setProgressBar(R.id.gf_progress, max, progress, false)
            views.setTextViewText(R.id.gf_cur, formatTime(s.position))
            views.setTextViewText(R.id.gf_total, formatTime(s.duration))
        }
        return views
    }

    // ---- drawing -------------------------------------------------------------------------------

    private fun frostLevel(context: Context): Float =
        runCatching {
            kotlinx.coroutines.runBlocking {
                context.dataStore.data.first()[com.sxanalys.music.constants.GlassFrostBlurKey] ?: 0.2f
            }
        }.getOrDefault(0.2f)

    private fun glassBackground(art: Bitmap?, w: Int, h: Int, radius: Float, scale: Float, frost: Float): Bitmap {
        val depth = min(min(h * 0.30f, w * 0.22f), 34f * scale).coerceAtLeast(6f)
        val m = (depth * 1.8f).toInt() + 2
        val fw = w + 2 * m
        val fh = h + 2 * m
        val field = IntArray(fw * fh)
        if (art != null) {
            sharpCrop(art, fw, fh).getPixels(field, 0, fw, 0, 0, fw, fh)
            val blurR = (frost * 6f).toInt()
            if (blurR > 0) boxBlur(field, fw, fh, blurR)
        } else {
            java.util.Arrays.fill(field, 0xFF2A3446.toInt())
        }
        val out = IntArray(w * h)
        val cx = w / 2f
        val cy = h / 2f
        val hx = cx - radius
        val hy = cy - radius
        for (y in 0 until h) {
            val py = y + 0.5f - cy
            val qy = abs(py) - hy
            val sy = if (py < 0f) -1f else 1f
            for (x in 0 until w) {
                val px = x + 0.5f - cx
                val qx = abs(px) - hx
                val sx = if (px < 0f) -1f else 1f
                val nx: Float
                val ny: Float
                val sd: Float
                if (qx > 0f && qy > 0f) {
                    val len = sqrt(qx * qx + qy * qy)
                    nx = sx * qx / len
                    ny = sy * qy / len
                    sd = len - radius
                } else if (qx > qy) {
                    nx = sx
                    ny = 0f
                    sd = qx - radius
                } else {
                    nx = 0f
                    ny = sy
                    sd = qy - radius
                }
                val d = -sd
                if (d <= -1f) continue
                val cov = (d + 0.5f).coerceIn(0f, 1f)
                val t = if (d >= depth) 0f else (1f - d.coerceAtLeast(0f) / depth)
                val bend = depth * 1.5f * t.pow(1.45f)
                val bx = x + 0.5f + m
                val by = y + 0.5f + m
                var r: Float
                var g: Float
                var b: Float
                if (t <= 0f) {
                    val c = field[(y + m) * fw + (x + m)]
                    r = ((c shr 16) and 255).toFloat()
                    g = ((c shr 8) and 255).toFloat()
                    b = (c and 255).toFloat()
                } else {
                    r = fieldChannel(field, fw, fh, bx + nx * bend * 1.14f, by + ny * bend * 1.14f, 16)
                    g = fieldChannel(field, fw, fh, bx + nx * bend, by + ny * bend, 8)
                    b = fieldChannel(field, fw, fh, bx + nx * bend * 0.86f, by + ny * bend * 0.86f, 0)
                }
                val bright = 0.78f + 0.12f * (1f - t)
                r *= bright
                g *= bright
                b *= bright
                val alpha = cov
                out[y * w + x] =
                    ((alpha * 255f).toInt().coerceIn(0, 255) shl 24) or
                        (r.toInt().coerceIn(0, 255) shl 16) or
                        (g.toInt().coerceIn(0, 255) shl 8) or
                        b.toInt().coerceIn(0, 255)
            }
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(out, 0, w, 0, 0, w, h)

        return bmp
    }

    private fun fieldChannel(field: IntArray, fw: Int, fh: Int, x: Float, y: Float, shift: Int): Float {
        val fx = (x - 0.5f).coerceIn(0f, fw - 1.001f)
        val fy = (y - 0.5f).coerceIn(0f, fh - 1.001f)
        val x0 = fx.toInt()
        val y0 = fy.toInt()
        val ax = fx - x0
        val ay = fy - y0
        val i = y0 * fw + x0
        val c00 = (field[i] shr shift) and 255
        val c10 = (field[i + 1] shr shift) and 255
        val c01 = (field[i + fw] shr shift) and 255
        val c11 = (field[i + fw + 1] shr shift) and 255
        return (c00 * (1f - ax) + c10 * ax) * (1f - ay) + (c01 * (1f - ax) + c11 * ax) * ay
    }

    /** Full-resolution blur (3 box passes ~ gaussian): smooth, no blocky downscale/upscale. */
    private fun boxBlur(px: IntArray, w: Int, h: Int, radius: Int) {
        val tmp = IntArray(px.size)
        repeat(3) {
            blurPass(px, tmp, w, h, radius, true)
            blurPass(tmp, px, w, h, radius, false)
        }
    }

    private fun blurPass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val div = 2 * r + 1
        for (l in 0 until lines) {
            fun idx(i: Int) = if (horizontal) l * w + i else i * w + l
            var sr = 0
            var sg = 0
            var sb = 0
            for (k in -r..r) {
                val c = src[idx(k.coerceIn(0, len - 1))]
                sr += (c shr 16) and 255
                sg += (c shr 8) and 255
                sb += c and 255
            }
            for (i in 0 until len) {
                dst[idx(i)] = (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = src[idx((i + r + 1).coerceIn(0, len - 1))]
                val rem = src[idx((i - r).coerceIn(0, len - 1))]
                sr += ((add shr 16) and 255) - ((rem shr 16) and 255)
                sg += ((add shr 8) and 255) - ((rem shr 8) and 255)
                sb += (add and 255) - (rem and 255)
            }
        }
    }

    private fun sharpCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        // Center-crop to target aspect and scale with filtering: full-bleed, sharp, no blur
        val targetRatio = w.toFloat() / h
        val srcRatio = src.width.toFloat() / src.height
        val cw: Int
        val ch: Int
        if (srcRatio > targetRatio) {
            ch = src.height
            cw = (ch * targetRatio).toInt()
        } else {
            cw = src.width
            ch = (cw / targetRatio).toInt()
        }
        val cropped = Bitmap.createBitmap(src, (src.width - cw) / 2, (src.height - ch) / 2, max(cw, 1), max(ch, 1))
        return Bitmap.createScaledBitmap(cropped, w, h, true)
    }

    private fun roundedSquare(src: Bitmap?, size: Int, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        if (src == null) {
            c.drawColor(0x33FFFFFF)
        } else {
            val side = min(src.width, src.height)
            val crop = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
            c.drawBitmap(Bitmap.createScaledBitmap(crop, size, size, true), 0f, 0f, null)
        }
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        c.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, mask)
        return out
    }

    // ---- storage / intents ---------------------------------------------------------------------

    private fun saveArt(context: Context, art: Bitmap) {
        try {
            File(context.filesDir, ART_FILE).outputStream().use { art.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
        }
    }

    private fun loadArt(context: Context): Bitmap? {
        memArt?.let { return it }
        return try {
            val f = File(context.filesDir, ART_FILE)
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.also { memArt = it } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun action(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            code,
            Intent(context, MusicWidgetReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun formatTime(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(total / 60, total % 60)
    }
}
