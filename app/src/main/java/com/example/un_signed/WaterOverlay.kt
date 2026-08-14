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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun WaterOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    profile: UserProfile,
    prefs: AppPreferences = AppPreferences(),
    onClose: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var weather      by remember { mutableStateOf(FitDataRepository.loadWeatherCache()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val existing     = remember { FitDataRepository.loadWaterEntry(today) }
    var glasses      by remember { mutableStateOf(existing?.glassesConsumed ?: 0) }

    // Kick off weather fetch if stale or missing
    LaunchedEffect(Unit) {
        if (!weather.isValid || weather.isStale()) {
            isRefreshing = true
            weather = WeatherService.getWeather(ctx)
            isRefreshing = false
        }
    }

    val goalMl = remember(profile, weather) {
        WaterGoal.compute(profile, if (weather.isValid) weather else null)
    }
    val glassMl = 250
    val targetGlasses = ((goalMl + glassMl - 1) / glassMl).coerceAtLeast(1)

    // Persist whenever glasses count changes
    LaunchedEffect(glasses, goalMl) {
        FitDataRepository.saveWaterEntry(
            today,
            WaterDailyLog(
                date = today.toString(),
                glassesConsumed = glasses,
                glassMl = glassMl,
                goalMl = goalMl,
                temperatureC = if (weather.isValid) weather.temperatureC else null
            )
        )
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
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0E1B2E).copy(alpha = 0.95f), Color(0xFF060B14).copy(alpha = 0.95f))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(listOf(Color(0xFF4EA8DE).copy(alpha = 0.55f), Color.Transparent, Color(0xFF3A7BB0).copy(alpha = 0.3f))),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "WATER",
                color = Color(0xFFBFE1FF),
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = Color(0xFF4EA8DE), blurRadius = 12f))
            )

            // Weather chip
            Spacer(Modifier.height(6.dp))
            WeatherChipInternal(weather = weather, refreshing = isRefreshing, font = contentFont, tempUnit = prefs.tempUnit) {
                scope.launch {
                    isRefreshing = true
                    weather = WeatherService.getWeather(ctx, forceRefresh = true)
                    isRefreshing = false
                }
            }

            Spacer(Modifier.height(14.dp))

            // Big central glass visualisation
            WaterBottle(glasses = glasses, target = targetGlasses)

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${Units.displayVolume(glasses * glassMl, prefs.volumeUnit)}  /  ${Units.displayVolume(goalMl, prefs.volumeUnit)}",
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = NokiaFont,
                letterSpacing = 2.sp
            )
            Text(
                text = "$glasses / $targetGlasses glasses",
                color = Color(0xFFBFE1FF).copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = contentFont,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(14.dp))

            // Add / remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WaterBtn("−", Modifier.weight(1f), enabled = glasses > 0) {
                    if (glasses > 0) glasses -= 1
                }
                WaterBtn("+ GLASS", Modifier.weight(2f), primary = true) {
                    glasses += 1
                }
                WaterBtn("+", Modifier.weight(1f)) {
                    glasses += 1
                }
            }

            Spacer(Modifier.height(12.dp))

            // Breakdown
            GoalBreakdown(profile, weather, goalMl, prefs, contentFont)

            Spacer(Modifier.height(12.dp))

            // 7-day history
            SevenDayHistory(today, targetGlasses, contentFont)

            Spacer(Modifier.height(14.dp))
            Text(
                "CLOSE",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}

@Composable
private fun WeatherChip(weather: WeatherData, refreshing: Boolean, font: FontFamily, onRefresh: () -> Unit) {
    WeatherChipInternal(weather, refreshing, font, "C", onRefresh)
}

