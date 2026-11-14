package com.sxanalys.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxanalys.music.LocalDatabase
import com.sxanalys.music.R
import com.sxanalys.music.constants.DailyListeningGoalKey
import com.sxanalys.music.utils.computeListeningStreak
import com.sxanalys.music.utils.nextStreakMilestone
import com.sxanalys.music.utils.rememberPreference
import java.time.LocalDate
import java.time.LocalTime

private val GOAL_PRESETS = listOf(0, 15, 30, 60, 90, 120)

@Composable
fun ListeningStreakCard(modifier: Modifier = Modifier) {
    val database = LocalDatabase.current
    val today = LocalDate.now()
    val dayStart = remember(today) { today.atStartOfDay() }
    val dayEnd = remember(today) { today.atTime(LocalTime.MAX) }
    val weekStart = remember(today) { today.minusDays(6).atStartOfDay() }

    val rawDays by database.listeningDays().collectAsStateWithLifecycle(initialValue = emptyList())
    val todayMs by remember(today) { database.getTotalPlayTimeInRange(dayStart, dayEnd) }
        .collectAsStateWithLifecycle(initialValue = 0L)
    val weekMs by remember(today) { database.getTotalPlayTimeInRange(weekStart, dayEnd) }
        .collectAsStateWithLifecycle(initialValue = 0L)
    val weekSongs by remember(today) { database.getUniqueSongCountInRange(weekStart, dayEnd) }
        .collectAsStateWithLifecycle(initialValue = 0)
    val (goalMinutes, onGoalChange) = rememberPreference(DailyListeningGoalKey, defaultValue = 30)

    val streak =
        remember(rawDays, today) {
            computeListeningStreak(rawDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }, today)
        }
    if (rawDays.isEmpty()) return

    val todayMinutes = ((todayMs ?: 0L) / 60_000L).toInt()
    val weekMinutes = ((weekMs ?: 0L) / 60_000L).toInt()
    val milestone = nextStreakMilestone(streak.current)
    val mono = FontFamily.Monospace
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .glassPanel(RoundedCornerShape(22.dp))
                .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.streak_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = mono,
            letterSpacing = 1.6.sp,
            color = muted,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = streak.current.toString(),
                fontFamily = mono,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = pluralStringResource(R.plurals.streak_days, streak.current),
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text =
                if (milestone != null) {
                    stringResource(R.string.streak_next_milestone, milestone - streak.current, milestone)
                } else {
                    stringResource(R.string.streak_max_milestone)
                },
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.streak_last_28).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = mono,
            letterSpacing = 1.2.sp,
            color = muted,
        )
        Spacer(Modifier.height(8.dp))
        streak.last28Days.chunked(7).forEachIndexed { rowIndex, week ->
            if (rowIndex > 0) Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEachIndexed { colIndex, listened ->
                    val isToday = rowIndex == 3 && colIndex == 6
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    when {
                                        listened -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.outline
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    RoundedCornerShape(3.dp),
                                ),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCell(stringResource(R.string.streak_today), stringResource(R.string.streak_minutes, todayMinutes), Modifier.weight(1f))
            StatCell(stringResource(R.string.streak_week), stringResource(R.string.streak_minutes, weekMinutes), Modifier.weight(1f))
            StatCell(stringResource(R.string.streak_songs), weekSongs.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text =
                    if (goalMinutes > 0) {
                        stringResource(R.string.goal_label, goalMinutes)
                    } else {
                        stringResource(R.string.goal_off)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    val next = GOAL_PRESETS[(GOAL_PRESETS.indexOf(goalMinutes) + 1) % GOAL_PRESETS.size]
                    onGoalChange(next)
                },
            ) {
                Text(stringResource(R.string.goal_change))
            }
        }
        if (goalMinutes > 0) {
            val progress = (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text =
                    if (todayMinutes >= goalMinutes) {
                        stringResource(R.string.goal_reached)
                    } else {
                        stringResource(R.string.goal_remaining, goalMinutes - todayMinutes)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
