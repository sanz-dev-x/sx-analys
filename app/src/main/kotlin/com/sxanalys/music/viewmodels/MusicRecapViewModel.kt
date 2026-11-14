package com.sxanalys.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxanalys.music.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import javax.inject.Inject

data class RecapEntry(
    val rank: Int,
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val plays: Int,
)

data class RecapYear(
    val year: Int,
    val entries: List<RecapEntry>,
)

@HiltViewModel
class MusicRecapViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
    ) : ViewModel() {
        val years = MutableStateFlow<List<RecapYear>?>(null)

        private val prefs get() = context.getSharedPreferences("music_recap_history", Context.MODE_PRIVATE)

        init {
            viewModelScope.launch(Dispatchers.IO) { load() }
        }

        private suspend fun load() {
            val now = LocalDateTime.now()
            val firstYear = database.firstEvent().first()?.event?.timestamp?.year ?: now.year
            val result = mutableListOf<RecapYear>()
            for (y in now.year downTo minOf(firstYear, now.year)) {
                val stored = readSnapshot(y)
                val frozen = y < now.year && stored != null
                val entries =
                    if (frozen) {
                        stored!!
                    } else {
                        val from = LocalDateTime.of(y, 1, 1, 0, 0, 0).minusNanos(1)
                        val to = LocalDateTime.of(y, 12, 31, 23, 59, 59)
                        database.mostPlayedSongsByCount(from, to, 10).first().mapIndexed { i, s ->
                            RecapEntry(i + 1, s.id, s.title, s.artistName.orEmpty(), s.thumbnailUrl, s.songCountListened)
                        }.also { if (it.isNotEmpty()) writeSnapshot(y, it) }
                    }
                // keep history even if the play events were cleared later
                val shown = if (entries.isEmpty()) stored.orEmpty() else entries
                if (shown.isNotEmpty()) result.add(RecapYear(y, shown))
            }
            years.value = result
        }

        private fun writeSnapshot(year: Int, entries: List<RecapEntry>) {
            val arr = JSONArray()
            entries.forEach {
                arr.put(
                    JSONObject()
                        .put("id", it.songId)
                        .put("title", it.title)
                        .put("artist", it.artist)
                        .put("thumb", it.thumbnailUrl)
                        .put("plays", it.plays),
                )
            }
            prefs.edit().putString("year_$year", arr.toString()).apply()
        }

        private fun readSnapshot(year: Int): List<RecapEntry>? {
            val raw = prefs.getString("year_$year", null) ?: return null
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    RecapEntry(i + 1, o.getString("id"), o.getString("title"), o.optString("artist"), o.optString("thumb"), o.getInt("plays"))
                }
            }.getOrNull()
        }
    }
