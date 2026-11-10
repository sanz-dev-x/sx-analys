package com.sxanalys.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sxanalys.music.R
import com.sxanalys.music.constants.StreakReminderEnabledKey
import com.sxanalys.music.constants.StreakReminderHourKey
import com.sxanalys.music.ui.component.ListDialog
import com.sxanalys.music.ui.component.Material3SettingsGroup
import com.sxanalys.music.ui.component.Material3SettingsItem
import com.sxanalys.music.utils.StreakReminder
import com.sxanalys.music.utils.rememberPreference
import kotlinx.coroutines.launch

private val ReminderHours = listOf(12, 15, 17, 18, 19, 20, 21, 22)

private fun hourLabel(hour: Int) = "%02d.00".format(hour)

@Composable
fun StreakSettingsGroup() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (enabled, onEnabledChange) = rememberPreference(StreakReminderEnabledKey, defaultValue = true)
    val (hour, onHourChange) = rememberPreference(StreakReminderHourKey, defaultValue = StreakReminder.DEFAULT_HOUR)
    var showHourDialog by rememberSaveable { mutableStateOf(false) }

    if (showHourDialog) {
        ListDialog(onDismiss = { showHourDialog = false }) {
            items(ReminderHours) { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onHourChange(option)
                                showHourDialog = false
                                scope.launch {
                                    kotlinx.coroutines.delay(150)
                                    StreakReminder.schedule(context.applicationContext)
                                }
                            }.padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    RadioButton(selected = option == hour, onClick = null)
                    Text(
                        text = hourLabel(option),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }

    Material3SettingsGroup(
        title = stringResource(R.string.streak_section_title),
        items =
            listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.streak_flame),
                    title = { Text(stringResource(R.string.streak_reminder)) },
                    description = { Text(stringResource(R.string.streak_reminder_desc)) },
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                onEnabledChange(it)
                                scope.launch {
                                    kotlinx.coroutines.delay(150)
                                    StreakReminder.schedule(context.applicationContext)
                                }
                            },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (enabled) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    },
                    onClick = {
                        onEnabledChange(!enabled)
                        scope.launch {
                            kotlinx.coroutines.delay(150)
                            StreakReminder.schedule(context.applicationContext)
                        }
                    },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.timer),
                    title = { Text(stringResource(R.string.streak_reminder_time)) },
                    description = { Text(hourLabel(hour)) },
                    enabled = enabled,
                    onClick = { showHourDialog = true },
                ),
            ),
    )
}
