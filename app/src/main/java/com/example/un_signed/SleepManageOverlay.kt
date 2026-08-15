package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * Popup shown when user long-holds the SLEEP quick-button for 8 seconds.
 * Lets them delete today's recorded sleep entries + discard an in-progress session.
 */
@Composable
fun SleepManageOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    activeSession: SleepSessionState,
    onDiscardActive: () -> Unit,
    onDeleteEntry: (SleepEntry) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val today = remember { LocalDate.now().toString() }
    var allEntries by remember { mutableStateOf(FitDataRepository.loadSleepEntries()) }
    val todayEntries = allEntries.filter { it.wakeDateIso == today }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 540.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.borderBrush(), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "MANAGE SLEEP",
                color = palette.onSurface,
                fontSize = 22.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp,
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
            )
            Text(
                today,
                color = palette.subtle,
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Active session banner
            if (activeSession.active) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.chipBg)
                        .border(1.dp, palette.accentPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("IN-PROGRESS SESSION", color = palette.accentPrimary, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    val hrs = (System.currentTimeMillis() - activeSession.startedAtEpochMs) / 3_600_000.0
                    Text("Running · %.1fh · %d disturbances".format(hrs, activeSession.disturbanceCount),
                        color = palette.onSurface, fontSize = 13.sp, fontFamily = contentFont)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.danger.copy(alpha = 0.85f))
                            .clickable {
                                onDiscardActive()
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DISCARD SESSION", color = Color.White, fontSize = 12.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Today's recorded entries
            Text("TODAY'S ENTRIES (${todayEntries.size})", color = palette.accentSecondary, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(6.dp))

            if (todayEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("no sleep recorded today", color = palette.faint, fontSize = 13.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(todayEntries) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.chipBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "%.1fh".format(entry.durationHours),
                                    color = palette.accentPrimary,
                                    fontSize = 16.sp,
                                    fontFamily = NokiaFont,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "quality " + "★".repeat(entry.quality.coerceIn(0, 5)) +
                                        if (entry.disturbances > 0) "  ·  ${entry.disturbances} disturbances" else "",
                                    color = palette.subtle,
                                    fontSize = 10.sp,
                                    fontFamily = contentFont
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.danger.copy(alpha = 0.8f))
                                    .clickable {
                                        onDeleteEntry(entry)
                                        allEntries = allEntries.filter { it.id != entry.id }
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("DELETE", color = Color.White, fontSize = 11.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "CLOSE",
                color = palette.faint,
                fontSize = 14.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}
