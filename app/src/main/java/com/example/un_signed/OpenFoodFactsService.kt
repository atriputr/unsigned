package com.example.un_signed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Collections

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
 *
 * Improvements over vanilla:
 *   • In-memory LRU cache with 10-minute TTL (search + product-by-id + brand suggestions)
 *   • Result-quality scoring — richer products (nutrition + nutriscore + nova + image) rank higher
 *   • Country fallback — if local country returns thin results, retry globally and merge
 *   • Query normalisation — trim / collapse whitespace / lowercase-safe for OFF
 *   • Deduplicated brand suggestions (OFF sometimes returns capital + lowercase duplicates)
 *   • Poor-entry filtering — products without a real name / nutrition are dropped
 */
object OpenFoodFactsService {

    // ── Cache infra ───────────────────────────────────────────
    private const val CACHE_TTL_MS = 10 * 60 * 1000L    // 10 min
    private const val CACHE_MAX_ENTRIES = 60

    private data class CacheEntry<T>(val value: T, val timestamp: Long) {
        fun isFresh(): Boolean = System.currentTimeMillis() - timestamp < CACHE_TTL_MS
    }

    private val searchCache: MutableMap<String, CacheEntry<List<OffProduct>>> =
        Collections.synchronizedMap(object : LinkedHashMap<String, CacheEntry<List<OffProduct>>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry<List<OffProduct>>>?): Boolean =
                size > CACHE_MAX_ENTRIES
        })
    private val productCache: MutableMap<String, CacheEntry<OffProduct?>> =
        Collections.synchronizedMap(object : LinkedHashMap<String, CacheEntry<OffProduct?>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry<OffProduct?>>?): Boolean =
                size > CACHE_MAX_ENTRIES
        })
    private val brandCache: MutableMap<String, CacheEntry<List<String>>> =
        Collections.synchronizedMap(object : LinkedHashMap<String, CacheEntry<List<String>>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, CacheEntry<List<String>>>?): Boolean =
                size > CACHE_MAX_ENTRIES
        })

    /** Clear all caches — call after a user-visible refresh or app language change. */
    fun clearCache() {
        searchCache.clear(); productCache.clear(); brandCache.clear()
    }

    // ── Query helpers ─────────────────────────────────────────
    /** Collapse whitespace, trim, lowercase for cache-key safety (query is not modified for API). */
    private fun normalize(s: String): String = s.trim().replace(Regex("\\s+"), " ")
    private fun cacheKey(query: String, country: String, category: String, limit: Int) =
        "q=${normalize(query).lowercase()}|c=${country.uppercase()}|cat=${category}|l=$limit"

    /**
     * Search products by name, optionally filtered by country + category tag.
     * Cached for 10 min. Falls back to global search if country filter returns few results.
     */
    suspend fun search(
        query: String,
        countryCode: String = "",
        categoryTag: String = "",
        limit: Int = 15
    ): List<OffProduct> = withContext(Dispatchers.IO) {
        val cleanQuery = normalize(query)
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val key = cacheKey(cleanQuery, countryCode, categoryTag, limit)
        searchCache[key]?.takeIf { it.isFresh() }?.let { return@withContext it.value }

        // Brand-only query heuristic: strip category to widen the pool for well-known brands
        val brandKeywords = listOf(
            "red bull", "redbull", "monster", "coca", "pepsi", "mountain dew", "gatorade",
            "doritos", "lays", "pringles", "cheetos", "oreo", "kit kat", "snickers", "mars",
            "cadbury", "nestle", "fanta", "sprite", "7up", "haldirams", "britannia", "parle",
            "amul", "bikaji", "kellogg", "hershey", "ferrero", "unilever"
        )
        val isBrandQuery = brandKeywords.any { cleanQuery.lowercase().contains(it) }
        val effectiveCategory = if (isBrandQuery) "" else categoryTag

        // ── Primary: country-scoped search (if country is set) ─
        val localResults = runSearch(cleanQuery, countryCode, effectiveCategory, limit)
        val ranked = rankAndFilter(localResults, cleanQuery)

        // ── Fallback: retry globally if local returned very few ─
        val merged = if (countryCode.isNotBlank() && ranked.size < (limit / 3).coerceAtLeast(3)) {
            val globalResults = runSearch(cleanQuery, "", effectiveCategory, limit)
            val globalRanked = rankAndFilter(globalResults, cleanQuery)
            // Prefer local — de-dup by id — then append global for coverage
            val seen = mutableSetOf<String>()
            (ranked + globalRanked).filter { p -> p.id.isNotBlank() && seen.add(p.id) }.take(limit)
        } else ranked.take(limit)

        searchCache[key] = CacheEntry(merged, System.currentTimeMillis())
        merged
    }

    /**
     * Live brand-name suggestions from OFF's suggest.pl endpoint.
     * Cached; deduplicates trailing/casing variants.
     */
    suspend fun suggestBrands(term: String): List<String> = withContext(Dispatchers.IO) {
        val cleanTerm = normalize(term)
        if (cleanTerm.length < 2) return@withContext emptyList()

        val key = cleanTerm.lowercase()
        brandCache[key]?.takeIf { it.isFresh() }?.let { return@withContext it.value }

        val fresh = try {
            val t = URLEncoder.encode(cleanTerm, "UTF-8")
            val url = URL("https://world.openfoodfacts.org/cgi/suggest.pl?tagtype=brands&term=$t")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            conn.setRequestProperty("User-Agent", "Unsigned-App Android (personal-tracker)")
            if (conn.responseCode !in 200..299) emptyList()
            else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONArray(body)
                // Deduplicate by lower-case + take first-cased occurrence
                val seen = mutableSetOf<String>()
                (0 until json.length())
                    .mapNotNull { json.optString(it, "").takeIf { s -> s.isNotBlank() } }
                    .filter { seen.add(it.lowercase()) }
                    .take(10)
            }
        } catch (_: Exception) { emptyList() }

        brandCache[key] = CacheEntry(fresh, System.currentTimeMillis())
        fresh
    }

    /** Products for a specific brand — cached, country-fallback, and ranked. */
    suspend fun productsByBrand(
        brand: String,
        countryCode: String = "",
        limit: Int = 30
    ): List<OffProduct> = withContext(Dispatchers.IO) {
        val cleanBrand = normalize(brand)
        if (cleanBrand.isBlank()) return@withContext emptyList()

        val key = "brand:${cleanBrand.lowercase()}|c=${countryCode.uppercase()}|l=$limit"
        searchCache[key]?.takeIf { it.isFresh() }?.let { return@withContext it.value }

        val results = try {
            val b = URLEncoder.encode(cleanBrand, "UTF-8")
            val countryParam = if (countryCode.isNotBlank())
                "&tagtype_1=countries&tag_contains_1=contains&tag_1=" +
                    URLEncoder.encode(countryCodeToTag(countryCode), "UTF-8")
            else ""

            val url = URL(
                "https://world.openfoodfacts.org/cgi/search.pl" +
                    "?tagtype_0=brands&tag_contains_0=contains&tag_0=$b" +
                    "&action=process&json=1&page_size=$limit" +
                    "&fields=code,product_name,brands,categories,image_small_url," +
                    "countries,quantity,nutriscore_grade,nova_group," +
                    "nutriments,additives_n,additives_tags,ingredients_text" +
                    countryParam
            )
            fetchProducts(url)
        } catch (_: Exception) { emptyList() }

        val ranked = rankAndFilter(results, cleanBrand).take(limit)
        searchCache[key] = CacheEntry(ranked, System.currentTimeMillis())
        ranked
    }

    /** Fetch a single product by barcode/id — cached. */
    suspend fun productById(id: String): OffProduct? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        productCache[id]?.takeIf { it.isFresh() }?.let { return@withContext it.value }

        val fresh = try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/${URLEncoder.encode(id, "UTF-8")}.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("User-Agent", "Unsigned-App Android (personal-tracker)")
            if (conn.responseCode !in 200..299) null
            else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                json.optJSONObject("product")?.let { parseProduct(it) }
            }
        } catch (_: Exception) { null }

        productCache[id] = CacheEntry(fresh, System.currentTimeMillis())
        fresh
    }

    // ── Internal helpers ──────────────────────────────────────

    /** Actual HTTP + parse. Never throws — returns empty on any failure. */
    private fun runSearch(
        query: String,
        countryCode: String,
        categoryTag: String,
        limit: Int
    ): List<OffProduct> = try {
        val q = URLEncoder.encode(query, "UTF-8")
        val countryParam = if (countryCode.isNotBlank())
            "&tagtype_0=countries&tag_contains_0=contains&tag_0=" +
                URLEncoder.encode(countryCodeToTag(countryCode), "UTF-8")
        else ""
        val categoryParam = if (categoryTag.isNotBlank())
            "&tagtype_1=categories&tag_contains_1=contains&tag_1=" + URLEncoder.encode(categoryTag, "UTF-8")
        else ""

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
        fetchProducts(url)
    } catch (_: Exception) { emptyList() }

    private fun fetchProducts(url: URL): List<OffProduct> {
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        conn.setRequestProperty("User-Agent", "Unsigned-App Android (personal-tracker)")
        if (conn.responseCode !in 200..299) return emptyList()
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val products = json.optJSONArray("products") ?: return emptyList()
        return (0 until products.length()).mapNotNull { i -> parseProduct(products.getJSONObject(i)) }
    }

    /**
     * Score + sort + dedupe.
     * Higher score = more useful product (has nutrition + nutriscore + nova + image + brand match).
     */
    private fun rankAndFilter(raw: List<OffProduct>, query: String): List<OffProduct> {
        val queryLower = query.lowercase()
        val queryTokens = queryLower.split(' ').filter { it.length >= 2 }
        val seen = mutableSetOf<String>()

        return raw
            .filter { p ->
                // Poor-quality filter: needs a real name
                p.productName.length >= 2 &&
                    p.productName != "-" &&
                    // De-dup by ID
                    (p.id.isBlank() || seen.add(p.id))
            }
            .map { p -> p to score(p, queryLower, queryTokens) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun score(p: OffProduct, queryLower: String, tokens: List<String>): Int {
        var s = 0
        // Exact / prefix / contains match in name
        val name = p.productName.lowercase()
        val brand = p.brand.lowercase()
        if (name == queryLower) s += 100
        if (name.startsWith(queryLower)) s += 40
        if (name.contains(queryLower)) s += 20
        // Token overlap
        tokens.forEach { t ->
            if (t in name) s += 10
            if (t in brand) s += 8
        }
        // Data richness
        if (p.imageUrl.isNotBlank()) s += 8
        if (p.nutriscore.isNotBlank()) s += 6
        if (p.novaGroup in 1..4) s += 6
        if (p.energyKcal100g > 0) s += 4
        if (p.brand.isNotBlank()) s += 4
        if (p.quantity.isNotBlank()) s += 2
        return s
    }

    private fun parseProduct(o: JSONObject): OffProduct? {
        val id = o.optString("code", o.optString("_id", ""))

        // Name resolution — fall through several fields; drop entries with no meaningful name
        val name = firstNonBlank(
            o.optString("product_name"),
            o.optString("product_name_en"),
            o.optString("generic_name"),
            o.optString("abbreviated_product_name")
        )?.trim() ?: return null
        if (name.length < 2 || name == "-" || name.equals("unknown", ignoreCase = true)) return null

        val nutriments = o.optJSONObject("nutriments") ?: JSONObject()
        // Energy: kcal preferred, else convert kJ → kcal
        val energy = nutriments.optDouble("energy-kcal_100g", Double.NaN).let {
            if (it.isNaN()) nutriments.optDouble("energy_100g", 0.0) / 4.184 else it
        }.coerceAtLeast(0.0)

        return OffProduct(
            id = id,
            productName = name,
            brand = o.optString("brands", "").trim(),
            category = firstOf(o.optString("categories", "")),
            imageUrl = o.optString("image_small_url", ""),
            country = firstOf(o.optString("countries", "")),
            quantity = o.optString("quantity", "").trim(),
            nutriscore = o.optString("nutriscore_grade", "").lowercase().let { if (it in listOf("a","b","c","d","e")) it else "" },
            novaGroup = o.optInt("nova_group", 0).coerceIn(0, 4),
            energyKcal100g   = energy,
            sugar100g        = nutriments.optDouble("sugars_100g", 0.0).coerceAtLeast(0.0),
            saturatedFat100g = nutriments.optDouble("saturated-fat_100g", 0.0).coerceAtLeast(0.0),
            salt100g         = nutriments.optDouble("salt_100g", nutriments.optDouble("sodium_100g", 0.0) * 2.5).coerceAtLeast(0.0),
            fiber100g        = nutriments.optDouble("fiber_100g", 0.0).coerceAtLeast(0.0),
            protein100g      = nutriments.optDouble("proteins_100g", 0.0).coerceAtLeast(0.0),
            additivesCount   = o.optInt("additives_n", 0).coerceAtLeast(0),
            additivesTags    = o.optJSONArray("additives_tags")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
            } ?: emptyList(),
            ingredientsText  = o.optString("ingredients_text", "").take(600)
        )
    }

    /** Pick first non-blank value from a variadic list. */
    private fun firstNonBlank(vararg candidates: String?): String? =
        candidates.firstOrNull { !it.isNullOrBlank() }

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
