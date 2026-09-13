package com.example.un_signed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Compact home-page weather widget:
 *   [ Condition emoji | temp · condition · location  ]  [ AQI badge ]
 *   [ 12-hour temperature sparkline                                  ]
 *
 * Tapping the strip opens the full WeatherOverlay via [onClick].
 */
@Composable
fun HomeWeatherStrip(
    weather: WeatherData,
    tempUnit: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalPalette.current
    val valid = weather.isValid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.prismBorderBrush(), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Condition emoji
            Text(
                text = conditionEmoji(weather.condition),
                fontSize = 22.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                if (valid) {
                    val tempStr = Units.displayTemp(weather.temperatureC, tempUnit, 0)
                    val feelStr = if (!weather.feelsLikeC.isNaN() &&
                        kotlin.math.abs(weather.feelsLikeC - weather.temperatureC) >= 1.0)
                        "  ·  feels ${Units.displayTemp(weather.feelsLikeC, tempUnit, 0)}" else ""
                    Text(
                        text = "$tempStr  ·  ${weather.condition.ifBlank { "—" }}$feelStr",
                        color = palette.onSurface,
                        fontSize = 13.sp,
                        fontFamily = titleFontFor(palette.name),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    val locName = weather.locationName.ifBlank { "current location" }.uppercase()
                    Text(
                        text = locName,
                        color = palette.subtle,
                        fontSize = 10.sp,
                        fontFamily = contentFontFor(palette.name),
                        letterSpacing = 1.sp
                    )
                } else {
                    Text(
                        text = if (isLoading) "REFRESHING WEATHER…" else "TAP TO FETCH WEATHER",
                        color = palette.subtle,
                        fontSize = 12.sp,
                        fontFamily = titleFontFor(palette.name),
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "location & AQI will appear here",
                        color = palette.faint,
                        fontSize = 10.sp,
                        fontFamily = contentFontFor(palette.name)
                    )
                }
            }

            if (valid && weather.usAqi >= 0) {
                AqiBadge(weather.usAqi, weather.aqiCategory, palette)
            } else if (isLoading) {
                Text(
                    text = "…",
                    color = palette.faint,
                    fontSize = 18.sp
                )
            }
        }

        if (valid && weather.hourlyForecast.size >= 2) {
            Spacer(Modifier.height(6.dp))
            HourlySparkline(
                temps = weather.hourlyForecast.map { it.tempC },
                palette = palette
            )
        }
    }
}

@Composable
private fun AqiBadge(aqi: Int, category: String, palette: ThemePalette) {
    val (fg, bg) = when {
        aqi <= 50 -> Color(0xFF8FE087) to Color(0xFF2E7D32).copy(alpha = 0.35f)
        aqi <= 100 -> Color(0xFFFFE082) to Color(0xFFF57F17).copy(alpha = 0.35f)
        aqi <= 150 -> Color(0xFFFFCC80) to Color(0xFFE65100).copy(alpha = 0.40f)
        aqi <= 200 -> Color(0xFFEF9A9A) to Color(0xFFC62828).copy(alpha = 0.45f)
        aqi <= 300 -> Color(0xFFCE93D8) to Color(0xFF6A1B9A).copy(alpha = 0.45f)
        else -> Color(0xFFE1BEE7) to Color(0xFF4A148C).copy(alpha = 0.50f)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(0.5.dp, fg.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = "AQI $aqi",
                color = fg,
                fontSize = 11.sp,
                fontFamily = titleFontFor(palette.name),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (category.isNotBlank()) {
                Text(
                    text = category.uppercase().take(14),
                    color = fg.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontFamily = contentFontFor(palette.name),
                    letterSpacing = 0.5.sp,
                    style = TextStyle(lineHeight = 9.sp)
                )
            }
        }
    }
}

@Composable
private fun HourlySparkline(temps: List<Double>, palette: ThemePalette) {
    val valid = temps.filter { !it.isNaN() }
    if (valid.size < 2) return

    val minT = valid.min()
    val maxT = valid.max()
    val range = (maxT - minT).coerceAtLeast(0.5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stepX = if (valid.size > 1) w / (valid.size - 1) else w

            val pts = valid.mapIndexed { i, v ->
                val ny = ((v - minT) / range).toFloat().coerceIn(0f, 1f)
                Offset(i * stepX, h * (1f - ny))
            }

            // Halo line
            for (i in 0 until pts.size - 1) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(palette.accentPrimary.copy(alpha = 0.9f), palette.accentSecondary.copy(alpha = 0.9f))
                    ),
                    start = pts[i], end = pts[i + 1],
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** Very lightweight condition → emoji mapper — no image assets needed. */
private fun conditionEmoji(condition: String): String {
    val c = condition.lowercase(Locale.ENGLISH)
    return when {
        "thunder" in c -> "⛈"
        "hail" in c -> "🌨"
        "heavy snow" in c || "snow showers" in c -> "❄️"
        "snow" in c -> "🌨"
        "freezing" in c -> "🌨"
        "heavy rain" in c || "rain showers" in c -> "🌧"
        "rain" in c -> "🌦"
        "drizzle" in c -> "🌧"
        "fog" in c || "mist" in c -> "🌫"
        "overcast" in c -> "☁️"
        "mostly clear" in c || "partly" in c -> "⛅"
        "clear" in c || "sunny" in c -> "☀️"
        else -> "🌡"
    }
}
