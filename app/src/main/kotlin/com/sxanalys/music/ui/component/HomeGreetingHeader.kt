package com.sxanalys.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxanalys.music.LocalDatabase
import com.sxanalys.music.R
import com.sxanalys.music.constants.ShowStreakOnHomeKey
import com.sxanalys.music.utils.computeListeningStreak
import com.sxanalys.music.utils.rememberPreference
import java.time.LocalDate

@Composable
fun HomeGreetingHeader(
    greeting: String,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val today = remember { LocalDate.now() }
    val (showStreak) = rememberPreference(ShowStreakOnHomeKey, defaultValue = true)
    val rawDays by remember(database) { database.listeningDays() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val streak =
        remember(rawDays, today) {
            computeListeningStreak(rawDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }, today)
        }

    if (!showStreak) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.6).sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
        )
        return
    }

    val scheme = MaterialTheme.colorScheme
    val lit = streak.listenedToday
    val headline =
        when {
            streak.current > 0 && lit ->
                "${streak.current} " + pluralStringResource(R.plurals.streak_days, streak.current)
            streak.current > 0 -> stringResource(R.string.home_streak_keep, streak.current)
            else -> stringResource(R.string.home_streak_start)
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp).glassPanel(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.streak_flame),
                contentDescription = null,
                tint = if (lit) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(26.dp),
            )
        }
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
