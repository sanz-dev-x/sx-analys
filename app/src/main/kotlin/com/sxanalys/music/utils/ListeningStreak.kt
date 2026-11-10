package com.sxanalys.music.utils

import java.time.LocalDate

data class ListeningStreak(
    val current: Int,
    val best: Int,
    val lastSevenDays: List<Boolean>,
    val last28Days: List<Boolean>,
    val listenedToday: Boolean,
)

fun computeListeningStreak(
    days: Collection<LocalDate>,
    today: LocalDate,
): ListeningStreak {
    val set = days.toHashSet()
    val listenedToday = today in set

    var cursor = if (listenedToday) today else today.minusDays(1)
    var current = 0
    while (cursor in set) {
        current++
        cursor = cursor.minusDays(1)
    }

    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (day in set.sorted()) {
        run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
        if (run > best) best = run
        previous = day
    }

    val lastSeven = (6 downTo 0).map { today.minusDays(it.toLong()) in set }
    val last28 = (27 downTo 0).map { today.minusDays(it.toLong()) in set }
    return ListeningStreak(current, best, lastSeven, last28, listenedToday)
}

private val MILESTONES = listOf(3, 7, 14, 30, 60, 100, 180, 365)

fun nextStreakMilestone(current: Int): Int? = MILESTONES.firstOrNull { it > current }
