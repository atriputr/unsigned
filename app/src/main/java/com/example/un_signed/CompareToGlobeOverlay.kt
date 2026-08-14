package com.example.un_signed

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompareToGlobeOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    profile: UserProfile,
    prefs: AppPreferences,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val sections = remember { ComparisonEngine.buildAll(profile, prefs) }

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
                "COMPARE ME TO GLOBE",
                color = palette.onSurface,
                fontSize = 22.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp,
                style = TextStyle(shadow = Shadow(color = Color(0xFF6ACBEA), blurRadius = 14f))
            )
            Text(
                "Context, not judgement · WHO / CDC / NSF benchmarks",
                color = Color(0xFF6ACBEA).copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                sections.forEach { section ->
                    SectionCard(section, titleFont, contentFont)
                }

                // Sources
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.chipBg)
                        .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("PRIMARY SOURCES", color = palette.accentSecondary, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    GlobalNorms.sources.forEach {
                        Text("· $it", color = palette.faint, fontSize = 10.sp, fontFamily = contentFont, lineHeight = 14.sp)
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
private fun SectionCard(section: ComparisonSection, titleFont: FontFamily, contentFont: FontFamily) {
    val palette = LocalPalette.current
    val accentColor = Color(section.accentHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.chipBg)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            section.title,
            color = accentColor,
            fontSize = 12.sp,
            fontFamily = titleFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp
        )
        section.comparisons.forEach { c ->
            ComparisonRow(c, accentColor, contentFont, titleFont)
        }
    }
}

@Composable
private fun ComparisonRow(c: Comparison, accent: Color, font: FontFamily, titleFont: FontFamily) {
    val palette = LocalPalette.current
    val bandColor = when (c.band) {
        GlobalNorms.Band.Excellent -> Color(0xFF3EA94E)
        GlobalNorms.Band.Healthy   -> Color(0xFF3E85C1)
        GlobalNorms.Band.Concern   -> Color(0xFFD8A11A)
        GlobalNorms.Band.AtRisk    -> palette.danger
        else                       -> palette.faint
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (palette.isLight) palette.chipBg else Color.Black.copy(alpha = 0.35f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Metric name + band pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                c.metric,
                color = palette.onSurface,
                fontSize = 13.sp,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(bandColor.copy(alpha = 0.22f))
                    .border(1.dp, bandColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(c.bandLabel.uppercase(), color = bandColor, fontSize = 9.sp, fontFamily = titleFont, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Big value + reference
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                c.userValueDisplay,
                color = palette.onSurface,
                fontSize = 24.sp,
                fontFamily = NokiaFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.width(8.dp))
            Text("·", color = palette.faint, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                c.referenceDisplay,
                color = palette.subtle,
                fontSize = 10.sp,
                fontFamily = font,
                modifier = Modifier.weight(1f)
            )
        }

        // Band gradient bar
        BandBar(c.band)

        // Percentile line if any
        c.percentile?.let { p ->
            Text(
                "≈ ${p}th percentile · ${c.percentileLabel ?: ""}",
                color = accent.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Insight
        if (c.insight.isNotBlank()) {
            Text(c.insight, color = palette.subtle, fontSize = 11.sp, fontFamily = font, lineHeight = 15.sp)
        }
        // Source
        Text("src: ${c.source}", color = palette.faint, fontSize = 9.sp, fontFamily = font, fontStyle = FontStyle.Italic)
    }
}

@Composable
private fun BandBar(band: GlobalNorms.Band) {
    // 4-segment horizontal bar (AtRisk → Concern → Healthy → Excellent)
    // Current band highlighted; others dimmed.
    val segments = listOf(
        GlobalNorms.Band.AtRisk   to Color(0xFFFF6666),
        GlobalNorms.Band.Concern  to Color(0xFFFFC848),
        GlobalNorms.Band.Healthy  to Color(0xFF6FC1FF),
        GlobalNorms.Band.Excellent to Color(0xFF8CD86A)
    )
    Row(modifier = Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        segments.forEach { (b, color) ->
            val active = b == band
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                drawRoundRect(
                    color = if (active) color else color.copy(alpha = 0.20f),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(4f, 4f),
                    topLeft = Offset(0f, 0f)
                )
                if (active) {
                    drawRoundRect(
                        color = color.copy(alpha = 0.35f),
                        size = Size(size.width, size.height + 4f),
                        cornerRadius = CornerRadius(4f, 4f),
                        topLeft = Offset(0f, -2f)
                    )
                }
            }
        }
    }
}
