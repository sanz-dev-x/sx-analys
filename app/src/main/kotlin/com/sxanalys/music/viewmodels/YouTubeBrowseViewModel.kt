/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxanalys.innertube.YouTube
import com.sxanalys.innertube.models.filterYoutubeShorts
import com.sxanalys.innertube.pages.BrowseResult
import com.sxanalys.music.constants.HideExplicitKey
import com.sxanalys.music.constants.HideVideoSongsKey
import com.sxanalys.music.constants.HideYoutubeShortsKey
import com.sxanalys.music.utils.dataStore
import com.sxanalys.music.utils.get
import com.sxanalys.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    result.value = it
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}
