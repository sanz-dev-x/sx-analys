package com.sxanalys.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxanalys.music.R

@Composable
fun StreakChip(
    days: Int,
    lit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (lit) scheme.primaryContainer else scheme.surfaceContainerHighest,
        label = "streakContainer",
    )
    val content by animateColorAsState(
        if (lit) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.55f),
        label = "streakContent",
    )
    val description = stringResource(if (lit) R.string.streak_chip_lit else R.string.streak_chip_unlit, days)

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = if (lit) null else BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.wrapContentWidth(unbounded = true).semantics { contentDescription = description },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.streak_flame),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = days.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        }
    }
}
