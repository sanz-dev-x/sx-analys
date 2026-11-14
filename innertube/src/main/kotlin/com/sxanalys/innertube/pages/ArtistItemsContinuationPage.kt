package com.sxanalys.innertube.pages

import com.sxanalys.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
