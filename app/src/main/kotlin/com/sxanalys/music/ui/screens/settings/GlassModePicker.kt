package com.sxanalys.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sxanalys.music.R
import com.sxanalys.music.constants.GlassModeKey
import com.sxanalys.music.ui.component.glassChip
import com.sxanalys.music.ui.theme.GlassMode
import com.sxanalys.music.utils.rememberPreference

@Composable
fun GlassModePicker(modifier: Modifier = Modifier) {
    val (modeId, setModeId) = rememberPreference(GlassModeKey, defaultValue = "dark")
    val current = GlassMode.byId(modeId)
    val options =
        listOf(
            GlassMode.OFF to R.string.sx_glassmode_off,
            GlassMode.DARK to R.string.sx_glassmode_dark,
            GlassMode.BRIGHT to R.string.sx_glassmode_bright,
        )
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
        Text(
            text = stringResource(R.string.sx_glassmode_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { (mode, label) ->
                val shape = RoundedCornerShape(14.dp)
                Text(
                    text = stringResource(label),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (mode == current) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (mode == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .weight(1f)
                            .glassChip(mode == current, shape)
                            .clickable { setModeId(mode.id) }
                            .padding(horizontal = 6.dp, vertical = 12.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.sx_glassmode_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
