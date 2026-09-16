package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Update Log:
 *   · Current installed version + baked-in release notes
 *   · Past 3 previous versions with per-entry ROLLBACK buttons
 *
 * Rollback requires the user to uninstall the current build first (Android forbids in-place
 * downgrade of a signed app), so we surface a warning dialog before invoking the download.
 */
@Composable
fun UpdateLogOverlay(
    updateManager: UpdateManager,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    val titleFont = titleFontFor(palette.name)
    val contentFont = contentFontFor(palette.name)

    val currentName = remember { updateManager.currentVersionName() }
    val currentCode = remember { updateManager.currentVersionCode() }
    var history by remember { mutableStateOf<List<UpdateInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var rollbackTarget by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        history = updateManager.fetchHistory()
        loading = false
    }

    // Past-3 = anything strictly older than the current installed version
    val past = history.filter { it.versionCode < currentCode }.take(3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.prismBorderBrush(), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(24.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "UPDATE LOG",
                        color = palette.onSurface,
                        fontSize = 22.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 4.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = palette.accentPrimary.copy(alpha = 0.35f),
                                blurRadius = 12f
                            )
                        )
                    )
                    Text(
                        text = "history · rollback · release notes",
                        color = palette.faint,
                        fontSize = 10.sp,
                        fontFamily = contentFont,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                CloseButton(palette) { onClose() }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Current version card ─────────────────
                CurrentVersionCard(
                    versionName = currentName.ifBlank { "?" },
                    releaseNotes = UpdateManager.CURRENT_RELEASE_NOTES,
                    titleFont = titleFont,
                    contentFont = contentFont,
                    palette = palette
                )

                // ── Past 3 versions ──────────────────────
                Text(
                    text = "PAST UPDATES",
                    color = palette.accentSecondary,
                    fontSize = 10.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, start = 2.dp)
                )

                when {
                    loading -> {
                        Text(
                            "· loading history …",
                            color = palette.faint,
                            fontSize = 12.sp,
                            fontFamily = contentFont,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    past.isEmpty() -> {
                        Text(
                            "· no earlier versions on record",
                            color = palette.faint,
                            fontSize = 12.sp,
                            fontFamily = contentFont,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    else -> {
                        past.forEach { info ->
                            PastVersionCard(
                                info = info,
                                titleFont = titleFont,
                                contentFont = contentFont,
                                palette = palette,
                                onRollback = { rollbackTarget = info }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "CLOSE",
                color = palette.faint,
                fontSize = 13.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable {
                        Haptics.click(ctx)
                        onClose()
                    }
            )
        }
    }

    // ── Rollback confirmation ─────────────────────────────
    rollbackTarget?.let { target ->
        RollbackConfirmDialog(
            target = target,
            palette = palette,
            titleFont = titleFont,
            contentFont = contentFont,
            onCancel = { rollbackTarget = null },
            onConfirm = {
                rollbackTarget = null
                updateManager.rollbackTo(target)
            }
        )
    }
}

@Composable
private fun CurrentVersionCard(
    versionName: String,
    releaseNotes: String,
    titleFont: androidx.compose.ui.text.font.FontFamily,
    contentFont: androidx.compose.ui.text.font.FontFamily,
    palette: ThemePalette
) {
    val bulletLines = remember(releaseNotes) {
        releaseNotes.trim()
            .split(Regex("[.•·]\\s+"))
            .map { it.trim().trimEnd(',', '.', '·', '•') }
            .filter { it.isNotBlank() }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.accentPrimary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "◈ CURRENT",
                color = palette.accentPrimary,
                fontSize = 10.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "v$versionName",
                color = palette.onSurface,
                fontSize = 16.sp,
                fontFamily = NokiaFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = palette.accentPrimary.copy(alpha = 0.5f),
                        blurRadius = 8f
                    )
                )
            )
        }
        Text(
            text = "WHAT'S IN THIS VERSION",
            color = palette.subtle,
            fontSize = 9.sp,
            fontFamily = titleFont,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (bulletLines.isEmpty()) {
            Text(
                text = "Enhancements and stability improvements.",
                color = palette.onSurface.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontFamily = contentFont,
                lineHeight = 18.sp
            )
        } else {
            bulletLines.forEach { line ->
                Row {
                    Text(
                        "•  ",
                        color = palette.accentPrimary,
                        fontSize = 13.sp,
                        fontFamily = contentFont
                    )
                    Text(
                        text = line,
                        color = palette.onSurface.copy(alpha = 0.90f),
                        fontSize = 13.sp,
                        fontFamily = contentFont,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PastVersionCard(
    info: UpdateInfo,
    titleFont: androidx.compose.ui.text.font.FontFamily,
    contentFont: androidx.compose.ui.text.font.FontFamily,
    palette: ThemePalette,
    onRollback: () -> Unit
) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.chipBg.copy(alpha = 0.55f))
            .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "v${info.versionName}",
                color = palette.onSurface,
                fontSize = 14.sp,
                fontFamily = NokiaFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (!info.date.isNullOrBlank()) {
                Text(
                    text = "  ·  ${info.date}",
                    color = palette.faint,
                    fontSize = 10.sp,
                    fontFamily = contentFont,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.danger.copy(alpha = 0.18f))
                    .border(1.dp, palette.danger.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .clickable {
                        Haptics.click(ctx)
                        onRollback()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "↺ ROLLBACK",
                    color = palette.danger,
                    fontSize = 10.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
        Text(
            text = info.releaseNotes,
            color = palette.subtle,
            fontSize = 12.sp,
            fontFamily = contentFont,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun RollbackConfirmDialog(
    target: UpdateInfo,
    palette: ThemePalette,
    titleFont: androidx.compose.ui.text.font.FontFamily,
    contentFont: androidx.compose.ui.text.font.FontFamily,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val ctx = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.danger.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠  ROLLBACK",
                color = palette.danger,
                fontSize = 16.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Text(
                text = "you are about to install v${target.versionName}",
                color = palette.onSurface,
                fontSize = 13.sp,
                fontFamily = contentFont,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Android does not allow in-place downgrade of a signed app. You will need " +
                    "to UNINSTALL the current build when prompted, then re-install the older APK. " +
                    "App-local data may be lost.",
                color = palette.subtle,
                fontSize = 11.sp,
                fontFamily = contentFont,
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.chipBg)
                        .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            Haptics.click(ctx)
                            onCancel()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "CANCEL",
                        color = palette.subtle,
                        fontSize = 12.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.danger.copy(alpha = 0.8f))
                        .clickable {
                            Haptics.click(ctx)
                            onConfirm()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "DOWNLOAD",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CloseButton(palette: ThemePalette, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(8.dp))
            .clickable {
                Haptics.click(ctx)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "×",
            color = palette.subtle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
