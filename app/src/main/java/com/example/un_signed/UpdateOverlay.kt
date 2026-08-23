package com.example.un_signed

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UpdateOverlay(
    info: UpdateInfo,
    onUpdate: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "badgeScale"
    )

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
                .background(Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF0D0D1A))))
                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYSTEM UPDATE",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = BebasFont,
                fontStyle = FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Version ${info.versionName} available.",
                color = Color(0xFFEBC174),
                fontSize = 18.sp,
                fontFamily = BebasFont
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = info.releaseNotes,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = BebasFont,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF09e8ad))
                    .clickable { 
                        Haptics.click(context)
                        onUpdate() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("UPDATE NOW", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = BebasFont)
                
                // NEW Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .scale(badgeScale)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Red)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("NEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = BebasFont)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "LATER",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 16.sp,
                fontFamily = BebasFont,
                modifier = Modifier.clickable { 
                    Haptics.click(context)
                    onClose() 
                }
            )
        }
    }
}
