/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.sxanalys.music.R
import androidx.compose.ui.unit.dp

/**
 * iOS inset-grouped settings list: one continuous rounded card, hairline separators inset past the
 * icon, small uppercase section header, chevron for navigable rows.
 */
@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 8.dp),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .glassPanel(RoundedCornerShape(22.dp)),
        ) {
            items.forEachIndexed { index, item ->
                Material3SettingsItemRow(item = item)
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = if (item.icon != null || item.leadingContent != null) 58.dp else 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Material3SettingsItemRow(item: Material3SettingsItem) {
    val contentAlpha = if (item.enabled) 1f else 0.38f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "settingsItemPress",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    enabled = item.enabled && item.onClick != null,
                    onClick = { item.onClick?.invoke() },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(14.dp))
        } else if (item.icon != null) {
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                if (item.showBadge) {
                    BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                        Icon(painter = item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(painter = item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)),
            ) { item.title() }
            item.description?.let { desc ->
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)),
                ) { desc() }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
        if (item.trailingContent == null && item.onClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Data class for Material 3 settings item
 */
data class Material3SettingsItem(
    val icon: Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)
