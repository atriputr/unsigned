package com.example.un_signed

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
    // availability: AVAILABLE | NEEDS_UPDATE | UNAVAILABLE  — Health Connect provider status.
    var availability by remember {
        mutableStateOf(runCatching { FitSyncManager.availability(ctx) }
            .getOrDefault(FitnessDataRepository.Availability.UNAVAILABLE))
    }

    fun kickoff() {
        if (running) return
        if (availability != FitnessDataRepository.Availability.AVAILABLE) return
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

    /**
     * Opens the Google Play Store listing for Health Connect (works on Android < 14 where
     * HC isn't preinstalled). Falls back to the browser if Play Store isn't available.
     */
    fun openPlayStoreForHealthConnect() {
        val pkg = "com.google.android.apps.healthdata"
        val marketUri = Uri.parse("market://details?id=$pkg")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
        val intents = listOf(
            Intent(Intent.ACTION_VIEW, marketUri).setPackage("com.android.vending")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Intent.ACTION_VIEW, marketUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        for (i in intents) {
            try { ctx.startActivity(i); return } catch (_: Exception) { }
        }
        Toast.makeText(ctx, "Install Health Connect from Play Store manually", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        // Re-check availability every time the overlay opens — user may have installed HC
        // between opens.
        availability = runCatching { FitSyncManager.availability(ctx) }
            .getOrDefault(FitnessDataRepository.Availability.UNAVAILABLE)
        if (availability == FitnessDataRepository.Availability.AVAILABLE) {
            if (!FitSyncManager.hasAllPermissions(ctx)) grantedAll = false
            kickoff()
        }
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
                text = "SYNC WITH HEALTH",
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
                text = "importing biometrics from the Health app",
                color = palette.faint,
                fontSize = 10.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(Modifier.height(18.dp))

            when {
                availability == FitnessDataRepository.Availability.UNAVAILABLE -> {
                    HealthConnectUnavailablePanel(
                        needsUpdate = false,
                        titleFont = titleFont,
                        contentFont = contentFont,
                        palette = palette
                    )
                }
                availability == FitnessDataRepository.Availability.NEEDS_UPDATE -> {
                    HealthConnectUnavailablePanel(
                        needsUpdate = true,
                        titleFont = titleFont,
                        contentFont = contentFont,
                        palette = palette
                    )
                }
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

            // Branch 1: HC missing → single CTA to Play Store.
            if (availability != FitnessDataRepository.Availability.AVAILABLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PanelButton(
                        label = if (availability == FitnessDataRepository.Availability.NEEDS_UPDATE)
                            "UPDATE HEALTH APP" else "INSTALL HEALTH APP",
                        accent = true,
                        titleFont = titleFont,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    ) {
                        Haptics.click(ctx)
                        openPlayStoreForHealthConnect()
                    }
                    PanelButton(
                        label = "CLOSE",
                        accent = false,
                        titleFont = titleFont,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    ) { Haptics.click(ctx); onClose() }
                }
            } else if (!running && result != null) {
                // Primary row — Grant / Re-sync + Close
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
                            // Eager-init the HC client first — some HC versions silently
                            // ignore the permission intent otherwise. Also give the user
                            // immediate feedback that the tap registered.
                            FitSyncManager.ensureInitialized(ctx)
                            Toast.makeText(ctx, "Opening Health Connect…", Toast.LENGTH_SHORT).show()
                            val launched = try {
                                onRequestPermissions(); true
                            } catch (_: Throwable) { false }
                            // Belt-and-suspenders: after a brief delay, if nothing happened
                            // (user is still on this screen) they can tap OPEN HEALTH CONNECT
                            // below as a manual escape hatch — no automatic fallback here to
                            // avoid double-launching the sheet.
                            if (!launched) FitSyncManager.openHealthConnectSettings(ctx)
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

                // Secondary row — direct Health app navigation. Guaranteed escape hatch when
                // the Jetpack permission launcher no-ops on a given device/HC version combo.
                Spacer(Modifier.height(8.dp))
                PanelButton(
                    label = "OPEN HEALTH APP",
                    accent = false,
                    titleFont = titleFont,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Haptics.click(ctx)
                    if (!FitSyncManager.openHealthConnectSettings(ctx)) {
                        Toast.makeText(ctx, "Can't reach the Health app — install it from the Play Store.", Toast.LENGTH_LONG).show()
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "If GRANT PERMISSIONS doesn't open the sheet, tap OPEN HEALTH APP and grant access to Unsigned manually under Permissions.",
                    color = palette.faint,
                    fontSize = 10.sp,
                    fontFamily = contentFont,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 14.sp
                )
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

/**
 * Shown instead of the sync progress when the Health Connect provider is missing (not
 * installed on Android < 14 by default) or needs an update. The user needs to go to Play
 * Store first — the HC permission launcher is a no-op otherwise.
 */
@Composable
private fun HealthConnectUnavailablePanel(
    needsUpdate: Boolean,
    titleFont: androidx.compose.ui.text.font.FontFamily,
    contentFont: androidx.compose.ui.text.font.FontFamily,
    palette: ThemePalette
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.danger.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (needsUpdate) "⚠  HEALTH APP NEEDS UPDATE" else "⚠  HEALTH APP NOT INSTALLED",
            color = palette.danger,
            fontSize = 11.sp,
            fontFamily = titleFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = if (needsUpdate) {
                "The Health app on this device is out of date. Update it from the Play " +
                    "Store, then return here and re-open Sync With Health."
            } else {
                "This device doesn't have the Google Health app installed yet. On Android 13 " +
                    "and older it's an optional download — install it from the Play Store, " +
                    "then return here."
            },
            color = palette.onSurface.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontFamily = contentFont,
            lineHeight = 17.sp
        )
    }
}
