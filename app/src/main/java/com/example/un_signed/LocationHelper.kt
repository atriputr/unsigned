package com.example.un_signed

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ResolvedLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String,      // city/region name (best-effort)
    val source: String,     // "gps" | "network" | "ip"
    val countryCode: String = "",  // ISO 3166-1 alpha-2 (e.g., "IN", "US") — only from ip lookup
    val countryName: String = ""   // full country name — only from ip lookup
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Fast path: read last-known location from any enabled provider. Never blocks for a fresh fix. */
    private fun lastKnown(context: Context): ResolvedLocation? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = try { lm.getProviders(true) } catch (_: SecurityException) { return null }
        var best: Location? = null
        for (p in providers) {
            val loc = try { lm.getLastKnownLocation(p) } catch (_: SecurityException) { null } ?: continue
            if (best == null || loc.accuracy < best.accuracy) best = loc
        }
        val l = best ?: return null
        val source = when {
            providers.contains(LocationManager.GPS_PROVIDER) && l.provider == LocationManager.GPS_PROVIDER -> "gps"
            else -> "network"
        }
        return ResolvedLocation(l.latitude, l.longitude, "", source)
    }

    /** IP-based fallback — city-accurate, no permission needed. */
    private suspend fun ipLookup(): ResolvedLocation? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://ipapi.co/json/").openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val lat = json.optDouble("latitude", Double.NaN)
            val lon = json.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) return@withContext null
            val city = json.optString("city", "")
            val region = json.optString("region", "")
            val label = listOf(city, region).filter { it.isNotBlank() }.joinToString(", ")
            val countryCode = json.optString("country_code", "")
            val countryName = json.optString("country_name", "")
            ResolvedLocation(lat, lon, label, "ip", countryCode, countryName)
        } catch (_: Exception) { null }
    }

    /** Best-effort country ISO code — from cached weather → live IP → empty. */
    suspend fun countryCode(context: Context): String {
        val cachedName = FitDataRepository.loadWeatherCache().locationName
        if (cachedName.isNotBlank()) {
            // cached weather doesn't carry country; do a live IP lookup only if permission-less
        }
        return resolve(context)?.countryCode ?: ""
    }

    /** Try device location → fall back to IP lookup. */
    suspend fun resolve(context: Context): ResolvedLocation? {
        lastKnown(context)?.let { return it }
        return ipLookup()
    }
}
