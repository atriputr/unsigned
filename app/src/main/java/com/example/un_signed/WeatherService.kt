package com.example.un_signed

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object WeatherService {

    private const val TTL_MS = 3600_000L // 1 hour

    /** Fetch fresh weather and air quality from Open-Meteo — no API key required. */
    private suspend fun fetchOpenMeteo(lat: Double, lon: Double, locationName: String): WeatherData? =
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch Forecast
                val weatherUrl = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                        "&hourly=temperature_2m,weather_code" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,uv_index_max" +
                        "&timezone=auto"
                )
                val conn = weatherUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                conn.requestMethod = "GET"
                if (conn.responseCode !in 200..299) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val current = root.optJSONObject("current") ?: return@withContext null

                val temp = current.optDouble("temperature_2m", Double.NaN)
                val feelsLike = current.optDouble("apparent_temperature", temp)
                val humidity = current.optInt("relative_humidity_2m", 0)
                val windSpeed = current.optDouble("wind_speed_10m", 0.0)
                val code = current.optInt("weather_code", -1)

                if (temp.isNaN()) return@withContext null

                // Hourly Forecast (next 12 hours)
                val hourlyList = mutableListOf<HourlyForecast>()
                val hourlyObj = root.optJSONObject("hourly")
                if (hourlyObj != null) {
                    val times = hourlyObj.optJSONArray("time")
                    val temps = hourlyObj.optJSONArray("temperature_2m")
                    val codes = hourlyObj.optJSONArray("weather_code")
                    if (times != null && temps != null && codes != null) {
                        val count = minOf(12, times.length())
                        for (i in 0 until count) {
                            val rawTime = times.optString(i, "")
                            val hourLabel = try {
                                val parsed = LocalDateTime.parse(rawTime)
                                parsed.format(DateTimeFormatter.ofPattern("ha", Locale.ENGLISH)).lowercase()
                            } catch (_: Exception) { rawTime.takeLast(5) }
                            val hTemp = temps.optDouble(i, Double.NaN)
                            val hCode = codes.optInt(i, 0)
                            hourlyList.add(HourlyForecast(hourLabel, hTemp, describeWmo(hCode)))
                        }
                    }
                }

                // Daily Forecast (7 days)
                val dailyList = mutableListOf<DailyForecast>()
                val dailyObj = root.optJSONObject("daily")
                var uvMaxToday = 0.0
                if (dailyObj != null) {
                    val times = dailyObj.optJSONArray("time")
                    val maxTemps = dailyObj.optJSONArray("temperature_2m_max")
                    val minTemps = dailyObj.optJSONArray("temperature_2m_min")
                    val codes = dailyObj.optJSONArray("weather_code")
                    val uvs = dailyObj.optJSONArray("uv_index_max")
                    if (times != null && maxTemps != null && minTemps != null && codes != null) {
                        val count = minOf(7, times.length())
                        for (i in 0 until count) {
                            val rawTime = times.optString(i, "")
                            val dayLabel = if (i == 0) "Today" else try {
                                val parsed = LocalDate.parse(rawTime)
                                parsed.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
                            } catch (_: Exception) { rawTime }
                            val maxT = maxTemps.optDouble(i, Double.NaN)
                            val minT = minTemps.optDouble(i, Double.NaN)
                            val dCode = codes.optInt(i, 0)
                            val uv = uvs?.optDouble(i, 0.0) ?: 0.0
                            if (i == 0) uvMaxToday = uv
                            dailyList.add(DailyForecast(dayLabel, maxT, minT, describeWmo(dCode), uv))
                        }
                    }
                }

                // 2. Fetch Air Quality (AQI)
                var usAqi = -1
                var aqiCat = ""
                var pm25Val = Double.NaN
                var pm10Val = Double.NaN
                var no2Val = Double.NaN
                var o3Val = Double.NaN

                try {
                    val aqiUrl = URL(
                        "https://air-quality-api.open-meteo.com/v1/air-quality" +
                            "?latitude=$lat&longitude=$lon" +
                            "&current=us_aqi,pm10,pm2_5,nitrogen_dioxide,ozone" +
                            "&timezone=auto"
                    )
                    val aqiConn = aqiUrl.openConnection() as HttpURLConnection
                    aqiConn.connectTimeout = 4_000
                    aqiConn.readTimeout = 4_000
                    if (aqiConn.responseCode in 200..299) {
                        val aqiBody = aqiConn.inputStream.bufferedReader().use { it.readText() }
                        val aqiCurr = JSONObject(aqiBody).optJSONObject("current")
                        if (aqiCurr != null) {
                            usAqi = aqiCurr.optInt("us_aqi", -1)
                            pm25Val = aqiCurr.optDouble("pm2_5", Double.NaN)
                            pm10Val = aqiCurr.optDouble("pm10", Double.NaN)
                            no2Val = aqiCurr.optDouble("nitrogen_dioxide", Double.NaN)
                            o3Val = aqiCurr.optDouble("ozone", Double.NaN)
                            aqiCat = getAqiCategory(usAqi)
                        }
                    }
                } catch (_: Exception) { }

                WeatherData(
                    temperatureC = temp,
                    feelsLikeC = feelsLike,
                    humidityPercent = humidity,
                    windSpeedKmh = windSpeed,
                    uvIndex = uvMaxToday,
                    condition = describeWmo(code),
                    locationName = locationName,
                    latitude = lat,
                    longitude = lon,
                    usAqi = usAqi,
                    aqiCategory = aqiCat,
                    pm25 = pm25Val,
                    pm10 = pm10Val,
                    no2 = no2Val,
                    o3 = o3Val,
                    hourlyForecast = hourlyList,
                    dailyForecast = dailyList,
                    dataSource = "Open-Meteo Weather & Air Quality APIs (WMO Compliant)",
                    fetchedAt = System.currentTimeMillis()
                )
            } catch (_: Exception) { null }
        }

    private fun getAqiCategory(aqi: Int): String = when (aqi) {
        in 0..50 -> "Good"
        in 51..100 -> "Moderate"
        in 101..150 -> "Unhealthy for Sensitive Groups"
        in 151..200 -> "Unhealthy"
        in 201..300 -> "Very Unhealthy"
        in 301..500 -> "Hazardous"
        else -> if (aqi > 500) "Hazardous" else "Unknown"
    }

    /** Multi-source reverse-geocode (BigDataCloud → Nominatim → IP fallback). */
    private suspend fun reverseName(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        // 1. BigDataCloud free client API
        try {
            val url = URL("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=en")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4_000
            conn.readTimeout = 4_000
            conn.setRequestProperty("User-Agent", "UnsignedApp/1.0")
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val city = json.optString("city").ifBlank { json.optString("locality") }.ifBlank { json.optString("principalSubdivision") }
                val country = json.optString("countryName")
                if (city.isNotBlank()) {
                    return@withContext if (country.isNotBlank()) "$city, $country" else city
                }
            }
        } catch (_: Exception) { }

        // 2. OpenStreetMap Nominatim API
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4_000
            conn.readTimeout = 4_000
            conn.setRequestProperty("User-Agent", "UnsignedApp/1.0")
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val addr = json.optJSONObject("address")
                if (addr != null) {
                    val city = addr.optString("city").ifBlank { addr.optString("town") }.ifBlank { addr.optString("village") }.ifBlank { addr.optString("state_district") }
                    val country = addr.optString("country")
                    if (city.isNotBlank()) {
                        return@withContext if (country.isNotBlank()) "$city, $country" else city
                    }
                }
            }
        } catch (_: Exception) { }

        ""
    }

    private fun getNativeCityName(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                val country = addr.countryName ?: ""
                if (!city.isNullOrBlank()) {
                    if (country.isNotBlank()) "$city, $country" else city
                } else {
                    addr.featureName ?: ""
                }
            } else ""
        } catch (_: Exception) { "" }
    }

    /**
     * Get current weather. Returns cached value if fresh, else fetches.
     */
    suspend fun getWeather(context: Context, forceRefresh: Boolean = false): WeatherData {
        val cache = FitDataRepository.loadWeatherCache()
        val isGenericName = cache.locationName.isBlank() ||
            cache.locationName.equals("LOCATION", ignoreCase = true) ||
            cache.locationName.equals("DETECTED LOCATION", ignoreCase = true) ||
            cache.locationName.equals("MY LOCATION", ignoreCase = true)

        if (!forceRefresh && cache.isValid && !cache.isStale(TTL_MS) && !isGenericName) {
            return cache
        }

        val loc = LocationHelper.resolve(context) ?: return cache
        val nativeName = getNativeCityName(context, loc.latitude, loc.longitude)
        val rawName = nativeName.ifBlank { loc.label.ifBlank { reverseName(loc.latitude, loc.longitude) } }
        val name = if (rawName.isBlank() || rawName.equals("LOCATION", ignoreCase = true) || rawName.equals("DETECTED LOCATION", ignoreCase = true)) {
            reverseName(loc.latitude, loc.longitude).ifBlank { "CURRENT LOCATION" }
        } else rawName

        val fresh = fetchOpenMeteo(loc.latitude, loc.longitude, name) ?: return cache
        FitDataRepository.saveWeatherCache(fresh)
        return fresh
    }

    /** WMO weather-code → human-readable. */
    fun describeWmo(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Mostly Clear"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing Drizzle"
        61 -> "Light Rain"
        63 -> "Rain"
        65 -> "Heavy Rain"
        66, 67 -> "Freezing Rain"
        71 -> "Light Snow"
        73 -> "Snow"
        75 -> "Heavy Snow"
        77 -> "Snow Grains"
        80, 81, 82 -> "Rain Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm w/ Hail"
        else -> "Clear"
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
