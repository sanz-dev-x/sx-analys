/**
 * Sx anlys Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import com.sxanalys.music.constants.AppLanguageKey
import com.sxanalys.music.R

fun getArtistSeparator(context: Context): String = " ${context.getString(R.string.and)} "

fun <T> List<T>.joinToArtistString(
    conjunction: String,
    transform: (T) -> String,
): String = when (size) {
    0 -> ""
    1 -> transform(this[0])
    2 -> "${transform(this[0])}$conjunction${transform(this[1])}"
    else -> dropLast(1).joinToString(", ") { transform(it) } + "$conjunction${transform(last())}"
}

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

/**
 * Menyimpan bahasa aplikasi lalu menerapkannya. Android 13+ memakai LocaleManager lewat
 * AppCompatDelegate; versi lama membaca AppLanguageKey di MainActivity.onCreate, jadi activity dibuat ulang.
 */
suspend fun Activity.applyAppLanguage(languageTag: String) {
    applicationContext.dataStore.edit { it[AppLanguageKey] = languageTag }
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) recreate()
}
