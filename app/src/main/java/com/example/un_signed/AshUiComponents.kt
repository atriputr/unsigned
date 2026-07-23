package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassDialogContent(
    titleFont: FontFamily,
    buttonFont: FontFamily,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)) // 70% Translucent
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
                .clickable(enabled = false) { }, // Prevent close when clicking inside
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "IDEAL OPTIONS",
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f)
                )
            )

            GlassButton("1. EDUCATION", buttonFont)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("2. HEALTH", buttonFont)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("3. SKILL", buttonFont)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("4. PEACE", buttonFont)
        }
    }
}

@Composable
fun GlassButton(text: String, fontFamily: FontFamily) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { /* Action */ },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontFamily = fontFamily,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 24.dp)
        )
    }
}
