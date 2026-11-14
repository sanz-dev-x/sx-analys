/**
 * Sx anlys Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.sxanalys.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.ios_house_outlined,
        iconIdActive = R.drawable.ios_house_filled,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.ios_search,
        iconIdActive = R.drawable.ios_search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.ios_library_outlined,
        iconIdActive = R.drawable.ios_library_filled,
        route = "library"
    )

    object Settings : Screens(
        titleId = R.string.you,
        iconIdInactive = R.drawable.ios_person_outlined,
        iconIdActive = R.drawable.ios_person_filled,
        route = "settings"
    )

    companion object {
        val MainScreens = listOf(Home, Search, Library, Settings)
    }
}
