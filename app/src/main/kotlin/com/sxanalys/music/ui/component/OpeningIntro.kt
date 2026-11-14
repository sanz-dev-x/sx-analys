package com.sxanalys.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxanalys.music.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INTRO_TEXT = "SX ANLYS"
private const val LETTER_STEP_MS = 90L
private const val LETTER_FADE_MS = 380

/**
 * LOCKED: this opening animation must stay identical on every app launch. Do not change its timing, order or content.
 * Opening: the logo rises from the bottom to the center, then the text appears letter by letter below it.
 */
@Composable
fun OpeningIntro(onFinished: () -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    val logoOffset = remember { Animatable(screenHeight) }
    val letters = remember { INTRO_TEXT.map { Animatable(0f) } }
    val logoSize = 170.dp

    LaunchedEffect(Unit) {
        logoOffset.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
        letters.forEachIndexed { i, a ->
            launch {
                delay(i * LETTER_STEP_MS)
                a.animateTo(1f, tween(LETTER_FADE_MS))
            }
        }
        delay(INTRO_TEXT.length * LETTER_STEP_MS + LETTER_FADE_MS + 500L)
        onFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_sx_logo),
            contentDescription = null,
            modifier = Modifier.size(logoSize).offset(y = logoOffset.value.dp),
        )
        Row(modifier = Modifier.offset(y = logoSize / 2 + 36.dp)) {
            INTRO_TEXT.forEachIndexed { i, ch ->
                val a = letters[i]
                Text(
                    text = ch.toString(),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = a.value
                            translationY = (1f - a.value) * 18.dp.toPx()
                        },
                )
            }
        }
    }
}
