package com.sxanalys.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxanalys.music.ui.utils.resize

val HomeCardRadius = 18.dp
val HomeGridGap = 10.dp
val HomeSideInset = 16.dp
val QuickPickCardHeight = 64.dp

/** Kartu melengkung: sampul di kiri, judul di kanan. Dipakai grid 2 kolom di Beranda. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPickCard(
    title: String,
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(HomeCardRadius)
    Row(
        modifier =
            modifier
                .height(QuickPickCardHeight)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemThumbnail(
            thumbnailUrl = thumbnailUrl?.resize(200, 200),
            isActive = isActive,
            isPlaying = isPlaying,
            shape = shape,
            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
    }
}
