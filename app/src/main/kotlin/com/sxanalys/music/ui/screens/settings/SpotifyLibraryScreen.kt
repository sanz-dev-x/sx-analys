package com.sxanalys.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.sxanalys.music.ui.component.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.sxanalys.music.LocalPlayerAwareWindowInsets
import com.sxanalys.music.R
import com.sxanalys.music.spotify.SpotifyLibrary
import com.sxanalys.music.spotify.SpotifyPlaylist
import com.sxanalys.music.ui.component.IconButton
import com.sxanalys.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyLibraryScreen(navController: NavController) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(SpotifyLibrary.load(context)) }
    var opened by remember { mutableStateOf<SpotifyPlaylist?>(null) }

    Box(Modifier.fillMaxSize()) {
        val current = opened
        if (playlists.isEmpty()) {
            Text(
                stringResource(R.string.spotify_library_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else {
            LazyColumn(
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (current == null) {
                    items(playlists, key = { it.id }) { pl ->
                        Row(
                            Modifier.fillMaxWidth().clickable { opened = pl }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Cover(pl.artworkUrl, 56)
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(pl.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (pl.tracks.isEmpty()) stringResource(R.string.spotify_cover_only) else stringResource(R.string.spotify_track_count, pl.tracks.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = {
                                SpotifyLibrary.remove(context, pl.id)
                                playlists = SpotifyLibrary.load(context)
                            }) { Text(stringResource(R.string.delete)) }
                        }
                        HorizontalDivider(Modifier.padding(start = 86.dp), thickness = 0.5.dp)
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.spotify_metadata_only),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(current.tracks) { t ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(t.artworkUrl ?: current.artworkUrl, 44)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    t.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        TopAppBar(
            title = { Text(current?.name ?: stringResource(R.string.spotify_library)) },
            navigationIcon = {
                IconButton(
                    onClick = { if (opened != null) opened = null else navController.navigateUp() },
                    onLongClick = navController::backToMain,
                ) { Icon(painterResource(R.drawable.arrow_back), contentDescription = null) }
            },
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun Cover(url: String?, size: Int) {
    val shape = RoundedCornerShape(8.dp)
    if (url.isNullOrBlank()) {
        Box(Modifier.size(size.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
    } else {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).clip(shape))
    }
}
