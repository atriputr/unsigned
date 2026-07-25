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
    var habitName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .heightIn(max = 600.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.4f), Color.Transparent, Color.White.copy(alpha = 0.2f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(enabled = false) { }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HABITS",
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 20.dp),
                style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f))
            )

            // Input Area
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("ENTER HABIT", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp, fontFamily = titleFont)
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontFamily = contentFont),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        if (habitName.isEmpty()) {
                            Text("New habit...", color = Color.White.copy(alpha = 0.3f), fontSize = 20.sp, fontFamily = contentFont)
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
                    .background(Color(0xFFE41417).copy(alpha = 0.8f))
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
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "SAVED HABITS:",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 20.sp,
                fontFamily = contentFont,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(savedHabits) { index, habit ->
                    HabitListItem(
                        habit = habit, 
                        font = contentFont,
                        onUpdateCount = { diff -> 
                            onUpdateCount(index, habit.count + diff)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "BACK",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 18.sp,
                fontFamily = titleFont,
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}

@Composable
fun HabitListItem(habit: Habit, font: FontFamily, onUpdateCount: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
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
                color = Color.White.copy(alpha = 0.9f), 
                fontSize = 20.sp, 
                fontFamily = font,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = habit.count.toString(), 
                color = Color(0xFFFFA500), // Orange color for count
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold, 
                fontFamily = font,
                style = TextStyle(shadow = Shadow(color = Color(0xFFFFA500), blurRadius = 10f))
            )
        }
    }
}
