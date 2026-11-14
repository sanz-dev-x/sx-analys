package com.sxanalys.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.sxanalys.music.constants.GlassRefractionKey
import com.sxanalys.music.constants.GlassFrostBlurKey
import com.sxanalys.music.constants.GlassNavBlurKey
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
        val (refractionId, setRefractionId) = rememberPreference(GlassRefractionKey, defaultValue = "normal")
        val refractionOptions =
            listOf(
                "off" to R.string.sx_refraction_off,
                "soft" to R.string.sx_refraction_soft,
                "normal" to R.string.sx_refraction_normal,
                "strong" to R.string.sx_refraction_strong,
            )
        Text(
            text = stringResource(R.string.sx_refraction_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            refractionOptions.forEach { (id, label) ->
                val shape = RoundedCornerShape(14.dp)
                val selected = id == refractionId
                Text(
                    text = stringResource(label),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .weight(1f)
                            .glassChip(selected, shape)
                            .clickable { setRefractionId(id) }
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.sx_refraction_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        val (frostBlur, setFrostBlur) = rememberPreference(GlassFrostBlurKey, defaultValue = 0.2f)
        Text(
            text = stringResource(R.string.sx_glass_blur_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            (0..20).forEach { step ->
                val value = step / 10f
                val shape = RoundedCornerShape(14.dp)
                val selected = kotlin.math.abs(frostBlur - value) < 0.05f
                Text(
                    text = if (step % 10 == 0) (step / 10).toString() else "0.$step".takeIf { step < 10 } ?: "1.${step - 10}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .glassChip(selected, shape)
                            .clickable { setFrostBlur(value) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        val (navBlur, setNavBlur) = rememberPreference(GlassNavBlurKey, defaultValue = 1f)
        Text(
            text = stringResource(R.string.sx_nav_blur_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            (1..20).forEach { step ->
                val value = step / 10f
                val shape = RoundedCornerShape(14.dp)
                val selected = kotlin.math.abs(navBlur - value) < 0.05f
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", value),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .glassChip(selected, shape)
                            .clickable { setNavBlur(value) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.sx_nav_blur_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
