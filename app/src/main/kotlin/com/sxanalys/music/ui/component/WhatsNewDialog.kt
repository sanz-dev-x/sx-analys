package com.sxanalys.music.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sxanalys.music.R

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sx_whatsnew_title)) },
        text = { Text(stringResource(R.string.sx_whatsnew_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.sx_whatsnew_ok)) }
        },
    )
}
