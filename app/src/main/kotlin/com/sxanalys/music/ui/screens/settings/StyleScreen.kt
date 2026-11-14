package com.sxanalys.music.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import com.sxanalys.music.ui.component.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sxanalys.music.R
import com.sxanalys.music.constants.AppStyleKey
import com.sxanalys.music.constants.GlassMaterialKey
import com.sxanalys.music.constants.NavSizeKey
import com.sxanalys.music.ui.component.glassChip
import com.sxanalys.music.ui.component.liquidGlass
import com.sxanalys.music.ui.theme.AppStyle
import com.sxanalys.music.ui.theme.AppStyles
import com.sxanalys.music.ui.theme.GlassMaterial
import com.sxanalys.music.ui.theme.LocalGlassAccent
import com.sxanalys.music.ui.theme.LocalGlassMaterial
import com.sxanalys.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleScreen(navController: NavController) {
    val (styleId, setStyleId) = rememberPreference(AppStyleKey, defaultValue = "ios_blue")
    val (glassId, setGlassId) = rememberPreference(GlassMaterialKey, defaultValue = "auto")
    val (navSize, setNavSize) = rememberPreference(NavSizeKey, defaultValue = "auto")

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val currentStyle = AppStyles.byId(styleId)

    val navOptions =
        listOf(
            stringResource(R.string.sx_nav_auto) to "auto",
            stringResource(R.string.sx_nav_large) to "large",
            stringResource(R.string.sx_nav_normal) to "normal",
        )
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.sx_style_title)) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
            },
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
        ) {
            SectionTitle(R.string.sx_style_section_theme)
            Text(
                text = stringResource(R.string.sx_style_section_theme_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            AppStyles.all.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { style ->
                        StyleCard(
                            style = style,
                            selected = style.id == currentStyle.id,
                            dark = dark,
                            modifier = Modifier.weight(1f),
                            onClick = { setStyleId(style.id) },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            SectionTitle(R.string.sx_style_section_glass)
            GlassRow(
                label = R.string.sx_glass_auto,
                note = R.string.sx_glass_auto_note,
                material = currentStyle.glass,
                selected = glassId == "auto" || GlassMaterial.byId(glassId) == null,
                onClick = { setGlassId("auto") },
            )
            GlassMaterial.entries.forEach { material ->
                GlassRow(
                    label = material.label,
                    note = material.note,
                    material = material,
                    selected = glassId == material.id,
                    onClick = { setGlassId(material.id) },
                )
            }

            SectionTitle(R.string.sx_style_section_nav)
            ChoiceRow(options = navOptions, selected = navSize, onSelect = { setNavSize(it) })
            Text(
                text = stringResource(R.string.sx_nav_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SectionTitle(
    @StringRes id: Int,
) {
    Text(
        text = stringResource(id),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun StyleCard(
    style: AppStyle,
    selected: Boolean,
    dark: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val bg = style.bg(dark)
    val card = style.card(dark)
    val ink = style.ink(dark)
    val accent = style.accent(dark)
    val shape = RoundedCornerShape(16.dp)
    val line = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Column(
        modifier =
            modifier
                .clip(shape)
                .border(if (selected) 2.dp else 1.dp, line, shape)
                .clickable(onClick = onClick)
                .padding(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(54.dp)
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = 0.7f)),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(card)
                            .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = 0.25f)),
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 7.dp)
                        .clip(CircleShape)
                        .background(card)
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .size(if (style.ios) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) accent else ink.copy(alpha = 0.3f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(style.label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(style.note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GlassRow(
    @StringRes label: Int,
    @StringRes note: Int,
    material: GlassMaterial,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalGlassAccent.current
    val surface = MaterialTheme.colorScheme.surface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(64.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(accent, MaterialTheme.colorScheme.secondaryContainer))),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalGlassMaterial provides material) {
                Box(
                    Modifier
                        .width(44.dp)
                        .height(24.dp)
                        .liquidGlass(RoundedCornerShape(8.dp), fill = surface.copy(alpha = 0.25f)),
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
        ) {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (label, value) ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .glassChip(value == selected, shape)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}
