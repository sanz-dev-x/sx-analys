package com.sxanalys.music.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.sxanalys.music.ui.component.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import com.sxanalys.music.LocalPlayerAwareWindowInsets
import com.sxanalys.music.R
import com.sxanalys.music.constants.DynamicIslandCurvatureKey
import com.sxanalys.music.constants.DynamicIslandEnabledKey
import com.sxanalys.music.constants.DynamicIslandOnTrackChangeKey
import com.sxanalys.music.constants.DynamicIslandStyle
import com.sxanalys.music.constants.DynamicIslandStyleKey
import com.sxanalys.music.constants.DynamicIslandTopOffsetKey
import com.sxanalys.music.constants.DynamicIslandWidthKey
import com.sxanalys.music.constants.DynamicIslandHeightKey
import com.sxanalys.music.dynamicisland.IslandDefaultHeightDp
import com.sxanalys.music.dynamicisland.IslandDefaultWidthDp
import com.sxanalys.music.dynamicisland.DynamicIslandContent
import com.sxanalys.music.dynamicisland.IslandCallbacks
import com.sxanalys.music.dynamicisland.IslandTrack
import com.sxanalys.music.ui.component.IconButton
import com.sxanalys.music.ui.component.Material3SettingsGroup
import com.sxanalys.music.ui.component.Material3SettingsItem
import com.sxanalys.music.ui.utils.backToMain
import com.sxanalys.music.utils.rememberEnumPreference
import com.sxanalys.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicIslandSettings(navController: NavController) {
    val context = LocalContext.current
    val (enabled, onEnabledChange) = rememberPreference(DynamicIslandEnabledKey, defaultValue = false)
    val (style, onStyleChange) = rememberEnumPreference(DynamicIslandStyleKey, defaultValue = DynamicIslandStyle.IPHONE)
    val (curvature, onCurvatureChange) = rememberPreference(DynamicIslandCurvatureKey, defaultValue = 100)
    val (islandWidth, onIslandWidthChange) = rememberPreference(DynamicIslandWidthKey, defaultValue = IslandDefaultWidthDp)
    val (islandHeight, onIslandHeightChange) = rememberPreference(DynamicIslandHeightKey, defaultValue = IslandDefaultHeightDp)
    val (topOffset, onTopOffsetChange) = rememberPreference(DynamicIslandTopOffsetKey, defaultValue = 6)
    val (onTrackChange, onOnTrackChangeChange) = rememberPreference(DynamicIslandOnTrackChangeKey, defaultValue = true)

    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    LifecycleResumeEffect(Unit) {
        canDraw = Settings.canDrawOverlays(context)
        onPauseOrDispose { }
    }

    var previewExpanded by remember { mutableStateOf(false) }
    val sampleTitle = stringResource(R.string.sx_island_sample_title)
    val sampleArtist = stringResource(R.string.sx_island_sample_artist)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2A1F18), Color(0xFF3A2A1E), Color(0xFF14110F)),
                        ),
                    ),
            contentAlignment = Alignment.TopCenter,
        ) {
            DynamicIslandContent(
                track = IslandTrack(sampleTitle, sampleArtist, null, isPlaying = true, progress = 0.42f),
                expanded = previewExpanded,
                style = style,
                curvaturePercent = curvature,
                compactWidthDp = islandWidth,
                compactHeightDp = islandHeight,
                callbacks =
                    IslandCallbacks(
                        onToggleExpand = { previewExpanded = true },
                        onOpenApp = { previewExpanded = false },
                    ),
                modifier = Modifier.padding(top = (14 + topOffset).dp),
            )
            Text(
                text = stringResource(R.string.sx_island_preview_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.sx_section_island),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.music_note),
                        title = { Text(stringResource(R.string.sx_island_enable)) },
                        description = { Text(stringResource(R.string.sx_island_enable_desc)) },
                        trailingContent = { IslandSwitch(enabled, onEnabledChange) },
                        onClick = { onEnabledChange(!enabled) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.security),
                        title = { Text(stringResource(R.string.sx_island_permission)) },
                        description = {
                            Text(
                                stringResource(
                                    if (canDraw) R.string.sx_island_permission_granted else R.string.sx_island_permission_missing,
                                ),
                            )
                        },
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = { Text(stringResource(R.string.sx_island_on_track_change)) },
                        description = { Text(stringResource(R.string.sx_island_on_track_change_desc)) },
                        trailingContent = { IslandSwitch(onTrackChange, onOnTrackChangeChange) },
                        onClick = { onOnTrackChangeChange(!onTrackChange) },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.sx_island_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            FilterChip(
                selected = style == DynamicIslandStyle.IPHONE,
                onClick = { onStyleChange(DynamicIslandStyle.IPHONE) },
                label = { Text(stringResource(R.string.sx_island_style_iphone)) },
            )
            FilterChip(
                selected = style == DynamicIslandStyle.LIQUID_GLASS,
                onClick = { onStyleChange(DynamicIslandStyle.LIQUID_GLASS) },
                label = { Text(stringResource(R.string.sx_island_style_glass)) },
            )
        }

        Spacer(Modifier.height(12.dp))

        SliderRow(
            label = stringResource(R.string.sx_island_curvature),
            valueText = "$curvature%",
            value = curvature.toFloat(),
            range = 0f..100f,
            onValueChange = { onCurvatureChange(it.roundToInt()) },
        )
        SliderRow(
            label = stringResource(R.string.sx_island_width),
            valueText = "$islandWidth dp",
            value = islandWidth.toFloat(),
            range = 100f..260f,
            onValueChange = { onIslandWidthChange(it.roundToInt()) },
        )
        SliderRow(
            label = stringResource(R.string.sx_island_height),
            valueText = "$islandHeight dp",
            value = islandHeight.toFloat(),
            range = 28f..64f,
            onValueChange = { onIslandHeightChange(it.roundToInt()) },
        )
        SliderRow(
            label = stringResource(R.string.sx_island_top_offset),
            valueText = "$topOffset dp",
            value = topOffset.toFloat(),
            range = 0f..60f,
            onValueChange = { onTopOffsetChange(it.roundToInt()) },
        )

        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.sx_dynamic_island)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun IslandSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            Icon(
                painter = painterResource(if (checked) R.drawable.check else R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
