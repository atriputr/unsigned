package com.example.un_signed

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Named colour roles used by adaptive overlays (settings, water, sleep, briefing, weight, focus). */
data class ThemePalette(
    val name: String,
    val isLight: Boolean,
    val scrim: Color,              // background dim under overlay
    val appBackground: Color,      // whole-app background wash (behind everything)
    val homeTint: Color,           // colour multiplier over the baked home image
    val surfaceTop: Color,         // gradient top of glass card
    val surfaceBot: Color,         // gradient bottom of glass card
    val border: Color,             // border color of glass card
    val onSurface: Color,          // primary text
    val subtle: Color,             // secondary text
    val faint: Color,              // tertiary text
    val divider: Color,
    val chipBg: Color,             // pill / chip background
    val fieldBg: Color,            // text-field background
    val fieldBorder: Color,
    val accentPrimary: Color,      // primary interactive accent (mostly OrangeFire-family)
    val accentSecondary: Color,    // secondary accent
    val danger: Color,             // errors / destructive
    val success: Color,            // positive
    val statusBar: Color           // status bar color
) {
    /** Card surface gradient */
    fun surfaceBrush(): Brush = Brush.verticalGradient(listOf(surfaceTop, surfaceBot))
    fun borderBrush(): Brush  = Brush.verticalGradient(listOf(border, Color.Transparent, border.copy(alpha = 0.5f)))
}

object AppPalettes {
    val Dark = ThemePalette(
        name       = "DARK",
        isLight    = false,
        scrim      = Color(0xFF000000).copy(alpha = 0.75f),
        appBackground = Color(0xFF000000),
        homeTint      = Color.Transparent,
        surfaceTop = Color(0xFF1A1A28).copy(alpha = 0.96f),
        surfaceBot = Color(0xFF0B0B14).copy(alpha = 0.96f),
        border     = Color.White.copy(alpha = 0.40f),
        onSurface  = Color(0xFFFFFFFF),
        subtle     = Color.White.copy(alpha = 0.65f),
        faint      = Color.White.copy(alpha = 0.40f),
        divider    = Color.White.copy(alpha = 0.12f),
        chipBg     = Color.White.copy(alpha = 0.06f),
        fieldBg    = Color.White.copy(alpha = 0.08f),
        fieldBorder= Color.White.copy(alpha = 0.20f),
        accentPrimary   = Color(0xFFFF8A00),
        accentSecondary = Color(0xFFEBC174),
        danger  = Color(0xFFE41417),
        success = Color(0xFF09E8AD),
        statusBar = Color(0xFF333333).copy(alpha = 0.4f) // Grayish translucent
    )

    val Cream = ThemePalette(
        name       = "CREAM",
        isLight    = true,
        scrim      = Color(0xFF302818).copy(alpha = 0.55f),
        appBackground = Color(0xFFF7EFDD),
        homeTint      = Color(0xFFFCF8EF).copy(alpha = 0.86f),  // strong cream wash over dark image
        surfaceTop = Color(0xFFFCF8EF),
        surfaceBot = Color(0xFFF1E9D6),
        border     = Color(0xFFAA8E58).copy(alpha = 0.55f),
        onSurface  = Color(0xFF221A0E),
        subtle     = Color(0xFF6C5638),
        faint      = Color(0xFF9C875F),
        divider    = Color(0xFF221A0E).copy(alpha = 0.10f),
        chipBg     = Color(0xFF221A0E).copy(alpha = 0.05f),
        fieldBg    = Color(0xFFFFFFFF).copy(alpha = 0.65f),
        fieldBorder= Color(0xFF221A0E).copy(alpha = 0.18f),
        accentPrimary   = Color(0xFFD46A00),
        accentSecondary = Color(0xFFA05A20),
        danger  = Color(0xFFB01418),
        success = Color(0xFF1E8B4B),
        statusBar = Color(0xFFFFB6C1).copy(alpha = 0.4f) // Pinkish translucent
    )

    val Amber = ThemePalette(
        name       = "AMBER",
        isLight    = false,
        scrim      = Color(0xFF1B0F02).copy(alpha = 0.80f),
        appBackground = Color(0xFF14090A),
        homeTint      = Color(0xFF2A1408).copy(alpha = 0.55f),  // warm sepia wash
        surfaceTop = Color(0xFF2A1C0A).copy(alpha = 0.96f),
        surfaceBot = Color(0xFF14090A).copy(alpha = 0.96f),
        border     = Color(0xFFFFB454).copy(alpha = 0.55f),
        onSurface  = Color(0xFFFFD79A),
        subtle     = Color(0xFFF3B672).copy(alpha = 0.80f),
        faint      = Color(0xFFA6773B).copy(alpha = 0.65f),
        divider    = Color(0xFFFFB454).copy(alpha = 0.15f),
        chipBg     = Color(0xFFFFB454).copy(alpha = 0.08f),
        fieldBg    = Color(0xFF3A2A15).copy(alpha = 0.55f),
        fieldBorder= Color(0xFFFFB454).copy(alpha = 0.28f),
        accentPrimary   = Color(0xFFFFB454),
        accentSecondary = Color(0xFFFFD79A),
        danger  = Color(0xFFFF7770),
        success = Color(0xFFB1E28C),
        statusBar = Color(0xFFFF8C1A).copy(alpha = 0.4f) // Amber translucent
    )

    fun byName(name: String): ThemePalette = when (name.uppercase()) {
        "CREAM" -> Cream
        "AMBER" -> Amber
        else    -> Dark
    }
}

val LocalPalette = staticCompositionLocalOf { AppPalettes.Dark }

@Composable
fun AppThemeProvider(theme: String, content: @Composable () -> Unit) {
    val palette = AppPalettes.byName(theme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.statusBar.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = palette.isLight
            }
        }
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        content()
    }
}
