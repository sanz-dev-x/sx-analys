package com.sxanalys.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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

private val bgRes =
    mapOf(
        "default" to R.drawable.ic_launcher_glass_bg_default,
        "blue" to R.drawable.ic_launcher_glass_bg_blue,
        "teal" to R.drawable.ic_launcher_glass_bg_teal,
        "green" to R.drawable.ic_launcher_glass_bg_green,
        "orange" to R.drawable.ic_launcher_glass_bg_orange,
        "red" to R.drawable.ic_launcher_glass_bg_red,
        "pink" to R.drawable.ic_launcher_glass_bg_pink,
        "purple" to R.drawable.ic_launcher_glass_bg_purple,
        "graphite" to R.drawable.ic_launcher_glass_bg_graphite,
    )

@Composable
private fun GlassIcon(color: String, size: Dp, shape: Shape, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(shape), contentAlignment = Alignment.Center) {
        Image(painterResource(bgRes.getValue(color)), null, Modifier.requiredSize(size * 1.5f))
        Image(painterResource(R.drawable.ic_launcher_glass_fg), null, Modifier.requiredSize(size * 1.5f))
        Box(
            Modifier.matchParentSize().border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, Color.White.copy(alpha = 0.25f))),
                shape,
            ),
        )
    }
}

@Composable
private fun ColorDot(color: String, selected: Boolean, onClick: () -> Unit) {
    val ring by animateFloatAsState(if (selected) 1f else 0f, spring(0.7f, Spring.StiffnessMedium), label = "dotRing")
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, spring(0.55f, Spring.StiffnessMedium), label = "dotScale")
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .border(2.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = ring), CircleShape)
            .padding(4.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        GlassIcon(color, 36.dp, CircleShape)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppIconSettings(navController: NavController) {
    val context = LocalContext.current
    val current = remember { IconUtils.currentVariant(context) ?: "default" }
    var color by remember { mutableStateOf(current) }
    var applied by remember { mutableStateOf(current) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(24.dp))
        Crossfade(color, animationSpec = tween(260), label = "iconPreview") { c ->
            GlassIcon(c, 112.dp, RoundedCornerShape(28.dp))
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(color, 52.dp, CircleShape)
            GlassIcon(color, 52.dp, RoundedCornerShape(18.dp))
            GlassIcon(color, 52.dp, RoundedCornerShape(12.dp))
        }
        Spacer(Modifier.height(28.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (listOf("default") + IconUtils.colors).forEach { c -> ColorDot(c, c == color) { color = c } }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (color == "default") IconUtils.resetToDefault(context) else IconUtils.setVariant(context, color)
                applied = color
                Toast.makeText(context, R.string.app_icon_applied, Toast.LENGTH_SHORT).show()
            },
            enabled = applied != color,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text(stringResource(R.string.app_icon_apply)) }
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
