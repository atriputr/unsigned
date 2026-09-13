package com.example.un_signed

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.core.content.ContextCompat

@Composable
fun SettingsOverlay(
    titleFont: FontFamily,
    contentFont: FontFamily,
    prefs: AppPreferences,
    onPrefsChange: (AppPreferences) -> Unit,
    onEditProfile: () -> Unit,
    onWeightLog: () -> Unit,
    onVitalsLog: () -> Unit,
    onCheckUpdate: () -> Unit,
    onShowUpdateLog: () -> Unit,
    onChangeLanguage: () -> Unit,
    hasCalendarPermission: Boolean = false,
    hasReminderFitnessPermission: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    onRequestReminderFitnessPermission: () -> Unit = {},
    onClose: () -> Unit
) {
    val palette = LocalPalette.current
    val ctx = LocalContext.current
    var current by remember { mutableStateOf(prefs) }
    var showMyPermissions by remember { mutableStateOf(false) }

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

                SectionHeader("PERMISSIONS", palette)
                ActionButton(
                    label = "Calendar Sync — ${if (hasCalendarPermission) "Granted" else "Tap to grant"}",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx); onRequestCalendarPermission()
                }
                ActionButton(
                    label = "Reminders & Fitness — ${if (hasReminderFitnessPermission) "Granted" else "Tap to grant"}",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx); onRequestReminderFitnessPermission()
                }

                SectionHeader("SYNC TO PHONE", palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.on, current.syncTasksToPhoneCalendar, contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(syncTasksToPhoneCalendar = true))
                    }
                    Toggle(LocalStrings.current.off, !current.syncTasksToPhoneCalendar, contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(syncTasksToPhoneCalendar = false))
                    }
                }
                Text("Mirror tasks into your phone's Calendar app", color = palette.subtle, fontSize = 10.sp, fontFamily = contentFont)

                SectionHeader("SYNC TO ALARMS", palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Toggle(LocalStrings.current.on, current.syncTasksToPhoneAlarms, contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(syncTasksToPhoneAlarms = true))
                    }
                    Toggle(LocalStrings.current.off, !current.syncTasksToPhoneAlarms, contentFont, palette, Modifier.weight(1f)) {
                        update(current.copy(syncTasksToPhoneAlarms = false))
                    }
                }
                Text("Add timed tasks to your phone's Alarm app", color = palette.subtle, fontSize = 10.sp, fontFamily = contentFont)

                SectionHeader("POMODORO REMINDER INTERVAL", palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30).forEach { mins ->
                        Toggle("$mins m", current.pomodoroReminderIntervalMin == mins, contentFont, palette, Modifier.weight(1f)) {
                            update(current.copy(pomodoroReminderIntervalMin = mins))
                        }
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
                ActionButton("BP & Glucose", contentFont, palette) {
                    Haptics.click(ctx); onVitalsLog()
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
                ActionButton("MY PERMISSIONS", contentFont, palette) {
                    Haptics.click(ctx); showMyPermissions = true
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

        if (showMyPermissions) {
            MyPermissionsDialog(
                titleFont = titleFont,
                contentFont = contentFont,
                palette = palette,
                onRequestLocationPermission = onRequestLocationPermission,
                onRequestCalendarPermission = onRequestCalendarPermission,
                onRequestReminderFitnessPermission = onRequestReminderFitnessPermission,
                onClose = { showMyPermissions = false }
            )
        }
    }
}

@Composable
private fun MyPermissionsDialog(
    titleFont: FontFamily,
    contentFont: FontFamily,
    palette: ThemePalette,
    onRequestLocationPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestReminderFitnessPermission: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current

    val hasLocation = remember {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val hasCalendar = remember { PermissionsManager.hasCalendarPermission(ctx) }
    val hasNotifications = remember { PermissionsManager.hasNotificationPermission(ctx) }
    val hasActivity = remember { PermissionsManager.hasActivityRecognitionPermission(ctx) }
    val hasExactAlarm = remember { PermissionsManager.canScheduleExactAlarms(ctx) }
    val hasInstall = remember { ctx.packageManager.canRequestPackageInstalls() }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${ctx.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        } catch (_: Exception) { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.surfaceBrush())
                .border(1.5.dp, palette.borderBrush(), RoundedCornerShape(20.dp))
                .clickable(enabled = false) { }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "MY PERMISSIONS",
                color = palette.onSurface,
                fontSize = 22.sp,
                fontFamily = titleFont,
                style = TextStyle(shadow = Shadow(color = OrangeFire.copy(alpha = 0.5f), blurRadius = 8f))
            )
            Text(
                "Tap any item to grant or toggle permission",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionRow(
                    name = "Location Access",
                    granted = hasLocation,
                    description = "Weather, AQI & Location-based goals",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx)
                    if (!hasLocation) onRequestLocationPermission() else openAppSettings()
                }

                PermissionRow(
                    name = "Calendar Read & Write",
                    granted = hasCalendar,
                    description = "Two-way task sync with phone calendar",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx)
                    if (!hasCalendar) onRequestCalendarPermission() else openAppSettings()
                }

                PermissionRow(
                    name = "Notifications",
                    granted = hasNotifications,
                    description = "Task reminders, alarms & update notices",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx)
                    if (!hasNotifications) onRequestReminderFitnessPermission() else openAppSettings()
                }

                PermissionRow(
                    name = "Activity Recognition",
                    granted = hasActivity,
                    description = "Physical step counting & sensor tracking",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx)
                    if (!hasActivity) onRequestReminderFitnessPermission() else openAppSettings()
                }

                PermissionRow(
                    name = "Exact Alarms & Timers",
                    granted = hasExactAlarm,
                    description = "Precise timing for reminders & tasks",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx)
                    if (!hasExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                        } catch (_: Exception) { openAppSettings() }
                    } else openAppSettings()
                }

                PermissionRow(
                    name = "Install Packages",
                    granted = hasInstall,
                    description = "Direct in-app update installation",
                    font = contentFont,
                    palette = palette
                ) {
                    Haptics.click(ctx)
                    if (!hasInstall) {
                        try {
                            ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")))
                        } catch (_: Exception) { openAppSettings() }
                    } else openAppSettings()
                }
            }

            Spacer(Modifier.height(16.dp))

            // Open System Settings button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrangeFire.copy(alpha = 0.25f))
                    .border(1.dp, OrangeFire, RoundedCornerShape(10.dp))
                    .clickable {
                        Haptics.click(ctx)
                        openAppSettings()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "OPEN ALL APP SETTINGS",
                    color = palette.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = titleFont,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "CLOSE",
                color = palette.faint,
                fontSize = 13.sp,
                fontFamily = titleFont,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}

@Composable
private fun PermissionRow(
    name: String,
    granted: Boolean,
    description: String,
    font: FontFamily,
    palette: ThemePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.chipBg)
            .border(1.dp, if (granted) Color(0x664CAF50) else Color(0x66EF5350), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name.uppercase(),
                    color = palette.onSurface,
                    fontSize = 12.sp,
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (granted) Color(0x224CAF50) else Color(0x22EF5350))
                        .border(0.5.dp, if (granted) Color(0xFF4CAF50) else Color(0xFFEF5350), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (granted) "ENABLED ✎" else "TAP TO GRANT ✚",
                        color = if (granted) Color(0xFF81C784) else Color(0xFFFF8A80),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = font,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                description,
                color = palette.subtle,
                fontSize = 10.sp,
                fontFamily = font
            )
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
