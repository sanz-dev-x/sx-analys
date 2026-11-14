/**
 * Sx anlys Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.sxanalys.music.ui.screens.settings

import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import com.sxanalys.music.ui.component.glassPress
import com.sxanalys.music.ui.component.liquidGlass
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.sxanalys.music.ui.component.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sxanalys.innertube.models.WatchEndpoint
import com.sxanalys.music.BuildConfig
import com.sxanalys.music.LocalPlayerAwareWindowInsets
import com.sxanalys.music.LocalPlayerConnection
import com.sxanalys.music.R
import com.sxanalys.music.playback.PlayerConnection
import com.sxanalys.music.playback.queues.YouTubeQueue
import com.sxanalys.music.ui.component.IconButton
import com.sxanalys.music.ui.component.Material3SettingsGroup
import com.sxanalys.music.ui.component.Material3SettingsItem
import com.sxanalys.music.ui.utils.backToMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

private data class Contributor(
    val name: String,
    val roleRes: Int,
    val githubHandle: String,
    val avatarUrl: String = "https://github.com/$githubHandle.png",
    val githubUrl: String = "https://github.com/$githubHandle",
    val sponsorUrl: String? = null,
    val polygon: RoundedPolygon? = null,
    val favoriteSongVideoId: String? = null
)

private data class CommunityLink(
    val labelRes: Int,
    val iconRes: Int,
    val url: String
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val leadDeveloper = Contributor(
    name = "Sanz",
    roleRes = R.string.credits_lead_developer,
    githubHandle = "",
    polygon = MaterialShapes.Cookie9Sided,
    favoriteSongVideoId = null
)

private val communityLinks = listOf(
    CommunityLink(R.string.credits_view_repo, R.drawable.github, "https://github.com/sanz-dev-x/sx-analys"),
    CommunityLink(R.string.credits_license_name, R.drawable.info, "https://github.com/sanz-dev-x/sx-analys/blob/main/LICENSE")
)

private fun handleEasterEggClick(
    clickCount: Int,
    favoriteSongVideoId: String?,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    playerConnection: PlayerConnection?,
    wannaPlayStr: String,
    yeahStr: String,
    onCountUpdate: (Int) -> Unit
) {
    if (favoriteSongVideoId != null) {
        val newCount = clickCount + 1
        onCountUpdate(newCount)
        if (newCount >= 3) {
            onCountUpdate(0)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = wannaPlayStr,
                    actionLabel = yeahStr,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = favoriteSongVideoId)))
                }
            }
        }
    }
}

@Composable
private fun ContributorAvatar(
    avatarUrl: String,
    sizeDp: Int,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val fallback = painterResource(R.drawable.small_icon)
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.size(sizeDp.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = fallback,
            fallback = fallback,
            error = fallback,
        )
    }
}

@Composable
private fun SocialPill(
    iconRes: Int,
    handle: String,
    tintIcon: Boolean,
    url: String,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val ink = MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .height(52.dp)
            .glassPress(interaction, shape, pressedScale = 0.96f)
            .liquidGlass(shape, fill = ink.copy(alpha = 0.06f))
            .clickable(interactionSource = interaction, indication = null) { uriHandler.openUri(url) }
            .padding(horizontal = 14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (tintIcon) ink else Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = handle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SocialRow(
    iconRes: Int,
    label: String,
    handle: String,
    tintIcon: Boolean,
    url: String,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = 20.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (tintIcon) ink else Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = handle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeveloperCard(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(32.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(outer, fill = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(8.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.sanzx_banner),
            contentDescription = leadDeveloper.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1600f / 640f)
                .clip(RoundedCornerShape(24.dp)),
        )
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = leadDeveloper.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.credits_lead_developer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SocialPill(
                    iconRes = R.drawable.tiktok,
                    handle = "@sanz_crypt",
                    tintIcon = false,
                    url = "https://tiktok.com/@sanz_crypt",
                    uriHandler = uriHandler,
                    modifier = Modifier.weight(1f),
                )
                SocialPill(
                    iconRes = R.drawable.instagram,
                    handle = "@snzxvs",
                    tintIcon = true,
                    url = "https://www.instagram.com/snzxvs",
                    uriHandler = uriHandler,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
) {
    val uriHandler = LocalUriHandler.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val wannaPlayStr = stringResource(R.string.wanna_play_favorite_song)
    val yeahStr = stringResource(R.string.yeah)
    
    val windowInsets = LocalPlayerAwareWindowInsets.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                windowInsets.only(WindowInsetsSides.Top)
            )
        )

        Spacer(Modifier.height(16.dp))

        // Header: logo + name + version
        val ink = MaterialTheme.colorScheme.onSurface
        val soft = MaterialTheme.colorScheme.onSurfaceVariant
        val surface = MaterialTheme.colorScheme.surface
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.small_icon),
                contentDescription = stringResource(R.string.sxanalys),
                colorFilter = ColorFilter.tint(color = ink, blendMode = BlendMode.SrcIn),
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.sxanalys),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ink,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "${BuildConfig.VERSION_NAME} \u00B7 ${BuildConfig.ARCHITECTURE.lowercase(Locale.getDefault())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = soft,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Check for updates row
        val updateShape = RoundedCornerShape(24.dp)
        val updateInteraction = remember { MutableInteractionSource() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .glassPress(updateInteraction, updateShape, pressedScale = 0.98f)
                .liquidGlass(updateShape, fill = ink.copy(alpha = 0.05f))
                .clickable(interactionSource = updateInteraction, indication = null) {
                    navController.navigate("settings/updater")
                }
                .padding(horizontal = 18.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.check_for_updates_button),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = soft,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Developer card: full photo fading into the surface, name over the fade, avatar overlapping the corner
        val cardShape = RoundedCornerShape(32.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(cardShape)
                .background(surface),
        ) {
            Image(
                painter = painterResource(R.drawable.about_card),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.42f to Color.Transparent,
                            0.80f to surface.copy(alpha = 0.92f),
                            1.0f to surface,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp, end = 120.dp),
            ) {
                Text(
                    text = "S4NZ",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 40.sp,
                    letterSpacing = (-1).sp,
                    color = ink,
                )
                Text(
                    text = stringResource(R.string.credits_lead_developer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = soft,
                )
            }
            Image(
                painter = painterResource(R.drawable.about_cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = 40.dp)
                    .padding(end = 20.dp)
                    .size(92.dp)
                    .clip(CircleShape)
                    .border(4.dp, surface, CircleShape),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.about_social_media),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = soft,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 8.dp),
        )

        val socialShape = RoundedCornerShape(28.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(socialShape, fill = ink.copy(alpha = 0.05f)),
        ) {
            SocialRow(
                iconRes = R.drawable.tiktok,
                label = "TikTok",
                handle = "@sanz_crypt",
                tintIcon = false,
                url = "https://tiktok.com/@sanz_crypt",
                uriHandler = uriHandler,
            )
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ink.copy(alpha = 0.08f)),
            )
            SocialRow(
                iconRes = R.drawable.instagram,
                label = "Instagram",
                handle = "@snzxvs",
                tintIcon = true,
                url = "https://www.instagram.com/snzxvs",
                uriHandler = uriHandler,
            )
        }

        Spacer(Modifier.height(48.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        }
    )

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        )
    }
}
