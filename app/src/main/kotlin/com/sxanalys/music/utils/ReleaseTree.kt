package com.sxanalys.music.utils

import android.util.Log
import timber.log.Timber

/** Release logging tree: drops debug/info noise so hot paths do not write to logcat. */
class ReleaseTree : Timber.Tree() {
    override fun isLoggable(
        tag: String?,
        priority: Int,
    ): Boolean = priority >= Log.WARN

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val text = if (t != null) message + "\n" + Log.getStackTraceString(t) else message
        Log.println(priority, tag ?: "SXANLYS", text)
    }
}
