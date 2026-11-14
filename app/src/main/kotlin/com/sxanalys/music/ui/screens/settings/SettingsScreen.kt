/**
 * Sx anlys Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.screens.settings

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sxanalys.music.constants.AppLanguageKey
import com.sxanalys.music.constants.SYSTEM_DEFAULT
import com.sxanalys.music.ui.component.EnumDialog
import com.sxanalys.music.utils.applyAppLanguage
import com.sxanalys.music.utils.rememberPreference
import java.util.Locale
import kotlinx.coroutines.launch
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.sxanalys.music.BuildConfig
import com.sxanalys.music.LocalPlayerAwareWindowInsets
import com.sxanalys.music.R
import com.sxanalys.music.ui.component.Material3SettingsGroup
import com.sxanalys.music.ui.component.Material3SettingsItem
import com.sxanalys.music.ui.component.ReleaseNotesCard
import com.sxanalys.music.utils.Updater
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sxanalys.innertube.utils.parseCookieString
import com.sxanalys.music.LocalDatabase
import com.sxanalys.music.constants.InnerTubeCookieKey
import com.sxanalys.music.constants.ListenTogetherInTopBarKey
import com.sxanalys.music.constants.PauseListenHistoryKey
import com.sxanalys.music.ui.component.glassPanel
import com.sxanalys.music.ui.component.glassPress
import com.sxanalys.music.ui.component.liquidGlass
import com.sxanalys.music.utils.computeListeningStreak
import com.sxanalys.music.viewmodels.HomeViewModel
import java.time.LocalDate

@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scope = rememberCoroutineScope()
    val (storedLanguage) = rememberPreference(AppLanguageKey, defaultValue = SYSTEM_DEFAULT)
    val currentLanguage =
        if (storedLanguage == SYSTEM_DEFAULT) {
            if (Locale.getDefault().language == "id") "id" else "en"
        } else {
            storedLanguage.substringBefore('-')
        }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    if (showLanguageDialog) {
        EnumDialog(
            onDismiss = { showLanguageDialog = false },
            onSelect = { tag ->
                showLanguageDialog = false
                if (tag != currentLanguage) {
                    (context as? Activity)?.let { activity -> scope.launch { activity.applyAppLanguage(tag) } }
                }
            },
            title = stringResource(R.string.language),
            current = currentLanguage,
            values = listOf("id", "en"),
            valueText = {
                stringResource(if (it == "id") R.string.language_indonesian else R.string.language_english)
            },
        )
    }

    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo(
                "com.google.android.projection.gearhead", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        SettingsProfileCard(navController = navController)

        Spacer(modifier = Modifier.height(16.dp))

        SettingsActivityGroup(navController = navController)

        Spacer(modifier = Modifier.height(16.dp))

        // User Interface Section
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.appearance)) },
                    onClick = { navController.navigate("settings/appearance") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.palette),
                    title = { Text(stringResource(R.string.app_icon)) },
                    onClick = { navController.navigate("settings/app_icon") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.language)) },
                    description = {
                        Text(
                            stringResource(
                                if (currentLanguage == "id") R.string.language_indonesian else R.string.language_english,
                            ),
                        )
                    },
                    onClick = { showLanguageDialog = true },
                ),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.notification),
                    title = { Text(stringResource(R.string.sx_notifications)) },
                    onClick = { navController.navigate("settings/notifications") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.sx_dynamic_island)) },
                    onClick = { navController.navigate("settings/dynamic_island") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        StreakSettingsGroup()

        Spacer(modifier = Modifier.height(16.dp))

        // Player & Content Section (moved up and combined with content)
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.ios_play),
                    title = { Text(stringResource(R.string.player_and_audio)) },
                    onClick = { navController.navigate("settings/player") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.language),
                    title = { Text(stringResource(R.string.content)) },
                    onClick = { navController.navigate("settings/content") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.translate),
                    title = { Text(stringResource(R.string.ai_lyrics_translation)) },
                    onClick = { navController.navigate("settings/ai") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Android Auto Section — only shown if Android Auto is installed
        if (hasAndroidAuto) {
            Material3SettingsGroup(
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.ic_android_auto),
                        title = { Text(stringResource(R.string.android_auto)) },
                        onClick = { navController.navigate("settings/android_auto") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Privacy & Security Section
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.security),
                    title = { Text(stringResource(R.string.privacy)) },
                    onClick = { navController.navigate("settings/privacy") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Storage & Data Section
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.storage),
                    title = { Text(stringResource(R.string.storage)) },
                    onClick = { navController.navigate("settings/storage") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.restore),
                    title = { Text(stringResource(R.string.backup_restore)) },
                    onClick = { navController.navigate("settings/backup_restore") }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System & About Section
        Material3SettingsGroup(
            items = buildList {
                if (isAndroid12OrLater) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.link),
                            title = { Text(stringResource(R.string.default_links)) },
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                        "package:${context.packageName}".toUri()
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    when (e) {
                                        is ActivityNotFoundException -> {
                                            Toast.makeText(
                                                context,
                                                R.string.open_app_settings_error,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        is SecurityException -> {
                                            Toast.makeText(
                                                context,
                                                R.string.open_app_settings_error,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        else -> {
                                            Toast.makeText(
                                                context,
                                                R.string.open_app_settings_error,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                    )
                }
                if (BuildConfig.UPDATER_AVAILABLE) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.update),
                            title = { Text(stringResource(R.string.updater)) },
                            onClick = { navController.navigate("settings/updater") }
                        )
                    )
                }
                val showChangelog = com.sxanalys.music.LocalChangelogState.current
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.newspaper),
                        title = { Text(stringResource(R.string.changelog)) },
                        onClick = { showChangelog.value = true }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text(stringResource(R.string.about)) },
                        onClick = { navController.navigate("settings/about") }
                    )
                )
                if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME) {
                    val releaseInfo = Updater.getCachedLatestRelease()
                    val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }

                    if (downloadUrl != null) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.update),
                                title = { 
                                    Text(
                                        text = stringResource(R.string.new_version_available),
                                    )
                                },
                                description = {
                                    Text(
                                        text = latestVersionName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                showBadge = true,
                                onClick = { uriHandler.openUri(downloadUrl) }
                            )
                        )
                    }
                }
            }
        )
    if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME) {
            Spacer(modifier = Modifier.height(16.dp))
            ReleaseNotesCard()
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
private fun SettingsProfileCard(navController: NavController) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, defaultValue = "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val shape = RoundedCornerShape(26.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .glassPress(interactionSource, shape, pressedScale = 0.985f)
                .glassPanel(shape, strength = 0.9f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { navController.navigate(if (isLoggedIn) "account" else "login") },
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoggedIn && accountImageUrl != null) {
                AsyncImage(
                    model = accountImageUrl,
                    contentDescription = stringResource(R.string.account),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.account),
                    contentDescription = stringResource(R.string.account),
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isLoggedIn) accountName else stringResource(R.string.login),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.account),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier =
                Modifier
                    .height(36.dp)
                    .liquidGlass(CircleShape, fill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable { navController.navigate("settings/account") }
                    .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.account),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SettingsActivityGroup(navController: NavController) {
    val database = LocalDatabase.current
    val (pauseListenHistory) = rememberPreference(PauseListenHistoryKey, defaultValue = false)
    val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
    val eventCount by database.eventCount().collectAsStateWithLifecycle(initialValue = 0)
    val listeningDays by database.listeningDays().collectAsStateWithLifecycle(initialValue = emptyList())
    val streak =
        remember(listeningDays) {
            computeListeningStreak(
                listeningDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
                LocalDate.now(),
            )
        }
    val showHistory = !(pauseListenHistory && eventCount == 0)

    Material3SettingsGroup(
        items =
            buildList {
                if (showHistory) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.history),
                            title = { Text(stringResource(R.string.history)) },
                            onClick = { navController.navigate("history") },
                        ),
                    )
                }
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.streak_flame),
                        title = { Text(stringResource(R.string.streak_title)) },
                        description = {
                            Text("${streak.current} ${pluralStringResource(R.plurals.streak_days, streak.current)}")
                        },
                        onClick = { navController.navigate("stats") },
                    ),
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.stats),
                        title = { Text(stringResource(R.string.stats)) },
                        onClick = { navController.navigate("stats") },
                    ),
                )
                if (listenTogetherInTopBar) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.group_outlined),
                            title = { Text(stringResource(R.string.together)) },
                            onClick = { navController.navigate("listen_together_from_topbar") },
                        ),
                    )
                }
            },
    )
}
