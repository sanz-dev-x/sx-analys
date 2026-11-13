package com.sxanalys.music.dynamicisland

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.media3.common.Player
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sxanalys.music.MainActivity
import com.sxanalys.music.constants.DynamicIslandCurvatureKey
import com.sxanalys.music.constants.DynamicIslandEnabledKey
import com.sxanalys.music.constants.DynamicIslandOnTrackChangeKey
import com.sxanalys.music.constants.DynamicIslandStyle
import com.sxanalys.music.constants.DynamicIslandStyleKey
import com.sxanalys.music.constants.DynamicIslandTopOffsetKey
import com.sxanalys.music.constants.DynamicIslandWidthKey
import com.sxanalys.music.constants.DynamicIslandHeightKey
import com.sxanalys.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Draws the island as a system overlay while the app is in the background and music is playing.
 * All entry points must be called on the main thread.
 */
class DynamicIslandController(
    private val context: Context,
    private val playerProvider: () -> Player?,
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var enabled = false
    private var style by mutableStateOf(DynamicIslandStyle.IPHONE)
    private var curvature by mutableStateOf(100)
    private var islandWidth by mutableStateOf(IslandDefaultWidthDp)
    private var islandHeight by mutableStateOf(IslandDefaultHeightDp)
    private var topOffset = 6
    private var autoExpandOnTrackChange = true

    private var title by mutableStateOf("")
    private var artist by mutableStateOf("")
    private var artworkUrl by mutableStateOf<String?>(null)
    private var isPlaying by mutableStateOf(false)
    private var progress by mutableFloatStateOf(0f)
    private var positionMs by mutableStateOf(0L)
    private var durationMs by mutableStateOf(0L)
    private var expanded by mutableStateOf(false)

    private var host: ComposeView? = null
    private var owner: OverlayOwner? = null

    private val collapseRunnable = Runnable { expanded = false }
    private val hideRunnable = Runnable { if (!isPlaying) detach() }
    private val progressRunnable =
        object : Runnable {
            override fun run() {
                updateProgress()
                if (host != null && expanded) handler.postDelayed(this, 500L)
            }
        }

    private val processObserver =
        androidx.lifecycle.LifecycleEventObserver { _, _ -> refresh() }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        scope.launch {
            appContext.dataStore.data.collect { prefs ->
                enabled = prefs[DynamicIslandEnabledKey] ?: false
                style =
                    runCatching { DynamicIslandStyle.valueOf(prefs[DynamicIslandStyleKey] ?: "") }
                        .getOrDefault(DynamicIslandStyle.IPHONE)
                curvature = (prefs[DynamicIslandCurvatureKey] ?: 100).coerceIn(0, 100)
                islandWidth = (prefs[DynamicIslandWidthKey] ?: IslandDefaultWidthDp).coerceIn(100, 260)
                islandHeight = (prefs[DynamicIslandHeightKey] ?: IslandDefaultHeightDp).coerceIn(28, 64)
                autoExpandOnTrackChange = prefs[DynamicIslandOnTrackChangeKey] ?: true
                val newOffset = (prefs[DynamicIslandTopOffsetKey] ?: 6).coerceIn(0, 60)
                if (newOffset != topOffset) {
                    topOffset = newOffset
                    applyOffset()
                }
                refresh()
            }
        }
    }

    fun onPlaybackStateChanged() = refresh()

    fun onTrackChanged() {
        handler.postDelayed({
            refresh()
            if (host != null && autoExpandOnTrackChange) expandTemporarily()
        }, METADATA_SETTLE_MS)
    }

    fun release() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        handler.removeCallbacksAndMessages(null)
        detach()
        scope.cancel()
    }

    private fun inBackground() =
        !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun canDraw() = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(appContext)

    private fun refresh() {
        val player = playerProvider()
        val hasMedia = player?.currentMediaItem != null
        isPlaying = player?.isPlaying == true
        val shouldShow = enabled && canDraw() && inBackground() && hasMedia

        if (!shouldShow) {
            detach()
            return
        }
        readMetadata(player!!)
        attach()
        handler.removeCallbacks(hideRunnable)
        if (!isPlaying) handler.postDelayed(hideRunnable, PAUSED_LINGER_MS)
    }

    private fun readMetadata(player: Player) {
        val meta = player.mediaMetadata
        title = meta.title?.toString().orEmpty()
        artist = (meta.artist ?: meta.albumArtist)?.toString().orEmpty()
        artworkUrl = meta.artworkUri?.toString()
        updateProgress()
    }

    private fun updateProgress() {
        val player = playerProvider() ?: return
        val duration = player.duration
        positionMs = player.currentPosition.coerceAtLeast(0L)
        durationMs = if (duration > 0) duration else 0L
        progress = if (duration > 0) (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    private fun expandTemporarily() {
        expanded = true
        handler.removeCallbacks(collapseRunnable)
        handler.postDelayed(collapseRunnable, EXPANDED_LINGER_MS)
        handler.removeCallbacks(progressRunnable)
        handler.post(progressRunnable)
    }

    private fun toggleExpand() {
        if (expanded) {
            handler.removeCallbacks(collapseRunnable)
            expanded = false
        } else {
            expandTemporarily()
        }
    }

    private fun openApp() {
        expanded = false
        val intent =
            Intent(appContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        runCatching { appContext.startActivity(intent) }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val density = appContext.resources.displayMetrics.density
        return WindowManager
            .LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (topOffset * density).toInt()
                title = "SXDynamicIsland"
            }
    }

    private fun applyOffset() {
        val view = host ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        params.y = (topOffset * appContext.resources.displayMetrics.density).toInt()
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun attach() {
        if (host != null) return
        val lifecycleOwner = OverlayOwner().also { owner = it }
        val view =
            ComposeView(appContext).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setContent {
                    DynamicIslandContent(
                        track = IslandTrack(title, artist, artworkUrl, isPlaying, progress, positionMs, durationMs),
                        expanded = expanded,
                        style = style,
                        curvaturePercent = curvature,
                        compactWidthDp = islandWidth,
                        compactHeightDp = islandHeight,
                        callbacks =
                            IslandCallbacks(
                                onToggleExpand = ::toggleExpand,
                                onOpenApp = ::openApp,
                                onPlayPause = {
                                    playerProvider()?.let { if (it.isPlaying) it.pause() else it.play() }
                                    handler.postDelayed({ refresh() }, 120L)
                                    expandTemporarily()
                                },
                                onPrevious = {
                                    playerProvider()?.seekToPrevious()
                                    expandTemporarily()
                                },
                                onNext = {
                                    playerProvider()?.seekToNext()
                                    expandTemporarily()
                                },
                            ),
                    )
                }
            }
        val params = layoutParams()
        try {
            windowManager.addView(view, params)
            lifecycleOwner.resume()
            host = view
        } catch (_: Exception) {
            lifecycleOwner.destroy()
            owner = null
        }
    }

    private fun detach() {
        handler.removeCallbacks(collapseRunnable)
        handler.removeCallbacks(progressRunnable)
        expanded = false
        val view = host ?: return
        host = null
        runCatching { windowManager.removeViewImmediate(view) }
        owner?.destroy()
        owner = null
    }

    private class OverlayOwner :
        LifecycleOwner,
        SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)

        init {
            savedState.performAttach()
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.CREATED
        }

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

        fun resume() {
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    private companion object {
        const val EXPANDED_LINGER_MS = 4_000L
        const val PAUSED_LINGER_MS = 5_000L
        const val METADATA_SETTLE_MS = 150L
    }
}
