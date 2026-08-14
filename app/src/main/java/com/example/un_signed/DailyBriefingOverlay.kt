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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun DailyBriefingOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    profile: UserProfile,
    prefs: AppPreferences = AppPreferences(),
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var weather by remember { mutableStateOf(FitDataRepository.loadWeatherCache()) }
    var snapshot by remember { mutableStateOf(InsightsEngine.snapshot(profile)) }

    LaunchedEffect(Unit) {
        if (!weather.isValid || weather.isStale()) {
            scope.launch { weather = WeatherService.getWeather(ctx) }
        }
    }
    // Recompute snapshot whenever weather resolves (may shift water goal display)
    LaunchedEffect(weather.fetchedAt) { snapshot = InsightsEngine.snapshot(profile) }

    val nudges = InsightsEngine.nudges(snapshot, profile, weather)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF16121F).copy(alpha = 0.98f), Color(0xFF07060C).copy(alpha = 0.98f))))
                .border(1.5.dp, Brush.verticalGradient(listOf(OrangeFire.copy(alpha = 0.55f), Color.Transparent, OrangeFire.copy(alpha = 0.25f))), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DAILY BRIEFING",
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                letterSpacing = 3.sp,
                style = TextStyle(shadow = Shadow(color = OrangeFire, blurRadius = 12f))
            )
            Text(
                snapshot.date.toString(),
                color = OrangeFire.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 2.sp
            )
            if (weather.isValid) {
                Text(
                    "${Units.displayTemp(weather.temperatureC, prefs.tempUnit)} · ${weather.condition}${if (weather.locationName.isNotBlank()) " · ${weather.locationName}" else ""}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = contentFont
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metrics grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        label = "WATER",
                        value = "%d%%".format((snapshot.waterPercent * 100).toInt().coerceAtMost(200)),
                        detail = Units.displayVolume(snapshot.waterConsumedMl, prefs.volumeUnit) + " / " + Units.displayVolume(snapshot.waterGoalMl, prefs.volumeUnit),
                        accent = Color(0xFF4EA8DE),
                        streak = snapshot.streakWater,
                        font = contentFont,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "EXERCISE",
                        value = "${snapshot.exerciseWeekMin}m",
                        detail = "of 150 wk",
                        accent = Color(0xFF8CD86A),
                        streak = snapshot.streakExercise,
                        font = contentFont,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(
                        label = "SLEEP",
                        value = "%.1fh".format(snapshot.sleepLastHours),
                        detail = "target %.1fh".format(snapshot.sleepTargetHours),
                        accent = Color(0xFFB19CFF),
                        streak = snapshot.streakSleep,
                        font = contentFont,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "JUNK",
                        value = "${snapshot.junkToday}",
                        detail = "avg %.1f/day".format(snapshot.junkWeekAvg),
                        accent = if (snapshot.junkToday == 0) Color(0xFF8CD86A) else Color(0xFFFF9B44),
                        streak = snapshot.streakJunkClean,
                        streakLabel = "clean",
                        font = contentFont,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Nudges
                Text("BRIEFING", color = OrangeFire, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                nudges.forEach { line ->
                    Text(
                        "• $line",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontFamily = contentFont,
                        lineHeight = 18.sp
                    )
                }

                // Health metrics
                Spacer(Modifier.height(6.dp))
                if (profile.bmi > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoTile("BMI", "%.1f".format(profile.bmi), profile.bmiCategory, contentFont, Modifier.weight(1f))
                        InfoTile("TDEE", "%.0f".format(profile.tdee), "kcal/day", contentFont, Modifier.weight(1f))
                    }
                }

                // ── vs GLOBAL mini section ────────────────────
                Spacer(Modifier.height(8.dp))
                Text("VS GLOBAL", color = Color(0xFF6ACBEA), fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                val sections = remember(profile, prefs, snapshot) { ComparisonEngine.buildAll(profile, prefs) }
                val topRows = sections.flatMap { it.comparisons }.take(5)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    topRows.forEach { c ->
                        val bandColor = when (c.band) {
                            GlobalNorms.Band.Excellent -> Color(0xFF8CD86A)
                            GlobalNorms.Band.Healthy   -> Color(0xFF6FC1FF)
                            GlobalNorms.Band.Concern   -> Color(0xFFFFC848)
                            GlobalNorms.Band.AtRisk    -> Color(0xFFFF6666)
                            else                       -> Color.White.copy(alpha = 0.4f)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.metric, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontFamily = contentFont, modifier = Modifier.weight(1f))
                            Text(c.userValueDisplay, color = Color.White, fontSize = 12.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bandColor.copy(alpha = 0.22f))
                                    .border(1.dp, bandColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(c.bandLabel.uppercase(), color = bandColor, fontSize = 9.sp, fontFamily = titleFont, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun MetricCard(
    label: String,
    value: String,
    detail: String,
    accent: Color,
    streak: Int,
    streakLabel: String = "streak",
    font: FontFamily,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(label, color = accent, fontSize = 10.sp, fontFamily = BebasFont, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 24.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
        Text(detail, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontFamily = font)
        if (streak >= 2) {
            Text("🔥 $streak d $streakLabel", color = Color(0xFFFFC848), fontSize = 10.sp, fontFamily = font, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, detail: String, font: FontFamily, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp, fontFamily = BebasFont, letterSpacing = 2.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold)
        Text(detail, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp, fontFamily = font)
    }
}
