package com.sxanalys.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxanalys.music.BuildConfig
import com.sxanalys.music.R
import com.sxanalys.music.utils.ReleaseInfo
import java.text.DecimalFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private sealed interface NoteLine {
    data class Heading(val text: String) : NoteLine

    data class Bullet(val text: String) : NoteLine

    data class Plain(val text: String) : NoteLine
}

private fun parseNotes(raw: String): List<NoteLine> =
    raw
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("---") }
        .map { line ->
            when {
                line.startsWith("#") -> NoteLine.Heading(line.trimStart('#').trim())
                line.startsWith("- ") || line.startsWith("* ") -> NoteLine.Bullet(stripInline(line.drop(2)))
                else -> NoteLine.Plain(stripInline(line))
            }
        }.toList()

private fun stripInline(text: String): String =
    text
        .replace(Regex("\\s*\\(@[^)]*\\)"), "")
        .replace(Regex("\\s+@\\w[\\w-]*"), "")
        .replace("**", "")
        .replace("`", "")
        .trim()

private fun formatSize(bytes: Long): String? {
    if (bytes <= 0) return null
    val mb = bytes / (1024.0 * 1024.0)
    return DecimalFormat("0.#").format(mb) + " MB"
}

private fun formatDate(iso: String): String? =
    runCatching {
        OffsetDateTime
            .parse(iso)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrNull()

@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    sizeBytes: Long,
    isKmp: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
) {
    val notes = remember(release.description) { parseNotes(release.description) }
    val sizeText = remember(sizeBytes) { formatSize(sizeBytes) }
    val dateText = remember(release.releaseDate) { formatDate(release.releaseDate) }
    val mono = FontFamily.Monospace

    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(14.dp),
                    ).padding(top = 22.dp, bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.update_dialog_label),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = mono,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = release.versionName,
                        fontFamily = mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = BuildConfig.BASE_VERSION_NAME,
                        fontFamily = mono,
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val meta = listOfNotNull(dateText, sizeText).joinToString("  ·  ")
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            if (isKmp) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.kmp_upgrade_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier =
                    Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (notes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.changelog_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                notes.forEach { line ->
                    when (line) {
                        is NoteLine.Heading ->
                            Text(
                                text = line.text.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = mono,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )

                        is NoteLine.Bullet ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    Modifier
                                        .padding(top = 7.dp)
                                        .size(5.dp)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                        is NoteLine.Plain ->
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onUpdate,
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(48.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            if (isKmp) R.string.kmp_upgrade_action else R.string.update_action,
                        ),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.update_dialog_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onLater) {
                    Text(stringResource(R.string.kmp_upgrade_later))
                }
            }
        }
    }
}
