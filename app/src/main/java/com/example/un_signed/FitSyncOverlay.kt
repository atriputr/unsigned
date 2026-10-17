package com.example.un_signed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.launch

/**
 * Sync-with-Fit modal.
 *   · Shows live progress while [FitSyncManager.syncAll] walks each HC record type.
 *   · When finished, swaps to a compact summary of what was imported.
 *   · "RE-SYNC" + "CLOSE" buttons on completion.
 *
 * The caller is responsible for requesting Health Connect permissions BEFORE opening this
 * overlay — if permissions are missing the sync still runs but each row returns null and the
 * summary reflects "No metrics available from Fit yet".
 */
@Composable
fun FitSyncOverlay(
    onRequestPermissions: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    val titleFont = titleFontFor(palette.name)
    val contentFont = contentFontFor(palette.name)
    val scope = rememberCoroutineScope()

    val progress by FitSyncManager.progress.collectAsState()
    val animatedFraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(260),
        label = "syncProgress"
    )

    var result by remember { mutableStateOf<FitSyncManager.SyncResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }
    var grantedAll by remember { mutableStateOf<Boolean?>(null) }

    fun kickoff() {
        if (running) return
        running = true
        result = null
        error = null
        FitSyncManager.resetProgress()
        scope.launch {
            grantedAll = runCatching { FitSyncManager.hasAllPermissions(ctx) }.getOrDefault(false)
            try {
                result = FitSyncManager.syncAll(ctx)
            } catch (e: Exception) {
                error = e.message ?: "Sync failed"
            } finally {
                running = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!FitSyncManager.hasAllPermissions(ctx)) {
            // Nudge for permissions first, let user re-run from the panel.
            grantedAll = false
        }
        kickoff()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { if (!running) onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.prismBorderBrush(), RoundedCornerShape(22.dp))
                .clickable(enabled = false) { }
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYNC WITH FIT",
                color = palette.onSurface,
                fontSize = 22.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = palette.accentPrimary.copy(alpha = 0.35f),
                        blurRadius = 12f
                    )
                )
            )
            Text(
                text = "importing biometrics from Health Connect",
                color = palette.faint,
                fontSize = 10.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(Modifier.height(18.dp))

            when {
                error != null -> {
                    Text(
                        text = "✕  ${error}",
                        color = palette.danger,
                        fontSize = 13.sp,
                        fontFamily = contentFont,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                running || result == null -> {
                    ProgressBar(animatedFraction, palette)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (progress.task.isNotBlank()) "pulling ${progress.task} …" else "preparing …",
                        color = palette.subtle,
                        fontSize = 12.sp,
                        fontFamily = NokiaFont,
                        letterSpacing = 1.sp
                    )
                }
                else -> {
                    ResultPanel(result!!, grantedAll, palette, contentFont, titleFont)
                }
            }

            // ── Buttons ─────────────────────────────────────
            Spacer(Modifier.height(18.dp))

            if (!running && result != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (grantedAll == false) {
                        PanelButton(
                            label = "GRANT PERMISSIONS",
                            accent = true,
                            titleFont = titleFont,
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        ) {
                            Haptics.click(ctx)
                            onRequestPermissions()
                        }
                    } else {
                        PanelButton(
                            label = "RE-SYNC",
                            accent = true,
                            titleFont = titleFont,
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        ) {
                            Haptics.click(ctx)
                            kickoff()
                        }
                    }
                    PanelButton(
                        label = "CLOSE",
                        accent = false,
                        titleFont = titleFont,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    ) {
                        Haptics.click(ctx); onClose()
                    }
                }
            } else if (!running) {
                PanelButton(
                    label = "CANCEL",
                    accent = false,
                    titleFont = titleFont,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth()
                ) { onClose() }
            }
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, palette: ThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(palette.accentPrimary)
        )
    }
}

@Composable
private fun ResultPanel(
    result: FitSyncManager.SyncResult,
    grantedAll: Boolean?,
    palette: ThemePalette,
    contentFont: androidx.compose.ui.text.font.FontFamily,
    titleFont: androidx.compose.ui.text.font.FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.success.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✓ SYNCED", color = palette.success, fontSize = 11.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${result.granted} / ${result.requested} perms",
                color = palette.subtle,
                fontSize = 10.sp,
                fontFamily = NokiaFont,
                letterSpacing = 1.sp
            )
        }
        Text(
            text = result.summary(),
            color = palette.onSurface.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontFamily = contentFont,
            lineHeight = 17.sp
        )
        if (grantedAll == false) {
            Text(
                text = "Some permissions aren't granted yet. Tap GRANT PERMISSIONS to open the Health Connect sheet for the missing types.",
                color = palette.subtle,
                fontSize = 10.sp,
                fontFamily = contentFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            text = "took ${result.durationMs} ms",
            color = palette.faint,
            fontSize = 9.sp,
            fontFamily = NokiaFont,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PanelButton(
    label: String,
    accent: Boolean,
    titleFont: androidx.compose.ui.text.font.FontFamily,
    palette: ThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent) palette.accentPrimary.copy(alpha = 0.85f) else palette.chipBg)
            .border(1.dp, if (accent) palette.accentPrimary else palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (accent) Color.Black else palette.subtle,
            fontSize = 12.sp,
            fontFamily = titleFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}
