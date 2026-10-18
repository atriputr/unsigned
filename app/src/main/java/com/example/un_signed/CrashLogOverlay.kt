package com.example.un_signed

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import java.io.File

/**
 * In-app crash log viewer. Reads `filesDir/crash.log` (populated by the uncaught-exception
 * handler installed in ProfileSelectionActivity.installCrashLogger) and renders it in a
 * scrollable monospace block. Buttons: SHARE (sends via ACTION_SEND), CLEAR, CLOSE.
 *
 * Purpose: when the user hits a crash on their physical device, they can:
 *   1. Reopen the app → Settings → VIEW LAST CRASH
 *   2. Read the trace / screenshot / share it
 *   3. Pass it back so the exact failing line is pinpointable instead of guessing.
 */
@Composable
fun CrashLogOverlay(onClose: () -> Unit) {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    val titleFont = titleFontFor(palette.name)
    val contentFont = contentFontFor(palette.name)

    val crashFile = remember { File(ctx.filesDir, "crash.log") }
    var contents by remember {
        mutableStateOf(
            runCatching { if (crashFile.exists()) crashFile.readText() else "" }.getOrDefault("")
        )
    }

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
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.prismBorderBrush(), RoundedCornerShape(22.dp))
                .clickable(enabled = false) { }
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LAST CRASH LOG",
                color = palette.onSurface,
                fontSize = 20.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp,
                style = TextStyle(
                    shadow = Shadow(color = palette.danger.copy(alpha = 0.35f), blurRadius = 10f)
                )
            )
            Text(
                text = if (contents.isBlank()) "no crash recorded since install" else "most recent uncaught exception",
                color = palette.faint,
                fontSize = 10.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // ── Trace body ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.chipBg)
                    .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                if (contents.isBlank()) {
                    Text(
                        text = "no crash on record — the app has been running clean since install.\n\n" +
                            "if a crash happens next, come back here. the stack trace will show up with the exact line + file.",
                        color = palette.subtle,
                        fontSize = 12.sp,
                        fontFamily = contentFont,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 17.sp
                    )
                } else {
                    Text(
                        text = contents,
                        color = palette.onSurface.copy(alpha = 0.95f),
                        fontSize = 10.sp,
                        fontFamily = NokiaFont,
                        letterSpacing = 0.5.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Buttons ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (contents.isNotBlank()) {
                    CrashButton(
                        label = "SHARE",
                        accent = true,
                        titleFont = titleFont,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    ) {
                        Haptics.click(ctx)
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Unsigned crash log")
                                putExtra(Intent.EXTRA_TEXT, contents)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Share crash log")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Throwable) {
                            Toast.makeText(ctx, "Share failed — screenshot this screen instead.", Toast.LENGTH_LONG).show()
                        }
                    }
                    CrashButton(
                        label = "CLEAR",
                        accent = false,
                        titleFont = titleFont,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    ) {
                        Haptics.tick(ctx)
                        runCatching { crashFile.delete() }
                        contents = ""
                    }
                }
                CrashButton(
                    label = "CLOSE",
                    accent = false,
                    titleFont = titleFont,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                ) {
                    Haptics.click(ctx); onClose()
                }
            }
        }
    }
}

@Composable
private fun CrashButton(
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
            .background(if (accent) palette.danger.copy(alpha = 0.85f) else palette.chipBg)
            .border(1.dp, if (accent) palette.danger else palette.fieldBorder, RoundedCornerShape(10.dp))
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
