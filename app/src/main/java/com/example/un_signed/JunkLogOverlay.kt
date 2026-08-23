package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Multi-step wizard for logging a *specific* junk item.
 *
 *   1  Pick type       (Food / Liquid)
 *   2  Pick category   (Chips, Sodas, etc. — filtered by type)
 *   3  Enter brand + product; auto-search Open Food Facts filtered by country
 *   4  Pick a specific product from search results
 *   5  Confirm serving size → save + show personalised health-impact card
 */
@Composable
fun JunkLogOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    profile: UserProfile,
    onSaved: (JunkLogEntry) -> Unit,       // increment simple junk counter too
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(1) }
    var type by remember { mutableStateOf("food") }             // "food" | "liquid"
    var categoryTag by remember { mutableStateOf("") }
    var categoryLabel by remember { mutableStateOf("") }
    var queryText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OffProduct>>(emptyList()) }
    var pickedProduct by remember { mutableStateOf<OffProduct?>(null) }
    var servingGrams by remember { mutableStateOf(30) }
    var impact by remember { mutableStateOf<JunkImpact?>(null) }
    var country by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    // Live typing suggestions for brands
    var brandSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSuggesting by remember { mutableStateOf(false) }
    var suggestJob by remember { mutableStateOf<Job?>(null) }

    // Fetch country once for country-scoped search
    LaunchedEffect(Unit) {
        val loc = LocationHelper.resolve(ctx)
        country = loc?.countryCode ?: ""
    }

    // Debounced live brand suggestions while user types on step 3
    LaunchedEffect(queryText, step) {
        suggestJob?.cancel()
        if (step != 3 || queryText.trim().length < 2) {
            brandSuggestions = emptyList(); isSuggesting = false
            return@LaunchedEffect
        }
        suggestJob = scope.launch {
            delay(350L)
            isSuggesting = true
            brandSuggestions = OpenFoodFactsService.suggestBrands(queryText)
            isSuggesting = false
        }
    }

    GlassCard(
        modifier = Modifier.heightIn(max = 720.dp),
        onClose = onClose
    ) {
        Text(
            text = "LOG JUNK",
            color = palette.onSurface,
            fontSize = 22.sp,
            fontFamily = titleFont,
            fontStyle = FontStyle.Italic,
            letterSpacing = 3.sp,
            style = TextStyle(shadow = Shadow(color = palette.danger.copy(alpha = 0.4f), blurRadius = 10f))
        )
        Text(
            text = when (step) {
                1 -> "Step 1 · What kind?"
                2 -> "Step 2 · Which category?"
                3 -> "Step 3 · Brand + name"
                4 -> "Step 4 · Pick the exact product"
                else -> "Step 5 · Health impact"
            },
            color = palette.subtle,
            fontSize = 11.sp,
            fontFamily = contentFont,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (step) {
                // ────────────────────────────  STEP 1  ────────────
                1 -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TypeChip("🍽 FOOD", selected = type == "food", palette, contentFont, Modifier.weight(1f)) {
                            type = "food"; step = 2
                        }
                        TypeChip("🥤 LIQUID", selected = type == "liquid", palette, contentFont, Modifier.weight(1f)) {
                            type = "liquid"; step = 2
                        }
                    }
                }
                // ────────────────────────────  STEP 2  ────────────
                2 -> {
                    val cats = if (type == "food") OpenFoodFactsService.foodCategories else OpenFoodFactsService.liquidCategories
                    cats.forEach { (tag, label) ->
                        CategoryRow(label = label, selected = categoryTag == tag, palette, contentFont) {
                            categoryTag = tag; categoryLabel = label; step = 3
                        }
                    }
                }
                // ────────────────────────────  STEP 3  ────────────
                3 -> {
                    Text("Category: $categoryLabel", color = palette.subtle, fontSize = 12.sp, fontFamily = contentFont)
                    BasicTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        textStyle = TextStyle(color = palette.onSurface, fontSize = 16.sp, fontFamily = contentFont),
                        cursorBrush = SolidColor(palette.accentPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.fieldBg)
                            .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (queryText.isEmpty()) Text("brand + product…  e.g. Lays classic", color = palette.faint, fontSize = 14.sp, fontFamily = contentFont)
                                inner()
                            }
                        }
                    )

                    // ── Live brand suggestions ──────────────
                    when {
                        queryText.trim().length in 1..1 -> {
                            Text(
                                "keep typing… (min 2 chars for brand search)",
                                color = palette.faint, fontSize = 10.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic
                            )
                        }
                        isSuggesting -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.accentPrimary)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "finding brands…",
                                    color = palette.subtle, fontSize = 10.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic
                                )
                            }
                        }
                        brandSuggestions.isNotEmpty() -> {
                            Text(
                                "TAP A BRAND  ·  to see all local products",
                                color = palette.accentSecondary, fontSize = 9.sp, fontFamily = titleFont, letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                            )
                            brandSuggestions.forEach { b ->
                                BrandSuggestionRow(b, palette, contentFont) {
                                    scope.launch {
                                        queryText = b // set name to original
                                        isSearching = true; errorMsg = ""
                                        results = OpenFoodFactsService.productsByBrand(
                                            brand = b,
                                            countryCode = country
                                        )
                                        isSearching = false
                                        if (results.isEmpty()) errorMsg = "no products found for this brand in $country"
                                        else step = 4
                                    }
                                }
                            }
                        }
                        queryText.trim().length >= 2 && !isSuggesting -> {
                            Text(
                                "no brand matches — try searching by full product name below",
                                color = palette.faint, fontSize = 10.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.danger.copy(alpha = 0.9f))
                            .clickable(enabled = queryText.isNotBlank() && !isSearching) {
                                    Haptics.click(ctx)
                                    scope.launch {
                                        isSearching = true; errorMsg = ""
                                        results = OpenFoodFactsService.search(
                                            query = queryText,
                                            countryCode = country,
                                            categoryTag = categoryTag,
                                            limit = 20
                                        )
                                        isSearching = false
                                        if (results.isEmpty()) errorMsg = "no products found — try broader terms"
                                        else step = 4
                                    }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = palette.accentPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "SEARCH  ›",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = titleFont,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    if (country.isNotBlank()) {
                        Text("filtered to your country: $country", color = palette.faint, fontSize = 10.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
                    }
                    if (errorMsg.isNotBlank()) {
                        Text(errorMsg, color = palette.danger, fontSize = 11.sp, fontFamily = contentFont)
                    }
                }
                // ────────────────────────────  STEP 4  ────────────
                4 -> {
                    Text("${results.size} matches — tap to pick", color = palette.subtle, fontSize = 11.sp, fontFamily = contentFont)
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(results, key = { it.id }) { p ->
                            ResultRow(p, palette, contentFont) {
                                pickedProduct = p
                                servingGrams = if (type == "liquid") 250 else 30
                                impact = JunkImpactAnalyzer.analyse(p, servingGrams, profile)
                                step = 5
                            }
                        }
                    }
                }
                // ────────────────────────────  STEP 5  ────────────
                5 -> {
                    val p = pickedProduct
                    val im = impact
                    if (p != null && im != null) {
                        // Product header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.chipBg)
                                .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (p.brand.isNotBlank()) Text(p.brand.uppercase(), color = palette.accentSecondary, fontSize = 10.sp, fontFamily = titleFont, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                            Text(p.productName, color = palette.onSurface, fontSize = 15.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                NutriChip("NUTRI-${p.nutriscore.uppercase().ifBlank { "?" }}", nutriscoreColor(p.nutriscore))
                                if (p.novaGroup > 0) NutriChip("NOVA ${p.novaGroup}", novaColor(p.novaGroup))
                            }
                        }

                        // Serving size adjuster
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (type == "liquid") "SERVING (ml)" else "SERVING (g)", color = palette.subtle, fontSize = 11.sp, fontFamily = titleFont, letterSpacing = 2.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                listOf(-50, -10, +10, +50).forEach { delta ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(palette.chipBg)
                                            .border(1.dp, palette.fieldBorder, RoundedCornerShape(6.dp))
                                            .clickable {
                                                servingGrams = (servingGrams + delta).coerceIn(5, 1000)
                                                impact = JunkImpactAnalyzer.analyse(p, servingGrams, profile)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(if (delta > 0) "+$delta" else "$delta", color = palette.onSurface, fontSize = 10.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text("${servingGrams}${if (type == "liquid") "ml" else "g"}", color = palette.onSurface, fontSize = 15.sp, fontFamily = NokiaFont, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                            }
                        }

                        // Impact headline
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(severityBg(im.overallSeverity, palette))
                                .border(1.5.dp, severityColor(im.overallSeverity), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(im.headline, color = palette.onSurface, fontSize = 13.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold)
                            if (im.burnoffMinutes > 0) {
                                Text("~${im.burnoffMinutes} min brisk walk to burn this off", color = palette.subtle, fontSize = 11.sp, fontFamily = contentFont)
                            }
                        }

                        // Detailed impact lines
                        im.lines.forEach { line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.chipBg)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(severityColor(line.severity))
                                        .padding(top = 4.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(line.system.uppercase(), color = severityColor(line.severity), fontSize = 9.sp, fontFamily = titleFont, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                                    Text(line.message, color = palette.onSurface, fontSize = 12.sp, fontFamily = contentFont, lineHeight = 16.sp)
                                    Text("src · ${line.evidence}", color = palette.faint, fontSize = 9.sp, fontFamily = contentFont, fontStyle = FontStyle.Italic)
                                }
                            }
                        }

                        // Save button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.danger)
                                .clickable {
                                    val entry = JunkLogEntry(
                                        dateIso = LocalDate.now().toString(),
                                        type = type,
                                        category = categoryTag,
                                        brand = p.brand,
                                        productName = p.productName,
                                        productId = p.id,
                                        servingGrams = servingGrams,
                                        country = country,
                                        nutriscore = p.nutriscore,
                                        novaGroup = p.novaGroup,
                                        kcal = (p.energyKcal100g * servingGrams / 100.0).toInt(),
                                        sugarG = p.sugar100g * servingGrams / 100.0,
                                        satFatG = p.saturatedFat100g * servingGrams / 100.0,
                                        saltG = p.salt100g * servingGrams / 100.0,
                                        additivesCount = p.additivesCount,
                                        overallSeverity = im.overallSeverity.name
                                    )
                                    FitDataRepository.addJunkLogEntry(entry)
                                    onSaved(entry)
                                    onClose()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SAVE + COUNT", color = Color.White, fontSize = 14.sp, fontFamily = titleFont, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (step > 1) {
                Text(
                    "‹ BACK",
                    color = palette.subtle, fontSize = 13.sp, fontFamily = titleFont, letterSpacing = 2.sp,
                    modifier = Modifier.clickable { 
                        Haptics.click(ctx)
                        step = (step - 1).coerceAtLeast(1) 
                    }
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Text(
                "CLOSE",
                color = palette.faint,
                fontSize = 13.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable { 
                    Haptics.click(ctx)
                    onClose() 
                }
            )
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, palette: ThemePalette, font: FontFamily, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) palette.accentPrimary.copy(alpha = 0.25f) else palette.chipBg)
            .border(1.5.dp, if (selected) palette.accentPrimary else palette.fieldBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = palette.onSurface, fontSize = 15.sp, fontFamily = font, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
private fun CategoryRow(label: String, selected: Boolean, palette: ThemePalette, font: FontFamily, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.accentPrimary.copy(alpha = 0.20f) else palette.chipBg)
            .border(1.dp, if (selected) palette.accentPrimary else palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.onSurface, fontSize = 14.sp, fontFamily = font, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("›", color = palette.subtle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BrandSuggestionRow(brand: String, palette: ThemePalette, font: FontFamily, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(palette.accentPrimary)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = brand, 
            color = palette.onSurface, 
            fontSize = 14.sp, 
            fontFamily = font, 
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Text("›", color = palette.subtle, fontSize = 16.sp, fontFamily = font)
    }
}

@Composable
private fun ResultRow(p: OffProduct, palette: ThemePalette, font: FontFamily, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        if (p.brand.isNotBlank()) Text(p.brand.uppercase(), color = palette.accentSecondary, fontSize = 9.sp, fontFamily = font, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(p.productName, color = palette.onSurface, fontSize = 13.sp, fontFamily = font, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            if (p.nutriscore.isNotBlank()) NutriChip("NUTRI-${p.nutriscore.uppercase()}", nutriscoreColor(p.nutriscore))
            if (p.novaGroup > 0) NutriChip("NOVA ${p.novaGroup}", novaColor(p.novaGroup))
            if (p.quantity.isNotBlank()) Text(p.quantity, color = palette.faint, fontSize = 10.sp, fontFamily = font)
        }
    }
}

@Composable
private fun NutriChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

private fun nutriscoreColor(grade: String): Color = when (grade.lowercase()) {
    "a" -> Color(0xFF00B050); "b" -> Color(0xFF7ECC00)
    "c" -> Color(0xFFFFD400); "d" -> Color(0xFFFF8C1A); "e" -> Color(0xFFE41417)
    else -> Color(0xFFAAAAAA)
}

private fun novaColor(g: Int): Color = when (g) {
    1 -> Color(0xFF8CD86A); 2 -> Color(0xFFCCE9BC)
    3 -> Color(0xFFFFC848); 4 -> Color(0xFFE41417)
    else -> Color(0xFFAAAAAA)
}

private fun severityColor(s: ImpactLine.Severity): Color = when (s) {
    ImpactLine.Severity.Ok -> Color(0xFF8CD86A)
    ImpactLine.Severity.Watch -> Color(0xFFFFC848)
    ImpactLine.Severity.Bad -> Color(0xFFFF8C1A)
    ImpactLine.Severity.Critical -> Color(0xFFE41417)
}

private fun severityBg(s: ImpactLine.Severity, palette: ThemePalette): Color = when (s) {
    ImpactLine.Severity.Ok -> Color(0xFF8CD86A).copy(alpha = 0.12f)
    ImpactLine.Severity.Watch -> Color(0xFFFFC848).copy(alpha = 0.12f)
    ImpactLine.Severity.Bad -> Color(0xFFFF8C1A).copy(alpha = 0.15f)
    ImpactLine.Severity.Critical -> Color(0xFFE41417).copy(alpha = 0.15f)
}
