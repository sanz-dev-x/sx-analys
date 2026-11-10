package com.sxanalys.innertube.pages

import com.sxanalys.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
