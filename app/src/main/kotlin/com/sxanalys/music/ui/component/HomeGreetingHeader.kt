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

    val scheme = MaterialTheme.colorScheme
    val lit = streak.listenedToday
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-0.6).sp),
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showStreak && streak.current > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.glassPanel(CircleShape).padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.streak_flame),
                    contentDescription = null,
                    tint = if (lit) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = streak.current.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
            }
        }
    }
}
