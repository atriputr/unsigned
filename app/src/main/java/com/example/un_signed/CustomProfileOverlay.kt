package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
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
    onDelete: (CustomProfile) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    var showCreatePage by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<CustomProfile?>(null) }

    GlassCard(
        modifier = Modifier.heightIn(max = 600.dp),
        onClose = onClose
    ) {
        Text(
            text = LocalStrings.current.customProfile,
            color = palette.onSurface,
            fontSize = 24.sp,
            fontFamily = titleFont,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(bottom = 20.dp),
            style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
        )

        // CREATE PROFILE Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.chipBg)
                .border(width = 1.dp, color = palette.fieldBorder, shape = RoundedCornerShape(14.dp))
                .clickable { showCreatePage = true },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = LocalStrings.current.createProfile,
                color = palette.onSurface,
                fontSize = 22.sp,
                fontFamily = titleFont,
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = palette.divider, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            LocalStrings.current.previousOngoingProfiles,
            color = palette.subtle,
            fontSize = 22.sp,
            fontFamily = contentFont,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedProfiles, key = { it.id }) { profile ->
                ProfileListItem(profile, contentFont, onLongPress = { profileToDelete = profile })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = LocalStrings.current.back,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 18.sp,
            fontFamily = titleFont,
            modifier = Modifier.clickable { onClose() }
        )
        if (profileToDelete != null) {
            AlertDialog(
                onDismissRequest = { profileToDelete = null },
                containerColor = Color(0xFF1A1A1A),
                title = { Text(LocalStrings.current.deleteProfile, color = Color.White, fontFamily = titleFont) }, 
                text = { Text(LocalStrings.current.deleteProfileConfirm, color = Color.White.copy(alpha = 0.7f), fontFamily = contentFont) },
                confirmButton = {
                    Text(
                        LocalStrings.current.yes, 
                        color = Color(0xFFE41417), 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp).clickable { 
                            onDelete(profileToDelete!!)
                            profileToDelete = null 
                        }
                    )
                },
                dismissButton = {
                    Text(
                        LocalStrings.current.no, 
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(16.dp).clickable { profileToDelete = null }
                    )
                }
            )
        }
    }
}

@Composable
fun CreateProfileOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
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
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(350.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF0D0D1A))))
                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = LocalStrings.current.newProfile,
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CustomInputField(label = LocalStrings.current.name, value = name, onValueChange = { name = it }, labelFont = titleFont, contentFont = contentFont)
            Spacer(modifier = Modifier.height(12.dp))
            
            // DURATION FIELD (Clickable)
            Box(modifier = Modifier.clickable { showFromPicker = true }) {
                CustomInputField(
                    label = LocalStrings.current.duration, 
                    value = duration, 
                    onValueChange = { duration = it }, 
                    labelFont = titleFont, 
                    contentFont = contentFont,
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // TYPE FIELD (Clickable)
            Box(modifier = Modifier.clickable { showTypeOptions = true }) {
                CustomInputField(
                    label = LocalStrings.current.type, 
                    value = type, 
                    onValueChange = { type = it }, 
                    labelFont = titleFont, 
                    contentFont = contentFont,
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE41417).copy(alpha = 0.8f))
                    .clickable {
                        if (name.isNotBlank()) {
                            onSave(name, duration, type)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(LocalStrings.current.saveProfile, color = Color.White, fontSize = 18.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = LocalStrings.current.back,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontFamily = titleFont,
                modifier = Modifier.clickable { onClose() }
            )
        }

        // --- PICKER OVERLAYS ---
        if (showFromPicker) {
            GlassDateTimePicker(
                title = LocalStrings.current.from,
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
fun ProfileListItem(profile: CustomProfile, font: FontFamily, onLongPress: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
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
