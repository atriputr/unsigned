package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserProfileOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    existing: UserProfile,
    prefs: AppPreferences = AppPreferences(),
    onPrefsChange: (AppPreferences) -> Unit = {},
    isOnboarding: Boolean = false,
    onSave: (UserProfile) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current

    // Track units locally so the UI can react instantly to toggles
    var weightUnit by remember { mutableStateOf(prefs.weightUnit) }
    var heightUnit by remember { mutableStateOf(prefs.heightUnit) }

    // Initial displayed values honour the user's chosen units
    val initWeight = when {
        existing.weightKg <= 0 -> ""
        weightUnit == "lb" -> "%.1f".format(Units.kgToLb(existing.weightKg))
        else -> "%.1f".format(existing.weightKg)
    }

    // For height, we keep either a single cm/in string OR split ft & in
    val (initHeight, initHeightFt, initHeightIn) = remember(existing.heightCm, heightUnit) {
        when {
            existing.heightCm <= 0 -> Triple("", "", "")
            heightUnit == "in" -> {
                val (ft, inches) = Units.cmToFeetInches(existing.heightCm)
                Triple("", ft.toString(), inches.toString())
            }
            else -> Triple("%.0f".format(existing.heightCm), "", "")
        }
    }

    var name           by remember { mutableStateOf(existing.name) }
    var age            by remember { mutableStateOf(if (existing.ageYears > 0) existing.ageYears.toString() else "") }
    var gender         by remember { mutableStateOf(existing.gender.ifBlank { "Male" }) }
    var weight         by remember { mutableStateOf(initWeight) }
    var heightCm       by remember { mutableStateOf(initHeight) }
    var heightFt       by remember { mutableStateOf(initHeightFt) }
    var heightIn       by remember { mutableStateOf(initHeightIn) }
    var activityLevel  by remember { mutableStateOf(existing.activityLevel.ifBlank { "Moderate" }) }
    var languageCode   by remember { mutableStateOf(prefs.languageCode) }
    var errorMsg       by remember { mutableStateOf("") }
    var emails         by remember { mutableStateOf(existing.emails) }
    var newEmail       by remember { mutableStateOf("") }
    var onboardingStep by remember { mutableStateOf(if (isOnboarding) 0 else 1) } // 0: Lang, 1: Profile

    /** Convert & carry the currently-entered value over when the user flips the weight unit. */
    fun switchWeightUnit(to: String) {
        if (to == weightUnit) return
        val v = weight.toDoubleOrNull()
        if (v != null && v > 0) {
            weight = if (to == "lb") "%.1f".format(Units.kgToLb(v))
                     else "%.1f".format(Units.lbToKg(v))
        }
        weightUnit = to
        onPrefsChange(prefs.copy(weightUnit = to))
    }

    /** Convert & carry when the user flips the height unit. */
    fun switchHeightUnit(to: String) {
        if (to == heightUnit) return
        if (to == "in") {
            val cm = heightCm.toDoubleOrNull()
            if (cm != null && cm > 0) {
                val (ft, inches) = Units.cmToFeetInches(cm)
                heightFt = ft.toString(); heightIn = inches.toString()
            }
            heightCm = ""
        } else {
            val ft = heightFt.toIntOrNull() ?: 0
            val i  = heightIn.toIntOrNull() ?: 0
            if (ft > 0 || i > 0) heightCm = "%.0f".format(Units.feetInchesToCm(ft, i))
            heightFt = ""; heightIn = ""
        }
        heightUnit = to
        onPrefsChange(prefs.copy(heightUnit = to))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                if (!isOnboarding) onClose()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(width = 1.5.dp, brush = palette.borderBrush(), shape = RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onboardingStep == 0) {
                Text(
                    text = LocalStrings.current.language,
                    color = palette.onSurface,
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic
                )
                if (isOnboarding) {
                    Text(
                        LocalStrings.current.selectPreferredLanguage,
                        color = palette.subtle,
                        fontSize = 12.sp,
                        fontFamily = contentFont,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                    )
                } else {
                    Spacer(Modifier.height(14.dp))
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(Localization.languages.toList().size) { idx ->
                            val (code, name) = Localization.languages.toList()[idx]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (languageCode == code) OrangeFire.copy(alpha = 0.3f) else palette.chipBg)
                                    .border(1.dp, if (languageCode == code) OrangeFire else palette.fieldBorder, RoundedCornerShape(12.dp))
                                    .clickable { 
                                        languageCode = code
                                        onPrefsChange(prefs.copy(languageCode = code))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name, color = palette.onSurface, fontSize = 14.sp, fontFamily = contentFont)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF09e8ad))
                        .clickable { onboardingStep = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(LocalStrings.current.next, color = Color.Black, fontSize = 18.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = if (isOnboarding) LocalStrings.current.welcome else LocalStrings.current.yourProfile,
                    color = palette.onSurface,
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = palette.onSurface.copy(alpha = 0.3f), blurRadius = 8f))
                )
            if (isOnboarding) {
                Text(
                    LocalStrings.current.tellUsAboutYourself,
                    color = palette.subtle,
                    fontSize = 12.sp,
                    fontFamily = contentFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )
            } else {
                Spacer(Modifier.height(14.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabeledField(LocalStrings.current.name, name, palette, contentFont, keyboard = KeyboardType.Text) { name = it }
                LabeledField(LocalStrings.current.ageYears, age, palette, contentFont, keyboard = KeyboardType.Number) {
                    if (it.length <= 3 && it.all { c -> c.isDigit() }) age = it
                }

                Text(LocalStrings.current.gender, color = palette.subtle, fontSize = 12.sp, fontFamily = titleFont, letterSpacing = 2.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(LocalStrings.current.male, LocalStrings.current.female, LocalStrings.current.other).forEach { g ->
                        ChoicePill(g, gender == g, contentFont, palette, Modifier.weight(1f)) { gender = g }
                    }
                }

                // WEIGHT — label + inline unit toggle
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(LocalStrings.current.weight, color = palette.subtle, fontSize = 12.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    UnitSwitch(
                        left = LocalStrings.current.kg, leftSelected = weightUnit == "kg",
                        right = LocalStrings.current.lb, rightSelected = weightUnit == "lb",
                        font = contentFont, palette = palette,
                        onLeft = { switchWeightUnit("kg") },
                        onRight = { switchWeightUnit("lb") }
                    )
                }
                Spacer(Modifier.height(4.dp))
                LabeledField(label = "", value = weight, palette = palette, font = contentFont, keyboard = KeyboardType.Decimal) {
                    if (it.matches(Regex("^\\d{0,4}(\\.\\d{0,1})?$"))) weight = it
                }

                // HEIGHT — label + inline unit toggle; two fields if "in"
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("HEIGHT", color = palette.subtle, fontSize = 12.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    UnitSwitch(
                        left = "CM", leftSelected = heightUnit == "cm",
                        right = "FT / IN", rightSelected = heightUnit == "in",
                        font = contentFont, palette = palette,
                        onLeft = { switchHeightUnit("cm") },
                        onRight = { switchHeightUnit("in") }
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (heightUnit == "in") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            LabeledField(label = "FT", value = heightFt, palette = palette, font = contentFont, keyboard = KeyboardType.Number) {
                                if (it.length <= 1 && it.all { c -> c.isDigit() }) heightFt = it
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            LabeledField(label = "IN", value = heightIn, palette = palette, font = contentFont, keyboard = KeyboardType.Number) {
                                val v = it.toIntOrNull()
                                if (it.isEmpty() || (v != null && v in 0..11)) heightIn = it
                            }
                        }
                    }
                } else {
                    LabeledField(label = "", value = heightCm, palette = palette, font = contentFont, keyboard = KeyboardType.Decimal) {
                        if (it.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) heightCm = it
                    }
                }

                Text(LocalStrings.current.activityLevel, color = palette.subtle, fontSize = 12.sp, fontFamily = titleFont, letterSpacing = 2.sp)
                val levels = listOf(
                    "Sedentary" to "Little/no exercise",
                    "Light" to "1–3 days/week",
                    "Moderate" to "3–5 days/week",
                    "Active" to "6–7 days/week",
                    "VeryActive" to "Physical job / 2x/day"
                )
                levels.forEach { (key, desc) ->
                    ActivityRow(
                        selected = activityLevel == key,
                        title = if (key == "VeryActive") "VERY ACTIVE" else key.uppercase(),
                        subtitle = desc,
                        font = contentFont,
                        palette = palette,
                        onSelect = { activityLevel = key }
                    )
                }

                // ── IDENTITY (email accounts) ─────────────
                Spacer(Modifier.height(2.dp))
                Text(LocalStrings.current.identity, color = palette.subtle, fontSize = 12.sp, fontFamily = titleFont, letterSpacing = 2.sp)
                Text(LocalStrings.current.linkAccounts, color = palette.faint, fontSize = 10.sp, fontFamily = contentFont)

                // Quick provider chips → prefill the input
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "GMAIL" to "@gmail.com",
                        "TUTA"  to "@tutamail.com",
                        "APPLE" to "@icloud.com",
                        "META"  to "@meta.com",
                        "X"     to "@x.com"
                    ).forEach { (label, suffix) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.chipBg)
                                .border(1.dp, palette.fieldBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    if (!newEmail.contains("@")) newEmail = newEmail + suffix
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = palette.onSurface, fontSize = 9.sp, fontFamily = contentFont, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        textStyle = TextStyle(color = palette.onSurface, fontSize = 14.sp, fontFamily = contentFont),
                        cursorBrush = SolidColor(OrangeFire),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.fieldBg)
                            .border(1.dp, palette.fieldBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (newEmail.isEmpty()) Text(LocalStrings.current.emailPlaceholder, color = palette.faint, fontSize = 13.sp, fontFamily = contentFont)
                                inner()
                            }
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrangeFire.copy(alpha = 0.85f))
                            .clickable {
                                val e = newEmail.trim()
                                if (e.contains("@") && e.contains(".") && e !in emails) {
                                    emails = emails + e; newEmail = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
                if (emails.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        emails.forEach { e ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(palette.chipBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(providerBadge(e), color = OrangeFire, fontSize = 9.sp, fontFamily = titleFont, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(end = 6.dp))
                                Text(e, color = palette.onSurface, fontSize = 12.sp, fontFamily = contentFont, modifier = Modifier.weight(1f))
                                Text("×", color = palette.faint, fontSize = 16.sp, modifier = Modifier.clickable { emails = emails - e }.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }

                // Compute canonical SI values from whatever the user has typed
                val wKgPreview = weight.toDoubleOrNull()
                    ?.let { if (weightUnit == "lb") Units.lbToKg(it) else it } ?: 0.0
                val hCmPreview = when (heightUnit) {
                    "in" -> {
                        val ft = heightFt.toIntOrNull() ?: 0
                        val i  = heightIn.toIntOrNull() ?: 0
                        if (ft > 0 || i > 0) Units.feetInchesToCm(ft, i) else 0.0
                    }
                    else -> heightCm.toDoubleOrNull() ?: 0.0
                }

                // Preview computed metrics
                val previewProfile = UserProfile(
                    name = name,
                    ageYears = age.toIntOrNull() ?: 0,
                    gender = gender,
                    weightKg = wKgPreview,
                    heightCm = hCmPreview,
                    activityLevel = activityLevel
                )
                if (previewProfile.bmi > 0) {
                    Spacer(Modifier.height(4.dp))
                    MetricPreview(previewProfile, prefs.copy(weightUnit = weightUnit, heightUnit = heightUnit), contentFont, palette)
                }
            }

            if (errorMsg.isNotBlank()) {
                Text(
                    errorMsg,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    fontFamily = contentFont,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE41417).copy(alpha = 0.9f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable {
                        val ageInt = age.toIntOrNull() ?: 0
                        val wRaw = weight.toDoubleOrNull() ?: 0.0
                        val wKg = if (weightUnit == "lb") Units.lbToKg(wRaw) else wRaw
                        val hCm = when (heightUnit) {
                            "in" -> {
                                val ft = heightFt.toIntOrNull() ?: 0
                                val i  = heightIn.toIntOrNull() ?: 0
                                if (ft == 0 && i == 0) 0.0 else Units.feetInchesToCm(ft, i)
                            }
                            else -> heightCm.toDoubleOrNull() ?: 0.0
                        }
                        errorMsg = when {
                            name.isBlank() -> "Please enter your name."
                            ageInt !in 5..120 -> "Enter a valid age (5-120)."
                            wKg !in 20.0..300.0 -> "Enter a valid weight."
                            hCm !in 60.0..250.0 -> "Enter a valid height."
                            else -> ""
                        }
                        if (errorMsg.isBlank()) {
                            onSave(
                                UserProfile(
                                    name = name.trim(),
                                    ageYears = ageInt,
                                    gender = gender,
                                    weightKg = wKg,
                                    heightCm = hCm,
                                    activityLevel = activityLevel,
                                    setupComplete = true,
                                    createdAt = if (existing.createdAt > 0) existing.createdAt else System.currentTimeMillis(),
                                    emails = emails
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isOnboarding) LocalStrings.current.begin else LocalStrings.current.save,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    fontFamily = titleFont
                )
            }

            if (!isOnboarding) {
                Spacer(Modifier.height(10.dp))
                Text(
                    LocalStrings.current.cancel,
                    color = palette.faint,
                    fontSize = 14.sp,
                    fontFamily = titleFont,
                    letterSpacing = 2.sp,
                    modifier = Modifier.clickable { onClose() }
                )
            }
        }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    palette: ThemePalette,
    font: FontFamily,
    keyboard: KeyboardType,
    onChange: (String) -> Unit
) {
    Column {
        if (label.isNotBlank()) {
            Text(label, color = palette.subtle, fontSize = 12.sp, fontFamily = font, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            textStyle = TextStyle(color = palette.onSurface, fontSize = 18.sp, fontFamily = font),
            cursorBrush = SolidColor(OrangeFire),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.fieldBg)
                .border(1.dp, palette.fieldBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("—", color = palette.faint, fontSize = 18.sp, fontFamily = font)
                inner()
            }
        )
    }
}

@Composable
private fun UnitSwitch(
    left: String,
    leftSelected: Boolean,
    right: String,
    rightSelected: Boolean,
    font: FontFamily,
    palette: ThemePalette,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        UnitPill(left, leftSelected, font, palette, onLeft)
        UnitPill(right, rightSelected, font, palette, onRight)
    }
}

@Composable
private fun UnitPill(label: String, selected: Boolean, font: FontFamily, palette: ThemePalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) OrangeFire else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.Black else palette.subtle,
            fontSize = 10.sp,
            fontFamily = font,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    font: FontFamily,
    palette: ThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OrangeFire.copy(alpha = 0.35f) else palette.chipBg)
            .border(1.dp, if (selected) OrangeFire else palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label.uppercase(),
            color = if (selected) palette.onSurface else palette.subtle,
            fontSize = 13.sp,
            fontFamily = font,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ActivityRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    font: FontFamily,
    palette: ThemePalette,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OrangeFire.copy(alpha = 0.18f) else palette.chipBg)
            .border(1.dp, if (selected) OrangeFire else palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = palette.onSurface, fontSize = 15.sp, fontFamily = font, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(subtitle, color = palette.faint, fontSize = 11.sp, fontFamily = font)
        }
        if (selected) Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(OrangeFire))
    }
}

@Composable
private fun MetricPreview(p: UserProfile, prefs: AppPreferences, font: FontFamily, palette: ThemePalette) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.divider, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("PREVIEW", color = OrangeFire, fontSize = 10.sp, fontFamily = font, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        MetricRow("BMI", "%.1f (%s)".format(p.bmi, p.bmiCategory), font, palette)
        if (p.bmr > 0) MetricRow("BMR", "%.0f kcal/day".format(p.bmr), font, palette)
        if (p.tdee > 0) MetricRow("TDEE", "%.0f kcal/day".format(p.tdee), font, palette)
        MetricRow("Water baseline", Units.displayVolume(p.baseWaterMl, prefs.volumeUnit) + "/day", font, palette)
        MetricRow("Sleep target", "%.1f hr".format(p.recommendedSleepHours), font, palette)
    }
}

@Composable
private fun MetricRow(label: String, value: String, font: FontFamily, palette: ThemePalette) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = palette.subtle, fontSize = 12.sp, fontFamily = font)
        Text(value, color = palette.onSurface, fontSize = 12.sp, fontFamily = font, fontWeight = FontWeight.Bold)
    }
}

private fun providerBadge(email: String): String {
    val lower = email.lowercase()
    return when {
        lower.endsWith("@gmail.com") -> "GMAIL"
        lower.endsWith("@tutamail.com") || lower.endsWith("@tuta.io") -> "TUTA"
        lower.endsWith("@icloud.com") || lower.endsWith("@me.com") -> "APPLE"
        lower.endsWith("@meta.com") || lower.endsWith("@facebook.com") -> "META"
        lower.endsWith("@x.com") || lower.endsWith("@twitter.com") -> "X"
        lower.endsWith("@yahoo.com") -> "YAHOO"
        lower.endsWith("@outlook.com") || lower.endsWith("@hotmail.com") -> "MSFT"
        lower.endsWith("@protonmail.com") || lower.endsWith("@proton.me") -> "PROTON"
        else -> "MAIL"
    }
}
