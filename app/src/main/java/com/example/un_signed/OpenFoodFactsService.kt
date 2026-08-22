package com.example.un_signed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * A single product returned by Open Food Facts.
 * All nutrition fields are per 100 g (or per 100 ml for beverages).
 */
data class OffProduct(
    val id: String,
    val productName: String,
    val brand: String,
    val category: String,
    val imageUrl: String,
    val country: String,
    val quantity: String,             // e.g. "100g", "500ml"
    val nutriscore: String,           // a-e or empty
    val novaGroup: Int,               // 1..4 (4 = ultra-processed) or 0 unknown
    val energyKcal100g: Double,
    val sugar100g: Double,
    val saturatedFat100g: Double,
    val salt100g: Double,
    val fiber100g: Double,
    val protein100g: Double,
    val additivesCount: Int,
    val additivesTags: List<String>,
    val ingredientsText: String
)

/**
 * Free, no-key, global product database — perfect for junk-food tracking.
 * https://openfoodfacts.org
 */
object OpenFoodFactsService {

    /**
     * Search products by name, optionally filtered by country + category tag.
     * @param query brand + product name text.
     * @param countryCode ISO-2 like "IN" — used to bias to local products.
     * @param categoryTag e.g. "chips", "sodas", "chocolates" (English facet); "" to skip.
     */
    suspend fun search(
        query: String,
        countryCode: String = "",
        categoryTag: String = "",
        limit: Int = 15
    ): List<OffProduct> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val countryParam = if (countryCode.isNotBlank()) {
                // Country facet: search only products marketed in this country
                "&tagtype_0=countries&tag_contains_0=contains&tag_0=" +
                    URLEncoder.encode(countryCodeToTag(countryCode), "UTF-8")
            } else ""
            val categoryParam = if (categoryTag.isNotBlank()) {
                "&tagtype_1=categories&tag_contains_1=contains&tag_1=" + URLEncoder.encode(categoryTag, "UTF-8")
            } else ""

            val url = URL(
                "https://world.openfoodfacts.org/cgi/search.pl" +
                    "?search_terms=$q" +
                    "&search_simple=1&action=process&json=1&page_size=$limit" +
                    "&fields=code,product_name,brands,categories,image_small_url," +
                    "countries,quantity,nutriscore_grade,nova_group," +
                    "nutriments,additives_n,additives_tags,ingredients_text" +
                    countryParam +
                    categoryParam
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("User-Agent", "Unsigned-App Android (personal-tracker)")
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val products = json.optJSONArray("products") ?: return@withContext emptyList()
            (0 until products.length()).mapNotNull { i -> parseProduct(products.getJSONObject(i)) }
        } catch (_: Exception) { emptyList() }
    }

    /** Fetch a single product by barcode/id. */
    suspend fun productById(id: String): OffProduct? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/${URLEncoder.encode(id, "UTF-8")}.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("User-Agent", "Unsigned-App Android (personal-tracker)")
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val product = json.optJSONObject("product") ?: return@withContext null
            parseProduct(product)
        } catch (_: Exception) { null }
    }

    private fun parseProduct(o: JSONObject): OffProduct? {
        val id = o.optString("code", o.optString("_id", ""))
        val name = o.optString("product_name", "").ifBlank {
            o.optString("generic_name", "").ifBlank { return null }
        }
        val nutriments = o.optJSONObject("nutriments") ?: JSONObject()
        return OffProduct(
            id = id,
            productName = name,
            brand = o.optString("brands", ""),
            category = firstOf(o.optString("categories", "")),
            imageUrl = o.optString("image_small_url", ""),
            country = firstOf(o.optString("countries", "")),
            quantity = o.optString("quantity", ""),
            nutriscore = o.optString("nutriscore_grade", "").lowercase(),
            novaGroup = o.optInt("nova_group", 0),
            energyKcal100g = nutriments.optDouble("energy-kcal_100g", nutriments.optDouble("energy_100g", 0.0) / 4.184),
            sugar100g       = nutriments.optDouble("sugars_100g", 0.0),
            saturatedFat100g= nutriments.optDouble("saturated-fat_100g", 0.0),
            salt100g        = nutriments.optDouble("salt_100g", nutriments.optDouble("sodium_100g", 0.0) * 2.5),
            fiber100g       = nutriments.optDouble("fiber_100g", 0.0),
            protein100g     = nutriments.optDouble("proteins_100g", 0.0),
            additivesCount  = o.optInt("additives_n", 0),
            additivesTags   = o.optJSONArray("additives_tags")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
            } ?: emptyList(),
            ingredientsText = o.optString("ingredients_text", "").take(600)
        )
    }

    private fun firstOf(csv: String): String = csv.split(',').firstOrNull()?.trim().orEmpty()

    /** Convert country ISO code to Open Food Facts tag (e.g., "IN" → "en:india"). */
    private fun countryCodeToTag(iso: String): String {
        val name = countryName(iso).ifBlank { return "" }
        return "en:" + name.lowercase().replace(' ', '-')
    }

    private fun countryName(iso: String): String = when (iso.uppercase()) {
        "IN" -> "India"; "US" -> "United States"; "GB" -> "United Kingdom"
        "AU" -> "Australia"; "CA" -> "Canada"; "DE" -> "Germany"; "FR" -> "France"
        "IT" -> "Italy"; "ES" -> "Spain"; "JP" -> "Japan"; "CN" -> "China"
        "BR" -> "Brazil"; "MX" -> "Mexico"; "ZA" -> "South Africa"; "RU" -> "Russia"
        "SG" -> "Singapore"; "AE" -> "United Arab Emirates"; "PK" -> "Pakistan"
        "BD" -> "Bangladesh"; "LK" -> "Sri Lanka"; "NP" -> "Nepal"
        "TH" -> "Thailand"; "ID" -> "Indonesia"; "MY" -> "Malaysia"
        "PH" -> "Philippines"; "VN" -> "Vietnam"; "KR" -> "South Korea"
        "NL" -> "Netherlands"; "BE" -> "Belgium"; "CH" -> "Switzerland"
        "SE" -> "Sweden"; "NO" -> "Norway"; "DK" -> "Denmark"; "FI" -> "Finland"
        "IE" -> "Ireland"; "NZ" -> "New Zealand"; "PT" -> "Portugal"; "GR" -> "Greece"
        "PL" -> "Poland"; "AT" -> "Austria"; "TR" -> "Turkey"
        else -> ""
    }

    /** Canonical category tags for the wizard's second step. */
    val foodCategories = listOf(
        "chips" to "Chips & Crisps",
        "chocolates" to "Chocolates",
        "biscuits" to "Biscuits & Cookies",
        "candies" to "Candies & Sweets",
        "ice-creams" to "Ice Cream",
        "fried-foods" to "Fried Foods",
        "instant-noodles" to "Instant Noodles",
        "cereals" to "Sugary Cereals",
        "cakes" to "Cakes & Pastries",
        "fast-foods" to "Fast Food"
    )
    val liquidCategories = listOf(
        "sodas" to "Sodas & Colas",
        "energy-drinks" to "Energy Drinks",
        "juices" to "Fruit Juices",
        "sport-drinks" to "Sports Drinks",
        "milkshakes" to "Milkshakes",
        "alcoholic-beverages" to "Alcoholic Beverages",
        "sweetened-beverages" to "Sweetened Beverages",
        "iced-teas" to "Iced Teas"
    )
}
