package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun GlassDateTimePicker(
    title: String,
    titleFont: FontFamily,
    contentFont: FontFamily,
    onNext: (LocalDateTime) -> Unit,
    onClose: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val now = LocalTime.now()
    var selectedHour by remember { mutableStateOf(now.hour % 12.let { if (it == 0) 12 else it }) }
    var selectedMinute by remember { mutableStateOf(now.minute) }
    var isAm by remember { mutableStateOf(now.hour < 12) }
    
    var displayYear by remember { mutableStateOf(selectedDate.year) }
    var displayMonth by remember { mutableStateOf(selectedDate.monthValue) }

    val yearMonth = YearMonth.of(displayYear, displayMonth)
    val firstDayOffset = yearMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = yearMonth.lengthOfMonth()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(350.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))))
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 22.sp, fontFamily = titleFont)
            Spacer(modifier = Modifier.height(16.dp))

            // Calendar Mini View Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${yearMonth.month.name} $displayYear", color = Color.White, fontSize = 26.sp, fontFamily = contentFont)
                Row {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { 
                        if (displayMonth == 1) { displayMonth = 12; displayYear-- } else displayMonth--
                    }, contentAlignment = Alignment.Center) { Text("<", color = Color.White, fontFamily = contentFont) }
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { 
                        if (displayMonth == 12) { displayMonth = 1; displayYear++ } else displayMonth++
                    }, contentAlignment = Alignment.Center) { Text(">", color = Color.White, fontFamily = contentFont) }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Day Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = contentFont)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Full Grid
            Column {
                for (row in 0 until 6) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIdx = row * 7 + col
                            val day = if (cellIdx >= firstDayOffset && cellIdx < daysInMonth + firstDayOffset) {
                                cellIdx - firstDayOffset + 1
                            } else null
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (day != null && selectedDate.dayOfMonth == day && selectedDate.monthValue == displayMonth && selectedDate.year == displayYear)
                                            Color.White.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = day != null) { 
                                        if (day != null) selectedDate = LocalDate.of(displayYear, displayMonth, day)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isSelected = selectedDate.dayOfMonth == day && selectedDate.monthValue == displayMonth && selectedDate.year == displayYear
                                    Text(
                                        text = day.toString(),
                                        color = if (isSelected) Color(0xFFEBC174) else Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = contentFont
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontFamily = contentFont)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // Time Picker (12-hour format)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                TimeScroller("HOUR", selectedHour, 1..12, contentFont) { selectedHour = it }
                Spacer(Modifier.width(10.dp))
                Text(":", color = Color.White, fontSize = 24.sp, fontFamily = contentFont)
                Spacer(Modifier.width(10.dp))
                TimeScroller("MIN", selectedMinute, 0..59, contentFont) { selectedMinute = it }
                Spacer(Modifier.width(16.dp))
                
                // AM/PM Toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("P/A", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontFamily = contentFont)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { isAm = !isAm }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(if (isAm) "AM" else "PM", color = Color(0xFFEBC174), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = contentFont)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Next Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE41417))
                    .clickable { 
                        val hour24 = when {
                            isAm && selectedHour == 12 -> 0
                            isAm -> selectedHour
                            !isAm && selectedHour == 12 -> 12
                            else -> selectedHour + 12
                        }
                        onNext(LocalDateTime.of(selectedDate, LocalTime.of(hour24, selectedMinute)))
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("NEXT", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = titleFont)
            }
        }
    }
}

@Composable
fun TimeScroller(label: String, value: Int, range: IntRange, font: FontFamily, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontFamily = font)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { 
                var newVal = value - 1
                if (newVal < range.first) newVal = range.last
                onValueChange(newVal)
            }.padding(8.dp)) {
                Text("-", color = Color.White, fontSize = 20.sp, fontFamily = font)
            }
            Text(value.toString().padStart(2, '0'), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
            Box(Modifier.clickable { 
                var newVal = value + 1
                if (newVal > range.last) newVal = range.first
                onValueChange(newVal)
            }.padding(8.dp)) {
                Text("+", color = Color.White, fontSize = 20.sp, fontFamily = font)
            }
        }
    }
}
