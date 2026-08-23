package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val count: Int = 0
)

@Composable
fun HabitOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    savedHabits: MutableList<Habit>,
    onSave: (String) -> Unit,
    onUpdateCount: (Int, Int) -> Unit, // index, newCount
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    var habitName by remember { mutableStateOf("") }

    GlassCard(
        modifier = Modifier.heightIn(max = 600.dp),
        onClose = onClose
    ) {
        Text(
            text = "HABITS",
            color = palette.onSurface,
            fontSize = 26.sp,
            fontFamily = titleFont,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(bottom = 20.dp),
            style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
        )

        // Input Area
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("ENTER HABIT", color = palette.subtle, fontSize = 18.sp, fontFamily = titleFont)
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = habitName,
                onValueChange = { habitName = it },
                textStyle = TextStyle(color = palette.onSurface, fontSize = 20.sp, fontFamily = contentFont),
                cursorBrush = SolidColor(palette.accentPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.fieldBg)
                    .border(1.dp, palette.fieldBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { innerTextField ->
                    if (habitName.isEmpty()) {
                        Text("New habit...", color = palette.faint, fontSize = 20.sp, fontFamily = contentFont)
                    }
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.danger.copy(alpha = 0.85f))
                .clickable {
                    if (habitName.isNotBlank()) {
                        onSave(habitName)
                        habitName = ""
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("SAVE HABIT", color = Color.White, fontSize = 18.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "SAVED HABITS:",
            color = palette.subtle,
            fontSize = 20.sp,
            fontFamily = contentFont,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (savedHabits.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                EmptyState(text = "no habits tracked yet", fontFamily = contentFont)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(savedHabits, key = { _, h -> h.id }) { index, habit ->
                    HabitListItem(
                        habit = habit, 
                        font = contentFont,
                        onUpdateCount = { diff -> 
                            onUpdateCount(index, habit.count + diff)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "BACK",
            color = palette.faint,
            fontSize = 18.sp,
            fontFamily = titleFont,
            modifier = Modifier.clickable { onClose() }
        )
    }
}

@Composable
fun HabitListItem(habit: Habit, font: FontFamily, onUpdateCount: (Int) -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onUpdateCount(-1) },
                    onDoubleTap = { onUpdateCount(1) }
                )
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.name.uppercase(),
                color = palette.onSurface,
                fontSize = 20.sp,
                fontFamily = font,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = habit.count.toString(),
                color = palette.accentPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = font,
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.5f), blurRadius = 10f))
            )
        }
    }
}
