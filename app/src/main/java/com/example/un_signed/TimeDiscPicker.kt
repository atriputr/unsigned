package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun TimeDiscPicker(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onConfirm: (hour: Int, minute: Int, isAm: Boolean, repeat: Boolean, repeatInfo: String) -> Unit,
    onDismiss: () -> Unit
) {
    val now = Calendar.getInstance()
    var hours by remember { mutableStateOf(now.get(Calendar.HOUR) % 12) }
    var minutes by remember { mutableStateOf(now.get(Calendar.MINUTE)) }
    var isAm by remember { mutableStateOf(now.get(Calendar.AM_PM) == Calendar.AM) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showRepeatOptions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(enabled = false) {}
        ) {
            DualHandleTimeDial(
                hours = hours,
                minutes = minutes,
                onValueChange = { h, m -> hours = h; minutes = m },
                dialSize = 280.dp,
                contentFont = contentFont
            )

            Spacer(Modifier.height(28.dp))

            // AM/PM Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAm) Color(0xFFFF9500) else Color.White.copy(alpha = 0.1f))
                        .clickable { isAm = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("AM", color = if (isAm) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontFamily = contentFont)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isAm) Color(0xFFFF9500) else Color.White.copy(alpha = 0.1f))
                        .clickable { isAm = false }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("PM", color = if (!isAm) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontFamily = contentFont)
                }
            }

            Spacer(Modifier.height(20.dp))

            val displayHour = if (hours == 0) 12 else hours
            Text(
                text = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minutes, if (isAm) "AM" else "PM"),
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = contentFont,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFF9500))
                    .clickable { onConfirm(hours, minutes, isAm, false, "") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SET",
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "SET WITH REPEAT →",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontFamily = titleFont,
                modifier = Modifier.clickable { showRepeatOptions = true }
            )
        }

        if (showRepeatDialog) {
            AlertDialog(
                onDismissRequest = { showRepeatDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("REPEAT", color = Color.White, fontFamily = titleFont, fontSize = 24.sp)
                    }
                },
                confirmButton = {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF09e8ad))
                            .clickable { 
                                showRepeatDialog = false
                                showRepeatOptions = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE41417))
                            .clickable { 
                                onConfirm(hours, minutes, isAm, false, "")
                                showRepeatDialog = false 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showRepeatOptions) {
            RepeatOptionsOverlay(
                titleFont = titleFont,
                onSave = { info, _ ->
                    onConfirm(hours, minutes, isAm, true, info)
                    showRepeatOptions = false
                },
                onClose = { showRepeatOptions = false }
            )
        }
    }
}
