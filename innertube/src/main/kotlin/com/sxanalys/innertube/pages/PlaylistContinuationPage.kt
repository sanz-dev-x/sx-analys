package com.sxanalys.innertube.pages

import com.sxanalys.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
