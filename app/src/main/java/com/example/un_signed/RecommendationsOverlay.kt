package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecommendationsOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    profile: UserProfile,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val recs = remember { RecommendationEngine.analyse(profile) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.borderBrush(), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "TIPS FOR YOU",
                color = palette.onSurface,
                fontSize = 22.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp,
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.4f), blurRadius = 12f))
            )
            Text(
                "Personalised food + habit picks · based on your routine",
                color = palette.subtle,
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            if (recs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "log more data to get personalised tips",
                        color = palette.faint,
                        fontSize = 13.sp,
                        fontFamily = contentFont,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recs.forEach { r ->
                        RecommendationCard(r, palette, titleFont, contentFont)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "CLOSE",
                color = palette.faint,
                fontSize = 14.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    r: Recommendation,
    palette: ThemePalette,
    titleFont: FontFamily,
    contentFont: FontFamily
) {
    val accent = when (r.priority) {
        Recommendation.Priority.High -> Color(0xFFFF6666)
        Recommendation.Priority.Medium -> Color(0xFFFFC848)
        Recommendation.Priority.Low -> Color(0xFF6ACBEA)
    }
    val catColor = when (r.category) {
        "FOOD" -> Color(0xFF8CD86A)
        "HABIT" -> Color(0xFFB19CFF)
        "LIFESTYLE" -> palette.accentPrimary
        "WARNING" -> palette.danger
        else -> palette.subtle
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.chipBg)
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Priority dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                r.category,
                color = catColor,
                fontSize = 10.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        Text(
            r.title,
            color = palette.onSurface,
            fontSize = 15.sp,
            fontFamily = contentFont,
            fontWeight = FontWeight.Bold,
            lineHeight = 20.sp
        )
        Text(
            "why · ${r.why}",
            color = palette.subtle,
            fontSize = 11.sp,
            fontFamily = contentFont,
            lineHeight = 15.sp
        )
        Text(
            "do · ${r.action}",
            color = palette.onSurface,
            fontSize = 12.sp,
            fontFamily = contentFont,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.sp
        )
        Text(
            "src · ${r.evidence}",
            color = palette.faint,
            fontSize = 9.sp,
            fontFamily = contentFont,
            fontStyle = FontStyle.Italic
        )
    }
}
