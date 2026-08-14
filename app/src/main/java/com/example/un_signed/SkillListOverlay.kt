package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reusable list overlay for skill categories (hobby / minor / major). */
@Composable
fun SkillListOverlay(
    category: String,    // "hobby" | "minor" | "major"
    titleFont: FontFamily,
    contentFont: FontFamily,
    onClose: () -> Unit
) {
    val title = when (category) {
        "hobby" -> "HOBBIES"
        "minor" -> "MINOR SKILLS"
        "major" -> "MAJOR SKILLS"
        else -> category.uppercase()
    }
    val subtitle = when (category) {
        "hobby" -> "Things you do for joy"
        "minor" -> "Skills to grow gradually"
        "major" -> "Skills for life mastery"
        else -> ""
    }
    val accent = when (category) {
        "hobby" -> Color(0xFFF4A2FF)
        "minor" -> Color(0xFF9DE8FF)
        "major" -> Color(0xFFFFD948)
        else -> OrangeFire
    }

    var allItems by remember { mutableStateOf(FitDataRepository.loadSkillItems()) }
    var newName by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var sessionMinutes by remember { mutableStateOf("") }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    val myItems = allItems.filter { it.category == category }
        .sortedByDescending { it.totalMinutes }

    fun addItem() {
        val n = newName.trim()
        if (n.isBlank()) return
        val newList = allItems + SkillItem(category = category, name = n)
        allItems = newList
        FitDataRepository.saveSkillItems(newList)
        newName = ""
    }

    fun logSession(item: SkillItem, mins: Int) {
        if (mins <= 0) return
        val session = SkillSession(durationMinutes = mins)
        val updated = item.copy(
            totalMinutes = item.totalMinutes + mins,
            sessions = item.sessions + session
        )
        val newList = allItems.map { if (it.id == item.id) updated else it }
        allItems = newList
        FitDataRepository.saveSkillItems(newList)
    }

    fun deleteItem(item: SkillItem) {
        val newList = allItems.filter { it.id != item.id }
        allItems = newList
        FitDataRepository.saveSkillItems(newList)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1A28).copy(alpha = 0.96f), Color(0xFF0B0B14).copy(alpha = 0.96f))))
                .border(1.5.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent, accent.copy(alpha = 0.25f))), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = accent, blurRadius = 12f))
            )
            Text(
                subtitle,
                color = accent.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Add new skill
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = contentFont),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (newName.isEmpty()) Text("Add ${category}…", color = Color.White.copy(alpha = 0.3f), fontSize = 15.sp, fontFamily = contentFont)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.85f))
                        .clickable { addItem() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("${myItems.size} tracked · total ${myItems.sumOf { it.totalMinutes }} min", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = contentFont)
            Spacer(Modifier.height(6.dp))

            if (myItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("no ${category}s yet", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(myItems, key = { it.id }) { item ->
                        val expanded = expandedId == item.id
                        val confirmDelete = deleteConfirmId == item.id
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (expanded) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (expanded) accent.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .pointerInput(item.id) {
                                    detectTapGestures(
                                        onTap = {
                                            expandedId = if (expanded) null else item.id
                                            if (deleteConfirmId != item.id) deleteConfirmId = null
                                        },
                                        onLongPress = { deleteConfirmId = item.id }
                                    )
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, color = Color.White, fontSize = 16.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold)
                                    Text("${item.totalMinutes} min · ${item.sessions.size} sessions", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = contentFont)
                                }
                                Text(
                                    formatDuration(item.totalMinutes),
                                    color = accent,
                                    fontSize = 15.sp,
                                    fontFamily = NokiaFont,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (expanded && !confirmDelete) {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(15, 30, 45, 60).forEach { m ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(accent.copy(alpha = 0.25f))
                                                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .clickable { logSession(item, m) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+${m}m", color = Color.White, fontSize = 12.sp, fontFamily = BebasFont, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicTextField(
                                        value = sessionMinutes,
                                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) sessionMinutes = it },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = contentFont, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp),
                                        decorationBox = { inner ->
                                            Box(contentAlignment = Alignment.Center) {
                                                if (sessionMinutes.isEmpty()) Text("min", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp, fontFamily = contentFont)
                                                inner()
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accent.copy(alpha = 0.85f))
                                            .clickable {
                                                sessionMinutes.toIntOrNull()?.let { logSession(item, it); sessionMinutes = "" }
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("LOG", color = Color.Black, fontSize = 12.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                                    }
                                }
                                Text("long-press card to delete", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp))
                            }
                            if (confirmDelete) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF6B1010))
                                            .border(1.dp, Color(0xFFFF3030), RoundedCornerShape(8.dp))
                                            .clickable { deleteItem(item); deleteConfirmId = null; expandedId = null },
                                        contentAlignment = Alignment.Center
                                    ) { Text("DELETE", color = Color.White, fontSize = 12.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .clickable { deleteConfirmId = null },
                                        contentAlignment = Alignment.Center
                                    ) { Text("CANCEL", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = titleFont, letterSpacing = 2.sp) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("CLOSE", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.clickable { onClose() })
        }
    }
}

private fun formatDuration(mins: Int): String {
    if (mins < 60) return "${mins}m"
    val h = mins / 60
    val m = mins % 60
    return if (m == 0) "${h}h" else "${h}h${m}m"
}
