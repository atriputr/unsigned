package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun ExportProgressOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .heightIn(max = 650.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))))
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("EXPORT DATA", color = Color.White, fontSize = 24.sp, fontFamily = titleFont)
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    ExportSectionHeader("Currently Active", Color(0xFF09e8ad), titleFont)
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.CenterStart) {
                        Text("No active profiles found", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp, fontFamily = contentFont)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    ExportSectionHeader("Inactive", Color(0xFFEBC174), titleFont)
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.CenterStart) {
                        Text("No inactive profiles", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp, fontFamily = contentFont)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    ExportSectionHeader("Profile Expired", Color(0xFFE41417), titleFont)
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.CenterStart) {
                        Text("No expired profiles", color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp, fontFamily = contentFont)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            
            // "VIEW WHOLE" Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF878b7c))
                    .clickable { /* View Whole Logic */ },
                contentAlignment = Alignment.Center
            ) {
                Text("VIEW WHOLE", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = titleFont)
            }
        }
    }
}

@Composable
fun ExportSectionHeader(title: String, color: Color, font: FontFamily) {
    Column {
        Text(title.uppercase(), color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = font)
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = color.copy(alpha = 0.4f), thickness = 1.5.dp)
    }
}
