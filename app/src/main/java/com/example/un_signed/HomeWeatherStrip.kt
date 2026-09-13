package com.example.un_signed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Home-page weather trace — designed to *disappear* into the ashes aesthetic.
 * No card, no border, no chunky badge — just a single low-contrast text row
 * with a hair-thin sparkline drawn beneath it like an instrument trace.
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
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // ── Line 1: emoji · temp · condition · feels ─── AQI dot + value ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (valid) {
                Text(
                    text = conditionEmoji(weather.condition),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 5.dp)
                )

                val tempStr = Units.displayTemp(weather.temperatureC, tempUnit, 0)
                val condStr = weather.condition.ifBlank { "—" }.uppercase()
                Text(
                    text = "$tempStr · $condStr",
                    color = palette.subtle,
                    fontSize = 11.sp,
                    fontFamily = NokiaFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (!weather.feelsLikeC.isNaN() &&
                    kotlin.math.abs(weather.feelsLikeC - weather.temperatureC) >= 1.0) {
                    Text(
                        text = "  ·  FEELS ${Units.displayTemp(weather.feelsLikeC, tempUnit, 0)}",
                        color = palette.faint,
                        fontSize = 10.sp,
                        fontFamily = NokiaFont,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                if (weather.usAqi >= 0) {
                    AqiInline(weather.usAqi, weather.aqiCategory, palette)
                }
            } else {
                Text(
                    text = if (isLoading) "· REFRESHING WEATHER TRACE …" else "· TAP TO CALIBRATE WEATHER",
                    color = palette.faint,
                    fontSize = 10.sp,
                    fontFamily = NokiaFont,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 1.sp
                )
            }
        }

        // ── Line 2: location (very muted, italic) ─────────
        if (valid && weather.locationName.isNotBlank()) {
            Text(
                text = weather.locationName.uppercase(),
                color = palette.faint,
                fontSize = 9.sp,
                fontFamily = NokiaFont,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        // ── Line 3: hair-thin sparkline (instrument trace) ─
        if (valid && weather.hourlyForecast.size >= 2) {
            Spacer(Modifier.height(4.dp))
            HourlyTrace(
                temps = weather.hourlyForecast.map { it.tempC },
                strokeColor = palette.accentPrimary.copy(alpha = 0.55f),
                baseColor = palette.faint.copy(alpha = 0.20f)
            )
        }
    }
}

/**
 * AQI shown as a small colored dot + monospace number — no chunky pill,
 * so it doesn't compete with the retro-industrial reel above.
 */
@Composable
private fun AqiInline(aqi: Int, category: String, palette: ThemePalette) {
    val dotColor = when {
        aqi <= 50 -> Color(0xFF8FE087)
        aqi <= 100 -> Color(0xFFFFE082)
        aqi <= 150 -> Color(0xFFFFCC80)
        aqi <= 200 -> Color(0xFFEF9A9A)
        aqi <= 300 -> Color(0xFFCE93D8)
        else -> Color(0xFFE1BEE7)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "AQI $aqi",
            color = palette.subtle,
            fontSize = 10.sp,
            fontFamily = NokiaFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        if (category.isNotBlank()) {
            Text(
                text = " · ${category.uppercase().take(10)}",
                color = palette.faint,
                fontSize = 9.sp,
                fontFamily = NokiaFont,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Full-width thin sparkline — a baseline dotted rule plus a single crisp trace.
 * Feels like it belongs on an old instrument panel rather than a modern chart.
 */
@Composable
private fun HourlyTrace(
    temps: List<Double>,
    strokeColor: Color,
    baseColor: Color
) {
    val valid = temps.filter { !it.isNaN() }
    if (valid.size < 2) return

    val minT = valid.min()
    val maxT = valid.max()
    val range = (maxT - minT).coerceAtLeast(0.5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stepX = if (valid.size > 1) w / (valid.size - 1) else w

            // Dotted baseline near the bottom — the "instrument rule"
            drawLine(
                color = baseColor,
                start = Offset(0f, h - 1f),
                end = Offset(w, h - 1f),
                strokeWidth = 0.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f)
            )

            // Actual trace — single hairline, no gradient, no fill
            val pts = valid.mapIndexed { i, v ->
                val ny = ((v - minT) / range).toFloat().coerceIn(0f, 1f)
                Offset(i * stepX, h * (1f - ny) * 0.85f + 1f)
            }
            for (i in 0 until pts.size - 1) {
                drawLine(
                    color = strokeColor,
                    start = pts[i], end = pts[i + 1],
                    strokeWidth = 1.5f,
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
        "heavy snow" in c || "snow showers" in c -> "❄"
        "snow" in c -> "🌨"
        "freezing" in c -> "🌨"
        "heavy rain" in c || "rain showers" in c -> "🌧"
        "rain" in c -> "🌦"
        "drizzle" in c -> "🌧"
        "fog" in c || "mist" in c -> "🌫"
        "overcast" in c -> "☁"
        "mostly clear" in c || "partly" in c -> "⛅"
        "clear" in c || "sunny" in c -> "☀"
        else -> "·"
    }
}
