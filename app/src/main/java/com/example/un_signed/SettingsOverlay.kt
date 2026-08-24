package com.example.un_signed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    prefs: AppPreferences,
    onPrefsChange: (AppPreferences) -> Unit,
    onEditProfile: () -> Unit,
    onWeightLog: () -> Unit,
    onCheckUpdate: () -> Unit,
    onShowUpdateLog: () -> Unit,
    onChangeLanguage: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    var current by remember { mutableStateOf(prefs) }

    fun update(p: AppPreferences) {
        current = p
        onPrefsChange(p)
        Haptics.tick(ctx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.borderBrush(), RoundedCornerShape(24.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                LocalStrings.current.settings,
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = OrangeFire.copy(alpha = 0.5f), blurRadius = 10f))
            )
            Text(
                LocalStrings.current.preferencesSummary,
                color = palette.subtle,
                fontSize = 11.sp,
                fontFamily = contentFont,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Theme ────────────────────────────────
                SectionHeader(LocalStrings.current.theme, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Value is the canonical KEY (DARK / CREAM / AMBER) — never localised.
                    // The visible label uses the localised string. This keeps AppPalettes.byName() working
                    // in every language.
                    listOf(
                        "DARK"  to LocalStrings.current.dark,
                        "CREAM" to LocalStrings.current.cream,
                        "AMBER" to LocalStrings.current.amber
                    ).forEach { (key, label) ->
                        Toggle(label.take(5), current.theme == key, contentFont, palette, Modifier.weight(1f)) {
                            update(current.copy(theme = key))
                        }
                    }
                }

                // ── Units ────────────────────────────────
                SectionHeader(LocalStrings.current.weightLabel, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.kg, current.weightUnit == "kg", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(weightUnit = "kg"))
                    }
                    Toggle(LocalStrings.current.lb, current.weightUnit == "lb", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(weightUnit = "lb"))
                    }
                }

                SectionHeader(LocalStrings.current.heightLabel, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.cm, current.heightUnit == "cm", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(heightUnit = "cm"))
                    }
                    Toggle(LocalStrings.current.inches, current.heightUnit == "in", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(heightUnit = "in"))
                    }
                }

                SectionHeader(LocalStrings.current.temperature, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.celsius, current.tempUnit == "C", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(tempUnit = "C"))
                    }
                    Toggle(LocalStrings.current.fahrenheit, current.tempUnit == "F", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(tempUnit = "F"))
                    }
                }

                SectionHeader(LocalStrings.current.distance, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.km, current.distanceUnit == "km", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(distanceUnit = "km"))
                    }
                    Toggle(LocalStrings.current.miles, current.distanceUnit == "mi", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(distanceUnit = "mi"))
                    }
                }

                SectionHeader(LocalStrings.current.volume, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.litres, current.volumeUnit == "L", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(volumeUnit = "L"))
                    }
                    Toggle(LocalStrings.current.ounces, current.volumeUnit == "oz", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(volumeUnit = "oz"))
                    }
                }

                SectionHeader(LocalStrings.current.weekStarts, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.mon, current.startOfWeek == "MONDAY", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(startOfWeek = "MONDAY"))
                    }
                    Toggle(LocalStrings.current.sun, current.startOfWeek == "SUNDAY", contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(startOfWeek = "SUNDAY"))
                    }
                }

                SectionHeader(LocalStrings.current.haptics, palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.on, current.hapticsEnabled, contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(hapticsEnabled = true))
                    }
                    Toggle(LocalStrings.current.off, !current.hapticsEnabled, contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(hapticsEnabled = false))
                    }
                }

                Spacer(Modifier.height(4.dp))

                SectionHeader("PROFILE", palette)
                ActionButton(LocalStrings.current.editProfile, contentFont, palette) {
                    Haptics.click(ctx); onEditProfile()
                }
                ActionButton(LocalStrings.current.weightLog, contentFont, palette) {
                    Haptics.click(ctx); onWeightLog()
                }

                Spacer(Modifier.height(4.dp))

                SectionHeader(LocalStrings.current.system, palette)
                ActionButton(
                    label = "${LocalStrings.current.preferredLanguage} (${Localization.languages[current.languageCode] ?: "English"})",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx); onChangeLanguage()
                }
                ActionButton(LocalStrings.current.checkUpdate, contentFont, palette) {
                    Haptics.click(ctx); onCheckUpdate()
                }
                ActionButton(LocalStrings.current.updateLog, contentFont, palette) {
                    Haptics.click(ctx); onShowUpdateLog()
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(LocalStrings.current.done, color = palette.faint, fontSize = 14.sp, fontFamily = titleFont, letterSpacing = 2.sp, modifier = Modifier.clickable { onClose() })
        }
    }
}

@Composable
private fun SectionHeader(text: String, palette: ThemePalette) {
    Text(text, color = palette.accentPrimary, fontSize = 10.sp, fontFamily = BebasFont, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Toggle(label: String, selected: Boolean, font: FontFamily, palette: ThemePalette, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OrangeFire.copy(alpha = 0.35f) else palette.chipBg)
            .border(1.dp, if (selected) OrangeFire else palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) palette.onSurface else palette.subtle,
            fontSize = 12.sp,
            fontFamily = font,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ActionButton(label: String, font: FontFamily, palette: ThemePalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.chipBg)
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = palette.onSurface, fontSize = 14.sp, fontFamily = font, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Text("›", color = palette.subtle, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
