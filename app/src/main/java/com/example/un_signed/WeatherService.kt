package com.example.un_signed

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WeatherService {

    private const val TTL_MS = 3600_000L // 1 hour

    /** Fetch fresh weather from Open-Meteo — no API key. */
    private suspend fun fetchOpenMeteo(lat: Double, lon: Double, locationName: String): WeatherData? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code" +
                        "&timezone=auto"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                conn.requestMethod = "GET"
                if (conn.responseCode !in 200..299) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).optJSONObject("current") ?: return@withContext null
                val temp = current.optDouble("temperature_2m", Double.NaN)
                val code = current.optInt("weather_code", -1)
                if (temp.isNaN()) return@withContext null
                WeatherData(
                    temperatureC = temp,
                    condition = describeWmo(code),
                    locationName = locationName,
                    latitude = lat,
                    longitude = lon,
                    fetchedAt = System.currentTimeMillis()
                )
            } catch (_: Exception) { null }
        }

    /** Reverse-geocode via Open-Meteo (only if we have coords but no city name). */
    private suspend fun reverseName(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode("$lat,$lon", "UTF-8")
            val url = URL("https://geocoding-api.open-meteo.com/v1/reverse?latitude=$lat&longitude=$lon&count=1&language=en&format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4_000
            conn.readTimeout = 4_000
            if (conn.responseCode !in 200..299) return@withContext ""
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext ""
            if (results.length() == 0) return@withContext ""
            val r = results.getJSONObject(0)
            listOf(r.optString("name"), r.optString("admin1"))
                .filter { it.isNotBlank() }
                .joinToString(", ")
        } catch (_: Exception) { "" }
    }

    /**
     * Get current weather. Returns cached value if fresh, else fetches.
     * Silently returns cached (possibly stale) or empty on failure — never throws.
     */
    suspend fun getWeather(context: Context, forceRefresh: Boolean = false): WeatherData {
        val cache = FitDataRepository.loadWeatherCache()
        if (!forceRefresh && cache.isValid && !cache.isStale(TTL_MS)) return cache

        val loc = LocationHelper.resolve(context) ?: return cache
        val name = loc.label.ifBlank { reverseName(loc.latitude, loc.longitude) }
        val fresh = fetchOpenMeteo(loc.latitude, loc.longitude, name) ?: return cache
        FitDataRepository.saveWeatherCache(fresh)
        return fresh
    }

    /** WMO weather-code → human-readable. */
    private fun describeWmo(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Mostly clear"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm w/ hail"
        else -> "—"
    }
}

/** Water goal calculator — factors weight, activity, and current temperature. */
object WaterGoal {
    fun compute(profile: UserProfile, weather: WeatherData?): Int {
        val base = profile.baseWaterMl.coerceAtLeast(1500)
        val activityBonus = when (profile.activityLevel) {
            "Active" -> 500
            "VeryActive" -> 800
            "Moderate" -> 250
            "Light" -> 100
            else -> 0
        }
        val tempBonus = when (val t = weather?.temperatureC) {
            null -> 0
            in Double.NEGATIVE_INFINITY..10.0 -> -200
            in 10.0..25.0 -> 0
            in 25.0..30.0 -> 250
            in 30.0..35.0 -> 500
            else -> if (t > 35.0) 1000 else 0
        }
        return (base + activityBonus + tempBonus).coerceIn(1500, 6000)
    }
}
