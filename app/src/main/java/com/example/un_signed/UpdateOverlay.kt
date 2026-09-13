package com.example.un_signed

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateOverlay(
    info: UpdateInfo,
    onUpdate: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val strings = LocalStrings.current
    val titleFont = titleFontFor(palette.name)
    val contentFont = contentFontFor(palette.name)

    // Pulsing indicator on the CTA — subtle, once-per-second
    val infiniteTransition = rememberInfiniteTransition(label = "cta-pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    // Current vs new version
    val currentVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
    }

    // Break the release-note prose into bullets if it looks like a list
    val bulletLines = remember(info.releaseNotes) {
        val raw = info.releaseNotes.trim()
        // Strip leading "v4.3:" style prefix so it doesn't repeat the header
        val cleaned = Regex("^v?\\d+(\\.\\d+)*[:：]\\s*").replaceFirst(raw, "")
        cleaned.split(Regex("[.•·]\\s+"))
            .map { it.trim().trimEnd(',', '.', '·', '•') }
            .filter { it.isNotBlank() }
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
                .width(340.dp)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.prismBorderBrush(), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header row: title + close (×) ──────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(24.dp)) // balances the × on the right
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${strings.system} ${strings.edit}",
                        color = palette.onSurface,
                        fontSize = 22.sp,
                        fontFamily = titleFont,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = palette.accentPrimary.copy(alpha = 0.35f),
                                blurRadius = 12f
                            )
                        )
                    )
                    Text(
                        text = "an over-the-air refresh awaits",
                        color = palette.faint,
                        fontSize = 10.sp,
                        fontFamily = contentFont,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.chipBg)
                        .border(1.dp, palette.fieldBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            Haptics.click(context)
                            onClose()
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

            Spacer(Modifier.height(18.dp))

            // ── Version pill: current → new ─────────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(palette.chipBg)
                    .border(1.dp, palette.accentPrimary.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentVersion.isNotBlank()) {
                    Text(
                        text = "v$currentVersion",
                        color = palette.subtle,
                        fontSize = 12.sp,
                        fontFamily = NokiaFont,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "  →  ",
                        color = palette.faint,
                        fontSize = 13.sp,
                        fontFamily = contentFont
                    )
                }
                Text(
                    text = "v${info.versionName}",
                    color = palette.accentPrimary,
                    fontSize = 14.sp,
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

            Spacer(Modifier.height(14.dp))

            // ── Release notes ───────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.chipBg.copy(alpha = 0.55f))
                    .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "WHAT'S NEW",
                    color = palette.accentSecondary,
                    fontSize = 9.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
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

            Spacer(Modifier.height(20.dp))

            // ── CTA: install / update ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.success)
                    .border(1.dp, palette.success.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .clickable {
                        Haptics.click(context)
                        onUpdate()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing dot in front of the label
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(pulse)
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.danger)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = strings.checkUpdate,
                        color = Color.Black,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = titleFont,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Back / dismiss (subtle) ─────────────────────────
            Text(
                text = strings.back,
                color = palette.faint,
                fontSize = 13.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .padding(4.dp)
                    .clickable {
                        Haptics.click(context)
                        onClose()
                    }
            )
        }
    }
}
