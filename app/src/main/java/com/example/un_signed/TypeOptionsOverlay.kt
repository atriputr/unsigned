package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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

@Composable
fun TypeOptionsOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onSelectionComplete: (String) -> Unit,
    onClose: () -> Unit
) {
    var currentView by remember { mutableStateOf("MAIN") }
    
    // Selection States
    val mainOptions = remember { mutableStateListOf<String>() }
    val remindOptions = remember { mutableStateListOf<String>() }
    val timerOptions = remember { mutableStateListOf<String>() }
    var priorityOption by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))))
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentView) {
                "MAIN" -> {
                    Text("SELECT TYPE", color = Color.White, fontSize = 22.sp, fontFamily = titleFont)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    TypeMainItem(
                        text = "REMIND BY", 
                        checked = "Remind by" in mainOptions, 
                        enabled = remindOptions.isNotEmpty(),
                        font = contentFont, 
                        onToggle = { if (it) mainOptions.add("Remind by") else mainOptions.remove("Remind by") },
                        onArrowClick = { currentView = "REMIND" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TypeMainItem(
                        text = "ADD TIMER BAR", 
                        checked = "Timer bar" in mainOptions, 
                        enabled = timerOptions.isNotEmpty(),
                        font = contentFont, 
                        onToggle = { if (it) mainOptions.add("Timer bar") else mainOptions.remove("Timer bar") },
                        onArrowClick = { currentView = "TIMER" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TypeMainItem(
                        text = "PRIORITY", 
                        checked = "Priority" in mainOptions, 
                        enabled = priorityOption.isNotEmpty(),
                        font = contentFont, 
                        onToggle = { if (it) mainOptions.add("Priority") else mainOptions.remove("Priority") },
                        onArrowClick = { currentView = "PRIORITY" }
                    )

                    Spacer(modifier = Modifier.height(30.dp))
                    DoneButton(titleFont) {
                        val result = mutableListOf<String>()
                        if ("Remind by" in mainOptions && remindOptions.isNotEmpty()) result.add("Remind: ${remindOptions.joinToString(",")}")
                        if ("Timer bar" in mainOptions && timerOptions.isNotEmpty()) result.add("Timer: ${timerOptions.joinToString(",")}")
                        if ("Priority" in mainOptions && priorityOption.isNotEmpty()) result.add("Priority: $priorityOption")
                        onSelectionComplete(result.joinToString(" | "))
                    }
                }

                "REMIND" -> {
                    NestedOptionView(
                        title = "REMIND BY", 
                        options = listOf("5m", "10m", "30m", "45m", "1hr", "-1hr", "-3hr", "-5hr", "-12hr"), 
                        selectedList = remindOptions, 
                        titleFont = titleFont, 
                        contentFont = contentFont, 
                        onBack = { 
                            if (remindOptions.isNotEmpty()) {
                                if ("Remind by" !in mainOptions) mainOptions.add("Remind by")
                            } else {
                                mainOptions.remove("Remind by")
                            }
                            currentView = "MAIN" 
                        }
                    )
                }

                "TIMER" -> {
                    NestedOptionView(
                        title = "TIMER BAR", 
                        options = listOf("on app", "on home", "on lock"), 
                        selectedList = timerOptions, 
                        titleFont = titleFont, 
                        contentFont = contentFont, 
                        onBack = { 
                            if (timerOptions.isNotEmpty()) {
                                if ("Timer bar" !in mainOptions) mainOptions.add("Timer bar")
                            } else {
                                mainOptions.remove("Timer bar")
                            }
                            currentView = "MAIN" 
                        }
                    )
                }

                "PRIORITY" -> {
                    PriorityOptionView(
                        selected = priorityOption, 
                        titleFont = titleFont, 
                        contentFont = contentFont, 
                        onSelect = { priorityOption = it }, 
                        onBack = { 
                            if (priorityOption.isNotEmpty()) {
                                if ("Priority" !in mainOptions) mainOptions.add("Priority")
                            } else {
                                mainOptions.remove("Priority")
                            }
                            currentView = "MAIN" 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TypeMainItem(text: String, checked: Boolean, enabled: Boolean, font: FontFamily, onToggle: (Boolean) -> Unit, onArrowClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onArrowClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFE41417), 
                uncheckedColor = if (enabled) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                disabledUncheckedColor = Color.White.copy(alpha = 0.1f),
                disabledCheckedColor = Color(0xFFE41417).copy(alpha = 0.4f)
            )
        )
        Text(
            text = text, 
            color = Color.White, 
            fontSize = 18.sp, 
            fontFamily = font, 
            modifier = Modifier.weight(1f)
        )
        Text(">", color = Color(0xFFEBC174), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NestedOptionView(title: String, options: List<String>, selectedList: MutableList<String>, titleFont: FontFamily, contentFont: FontFamily, onBack: () -> Unit) {
    Text(title, color = Color.White, fontSize = 22.sp, fontFamily = titleFont)
    Spacer(modifier = Modifier.height(16.dp))
    
    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
        items(options) { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { 
                    if (option in selectedList) selectedList.remove(option) else selectedList.add(option)
                }.padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = option in selectedList,
                    onCheckedChange = { if (it) selectedList.add(option) else selectedList.remove(option) },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEBC174))
                )
                Text(option, color = Color.White, fontSize = 18.sp, fontFamily = contentFont)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(20.dp))
    Box(
        modifier = Modifier.fillMaxWidth().height(45.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Text("BACK", color = Color.White, fontFamily = titleFont)
    }
}

@Composable
fun PriorityOptionView(selected: String, titleFont: FontFamily, contentFont: FontFamily, onSelect: (String) -> Unit, onBack: () -> Unit) {
    Text("PRIORITY", color = Color.White, fontSize = 22.sp, fontFamily = titleFont)
    Spacer(modifier = Modifier.height(20.dp))
    
    val options = listOf("<can repeat>", "<Finalize>")
    options.forEach { option ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .border(2.dp, if (selected == option) Color(0xFFEBC174) else Color.White.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .background(if (selected == option) Color(0xFFEBC174) else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(option, color = Color.White, fontSize = 18.sp, fontFamily = contentFont)
        }
    }

    Spacer(modifier = Modifier.height(30.dp))
    Box(
        modifier = Modifier.fillMaxWidth().height(45.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Text("BACK", color = Color.White, fontFamily = titleFont)
    }
}

@Composable
fun DoneButton(font: FontFamily, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE41417))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("DONE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = font)
    }
}
