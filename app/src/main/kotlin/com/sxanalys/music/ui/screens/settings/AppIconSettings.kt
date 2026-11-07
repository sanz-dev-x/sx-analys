package com.sxanalys.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sxanalys.music.LocalPlayerAwareWindowInsets
import com.sxanalys.music.R
import com.sxanalys.music.ui.component.IconButton
import com.sxanalys.music.ui.utils.backToMain
import com.sxanalys.music.utils.IconUtils

private val designRes =
    mapOf(
        "note" to R.drawable.ic_fg_note,
        "bars" to R.drawable.ic_fg_bars,
        "vinyl" to R.drawable.ic_fg_vinyl,
        "phones" to R.drawable.ic_fg_phones,
    )
private val colorValue =
    mapOf(
        "blue" to Color(0xFF0A84FF),
        "pink" to Color(0xFFFF375F),
        "orange" to Color(0xFFFF9F0A),
        "green" to Color(0xFF30D158),
        "purple" to Color(0xFFBF5AF2),
        "graphite" to Color(0xFF1C1C1E),
    )

@Composable
private fun IconTile(design: String, color: String, size: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.225f).dp))
            .background(colorValue.getValue(color))
            .border(
                if (selected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape((size * 0.225f).dp),
            ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(designRes.getValue(design)), null, tint = Color.Unspecified, modifier = Modifier.size((size * 1.1f).dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconSettings(navController: NavController) {
    val context = LocalContext.current
    val current = remember { IconUtils.currentVariant(context) }
    var design by remember { mutableStateOf(current?.first ?: "note") }
    var color by remember { mutableStateOf(current?.second ?: "blue") }
    var applied by remember { mutableStateOf(current) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(16.dp))
        IconTile(design, color, 96, false) {}
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.app_icon_design), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconUtils.designs.forEach { d -> IconTile(d, color, 64, d == design) { design = d } }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.app_icon_color), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconUtils.colors.forEach { c ->
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorValue.getValue(c))
                        .border(if (c == color) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent), CircleShape)
                        .clickable { color = c },
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                IconUtils.setVariant(context, design, color)
                applied = design to color
                Toast.makeText(context, R.string.app_icon_applied, Toast.LENGTH_SHORT).show()
            },
            enabled = applied != (design to color),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text(stringResource(R.string.app_icon_apply)) }
        TextButton(
            onClick = {
                IconUtils.resetToDefault(context)
                applied = null
                Toast.makeText(context, R.string.app_icon_applied, Toast.LENGTH_SHORT).show()
            },
        ) { Text(stringResource(R.string.app_icon_default)) }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.app_icon)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}