@Composable
private fun WeatherChipInternal(weather: WeatherData, refreshing: Boolean, font: FontFamily, tempUnit: String, onRefresh: () -> Unit) {
    val label = when {
        refreshing -> "fetching weather…"
        !weather.isValid -> "weather unavailable — tap to retry"
        else -> {
            val temp = Units.displayTemp(weather.temperatureC, tempUnit)
            val loc = if (weather.locationName.isNotBlank()) " · ${weather.locationName}" else ""
            "$temp · ${weather.condition}$loc"
        }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A2E44).copy(alpha = 0.7f))
            .border(1.dp, Color(0xFF4EA8DE).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable { onRefresh() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color(0xFFBFE1FF),
            fontSize = 11.sp,
            fontFamily = font,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun WaterBottle(glasses: Int, target: Int) {
    val ratio = if (target > 0) (glasses.toFloat() / target).coerceIn(0f, 1.2f) else 0f
    Box(
        modifier = Modifier
            .size(width = 130.dp, height = 180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rInset = 10f
            val bottleTop = h * 0.10f
            val bottleBottom = h
            val bottleH = bottleBottom - bottleTop
            val r = CornerRadius(16f, 16f)

            // Neck
            drawRoundRect(
                color = Color(0xFF223A55),
                topLeft = Offset(w * 0.38f, 0f),
                size = Size(w * 0.24f, h * 0.10f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Bottle body outline
            drawRoundRect(
                color = Color(0xFF0F2036),
                topLeft = Offset(rInset, bottleTop),
                size = Size(w - 2 * rInset, bottleH),
                cornerRadius = r
            )
            // Water fill (from bottom)
            val fillH = bottleH * ratio.coerceAtMost(1f)
            if (fillH > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4EA8DE), Color(0xFF2E6DA8)),
                        startY = bottleBottom - fillH,
                        endY = bottleBottom
                    ),
                    topLeft = Offset(rInset, bottleBottom - fillH),
                    size = Size(w - 2 * rInset, fillH),
                    cornerRadius = r
                )
                // Wave sheen
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(rInset + 6f, bottleBottom - fillH + 2f),
                    size = Size((w - 2 * rInset) * 0.35f, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }
            // Outline
            drawRoundRect(
                color = Color(0xFF4EA8DE).copy(alpha = 0.6f),
                topLeft = Offset(rInset, bottleTop),
                size = Size(w - 2 * rInset, bottleH),
                cornerRadius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
        Text(
            text = "%d%%".format((ratio.coerceAtMost(1f) * 100).toInt()),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = NokiaFont,
            style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 4f))
        )
    }
}

@Composable
private fun WaterBtn(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (primary) Color(0xFF2E6DA8) else Color(0xFF1A2E44)
    val border = if (primary) Color(0xFF6FC1FF) else Color(0xFF4EA8DE).copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .border(1.dp, if (enabled) border else border.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .let { if (enabled) it.clickable { onClick() } else it },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            fontSize = if (primary) 15.sp else 20.sp,
            fontFamily = BebasFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun GoalBreakdown(profile: UserProfile, weather: WeatherData, goalMl: Int, prefs: AppPreferences, font: FontFamily) {
    val base = profile.baseWaterMl
    val activityBonus = when (profile.activityLevel) {
        "Active" -> 500; "VeryActive" -> 800; "Moderate" -> 250; "Light" -> 100; else -> 0
    }
    val tempBonus = goalMl - base - activityBonus
    fun formatVol(ml: Int) = Units.displayVolume(ml, prefs.volumeUnit)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A1522).copy(alpha = 0.7f))
            .border(1.dp, Color(0xFF4EA8DE).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("HOW WE COMPUTED THIS", color = Color(0xFF4EA8DE), fontSize = 10.sp, fontFamily = font, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        BreakRow("Body baseline (35 ml × kg)", formatVol(base), font)
        if (activityBonus != 0) BreakRow("Activity (${profile.activityLevel.lowercase()})", "+" + formatVol(activityBonus), font)
        if (tempBonus != 0 && weather.isValid) {
            val temp = Units.displayTemp(weather.temperatureC, prefs.tempUnit)
            val sign = if (tempBonus > 0) "+" else "−"
            BreakRow("Temperature ($temp)", sign + formatVol(kotlin.math.abs(tempBonus)), font)
        }
    }
}

@Composable
private fun BreakRow(label: String, value: String, font: FontFamily) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, fontFamily = font)
        Text(value, color = Color.White, fontSize = 11.sp, fontFamily = font, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SevenDayHistory(today: LocalDate, targetGlasses: Int, font: FontFamily) {
    val history = remember { FitDataRepository.loadWaterHistory() }
    val days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("LAST 7 DAYS", color = Color(0xFF4EA8DE), fontSize = 10.sp, fontFamily = font, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            days.forEach { d ->
                val entry = history[d.toString()]
                val consumed = entry?.glassesConsumed ?: 0
                val goal = ((entry?.goalMl ?: (targetGlasses * 250)) / 250).coerceAtLeast(1)
                val pct = (consumed.toFloat() / goal).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0A1522)),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(pct)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF6FC1FF), Color(0xFF2E6DA8))
                                    )
                                )
                        )
                    }
                    Text(
                        d.dayOfWeek.name.take(2),
                        color = if (d == today) OrangeFire else Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = font,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
