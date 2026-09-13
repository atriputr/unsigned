package com.example.un_signed

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WeatherOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    context: Context,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var weatherData by remember { mutableStateOf(FitDataRepository.loadWeatherCache()) }
    var isLoading by remember { mutableStateOf(false) }

    fun refresh(force: Boolean) {
        scope.launch {
            isLoading = true
            weatherData = WeatherService.getWeather(context, forceRefresh = force)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (!weatherData.isValid || weatherData.isStale()) {
            refresh(force = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF212121A), Color(0xF208080E))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click inside card */ }
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WEATHER & AIR QUALITY",
                            color = OrangeFire,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont,
                            letterSpacing = 2.sp
                        )
                        val loc = weatherData.locationName.ifBlank { "DETECTING LOCATION..." }.uppercase()
                        Text(
                            text = loc,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = contentFont
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLoading) "REFRESHING..." else "REFRESH",
                            color = Color(0xFF64B5F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x222196F3))
                                .clickable { if (!isLoading) refresh(force = true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "✕",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onClose() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: Main Weather Hero
                    item {
                        WeatherHeroCard(weatherData = weatherData, titleFont = titleFont, contentFont = contentFont)
                    }

                    // Item 2: Air Quality Index (AQI)
                    item {
                        AqiCard(weatherData = weatherData, titleFont = titleFont, contentFont = contentFont)
                    }

                    // Item 3: Hourly Forecast
                    if (weatherData.hourlyForecast.isNotEmpty()) {
                        item {
                            HourlyForecastCard(weatherData = weatherData, titleFont = titleFont, contentFont = contentFont)
                        }
                    }

                    // Item 4: 7-Day Prediction Forecast
                    if (weatherData.dailyForecast.isNotEmpty()) {
                        item {
                            DailyForecastCard(weatherData = weatherData, titleFont = titleFont, contentFont = contentFont)
                        }
                    }

                    // Item 5: Data Sources
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "OFFICIAL DATA SOURCES",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = titleFont,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = weatherData.dataSource,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontFamily = contentFont
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherHeroCard(
    weatherData: WeatherData,
    titleFont: FontFamily,
    contentFont: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x331E293B))
            .border(1.dp, Color(0x4D64B5F6), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val tempStr = if (weatherData.temperatureC.isNaN()) "--°C"
                    else String.format(Locale.getDefault(), "%.1f°C", weatherData.temperatureC)

                    Text(
                        text = tempStr,
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    val cond = weatherData.condition.ifBlank { "CLEAR" }.uppercase()
                    Text(
                        text = cond,
                        color = Color(0xFFFFB454),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = contentFont,
                        letterSpacing = 1.sp
                    )
                }

                val feelsLike = if (weatherData.feelsLikeC.isNaN()) "--"
                else String.format(Locale.getDefault(), "%.1f°C", weatherData.feelsLikeC)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "FEELS LIKE",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontFamily = titleFont
                        )
                        Text(
                            text = feelsLike,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = contentFont
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Weather Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("HUMIDITY", "${weatherData.humidityPercent}%", titleFont, contentFont)
                MetricItem(
                    "WIND",
                    if (weatherData.windSpeedKmh.isNaN()) "--" else String.format(Locale.getDefault(), "%.1f km/h", weatherData.windSpeedKmh),
                    titleFont,
                    contentFont
                )
                MetricItem(
                    "UV INDEX",
                    if (weatherData.uvIndex.isNaN()) "--" else String.format(Locale.getDefault(), "%.1f", weatherData.uvIndex),
                    titleFont,
                    contentFont
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    titleFont: FontFamily,
    contentFont: FontFamily
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontFamily = titleFont,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = contentFont
        )
    }
}

@Composable
private fun AqiCard(
    weatherData: WeatherData,
    titleFont: FontFamily,
    contentFont: FontFamily
) {
    val aqi = weatherData.usAqi
    val category = weatherData.aqiCategory.ifBlank { "UNKNOWN" }.uppercase()

    val (badgeBg, badgeText) = when (aqi) {
        in 0..50 -> Pair(Color(0xFF2E7D32), Color(0xFFA5D6A7))
        in 51..100 -> Pair(Color(0xFFF57F17), Color(0xFFFFF59D))
        in 101..150 -> Pair(Color(0xFFE65100), Color(0xFFFFCC80))
        in 151..200 -> Pair(Color(0xFFC62828), Color(0xFFEF9A9A))
        in 201..300 -> Pair(Color(0xFF6A1B9A), Color(0xFFCE93D8))
        else -> if (aqi > 300) Pair(Color(0xFF4A148C), Color(0xFFE1BEE7)) else Pair(Color(0xFF37474F), Color.White)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x331E293B))
            .border(1.dp, badgeBg.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AIR QUALITY INDEX (AQI)",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (aqi >= 0) "$aqi US AQI" else "DATA UNAVAILABLE",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                }

                if (aqi >= 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            color = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            if (aqi >= 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PollutantItem("PM2.5", weatherData.pm25, "µg/m³", titleFont, contentFont)
                    PollutantItem("PM10", weatherData.pm10, "µg/m³", titleFont, contentFont)
                    PollutantItem("NO₂", weatherData.no2, "µg/m³", titleFont, contentFont)
                    PollutantItem("O₃", weatherData.o3, "µg/m³", titleFont, contentFont)
                }
            }
        }
    }
}

@Composable
private fun PollutantItem(
    label: String,
    value: Double,
    unit: String,
    titleFont: FontFamily,
    contentFont: FontFamily
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontFamily = titleFont
        )
        val valStr = if (value.isNaN()) "--" else String.format(Locale.getDefault(), "%.1f", value)
        Text(
            text = "$valStr $unit",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = contentFont
        )
    }
}

@Composable
private fun HourlyForecastCard(
    weatherData: WeatherData,
    titleFont: FontFamily,
    contentFont: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x221E293B))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = "HOURLY FORECAST PREDICTION",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = titleFont,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(weatherData.hourlyForecast) { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = item.timeLabel.uppercase(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontFamily = titleFont
                            )
                            Spacer(Modifier.height(4.dp))
                            val t = if (item.tempC.isNaN()) "--"
                            else String.format(Locale.getDefault(), "%.0f°", item.tempC)
                            Text(
                                text = t,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = contentFont
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyForecastCard(
    weatherData: WeatherData,
    titleFont: FontFamily,
    contentFont: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x221E293B))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = "7-DAY PREDICTION FORECAST",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = titleFont,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weatherData.dailyForecast.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.dayLabel.uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont,
                            modifier = Modifier.width(60.dp)
                        )

                        Text(
                            text = item.condition.uppercase(),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = contentFont
                        )

                        val maxT = if (item.maxTempC.isNaN()) "--"
                        else String.format(Locale.getDefault(), "%.0f°", item.maxTempC)
                        val minT = if (item.minTempC.isNaN()) "--"
                        else String.format(Locale.getDefault(), "%.0f°", item.minTempC)

                        Text(
                            text = "$maxT / $minT",
                            color = OrangeFire,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont
                        )
                    }
                }
            }
        }
    }
}
