package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class CustomProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val duration: String,
    val type: String
)

@Composable
fun CustomProfileOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    savedProfiles: List<CustomProfile>,
    onSave: (String, String, String) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }

    var showFromPicker by remember { mutableStateOf(false) }
    var showTillPicker by remember { mutableStateOf(false) }
    var showTypeOptions by remember { mutableStateOf(false) }
    var fromDateTime by remember { mutableStateOf<LocalDateTime?>(null) }

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
                text = "CUSTOM PROFILE",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 20.dp),
                style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f))
            )

            CustomInputField(label = "NAME", value = name, onValueChange = { name = it }, labelFont = titleFont, contentFont = contentFont)
            Spacer(modifier = Modifier.height(12.dp))
            
            // DURATION FIELD (Clickable)
            Box(modifier = Modifier.clickable { showFromPicker = true }) {
                CustomInputField(
                    label = "DURATION", 
                    value = duration, 
                    onValueChange = { duration = it }, 
                    labelFont = titleFont, 
                    contentFont = contentFont,
                    enabled = false // Disable direct typing
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // TYPE FIELD (Clickable)
            Box(modifier = Modifier.clickable { showTypeOptions = true }) {
                CustomInputField(
                    label = "TYPE", 
                    value = type, 
                    onValueChange = { type = it }, 
                    labelFont = titleFont, 
                    contentFont = contentFont,
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE41417).copy(alpha = 0.8f))
                    .clickable {
                        if (name.isNotBlank()) {
                            onSave(name, duration, type)
                            name = ""; duration = ""; type = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("SAVE PROFILE", color = Color.White, fontSize = 18.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "PREVIOUS/ONGOING PROFILES:",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 22.sp,
                fontFamily = contentFont,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedProfiles) { profile ->
                    ProfileListItem(profile, contentFont)
                }
            }
        }

        // --- PICKER OVERLAYS ---
        if (showFromPicker) {
            GlassDateTimePicker(
                title = "FROM",
                titleFont = titleFont,
                contentFont = contentFont,
                onNext = {
                    fromDateTime = it
                    showFromPicker = false
                    showTillPicker = true
                },
                onClose = { showFromPicker = false }
            )
        }

        if (showTillPicker) {
            GlassDateTimePicker(
                title = "TILL",
                titleFont = titleFont,
                contentFont = contentFont,
                onNext = { tillDateTime ->
                    val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
                    duration = "${fromDateTime?.format(formatter)} - ${tillDateTime.format(formatter)}"
                    showTillPicker = false
                },
                onClose = { showTillPicker = false }
            )
        }

        if (showTypeOptions) {
            TypeOptionsOverlay(
                titleFont = titleFont,
                contentFont = contentFont,
                onSelectionComplete = {
                    type = it
                    showTypeOptions = false
                },
                onClose = { showTypeOptions = false }
            )
        }
    }
}

@Composable
fun CustomInputField(label: String, value: String, onValueChange: (String) -> Unit, labelFont: FontFamily, contentFont: FontFamily, enabled: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 20.sp, fontFamily = labelFont)
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontFamily = contentFont),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text("Enter $label...", color = Color.White.copy(alpha = 0.3f), fontSize = 22.sp, fontFamily = contentFont)
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun ProfileListItem(profile: CustomProfile, font: FontFamily) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(profile.name.uppercase(), color = Color(0xFFEBC174), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = font)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DUR: ${profile.duration}", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontFamily = font)
                Text("TYPE: ${profile.type}", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontFamily = font)
            }
        }
    }
}
