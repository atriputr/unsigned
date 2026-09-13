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

    /**
     * GLASS_DARK — Liquid-Glass (Apple) × Aero (Windows Vista) hybrid.
     *  · Deep blue-grey base, high translucent whites
     *  · Chromatic prism borders on cards
     *  · Cyan/violet aero accents
     */
    val GlassDark = ThemePalette(
        name       = "GLASS_DARK",
        isLight    = false,
        scrim      = Color(0xFF03060E).copy(alpha = 0.60f),
        appBackground = Color(0xFF060A18),
        homeTint      = Color(0xFF0A0F22).copy(alpha = 0.35f),
        surfaceTop = Color(0xFFFFFFFF).copy(alpha = 0.13f),
        surfaceBot = Color(0xFF3B5FA8).copy(alpha = 0.10f),
        border     = Color(0xFF8AB8FF).copy(alpha = 0.55f),
        onSurface  = Color(0xFFF4F8FF),
        subtle     = Color(0xFFCCD9F5).copy(alpha = 0.80f),
        faint      = Color(0xFFA6BAE0).copy(alpha = 0.55f),
        divider    = Color.White.copy(alpha = 0.14f),
        chipBg     = Color(0xFF6DA0FF).copy(alpha = 0.08f),
        fieldBg    = Color.White.copy(alpha = 0.10f),
        fieldBorder= Color(0xFF7BB2FF).copy(alpha = 0.35f),
        accentPrimary   = Color(0xFF66D9FF),   // aero cyan
        accentSecondary = Color(0xFFB49CFF),   // liquid violet
        danger  = Color(0xFFFF7B95),
        success = Color(0xFF7BE0B7),
        statusBar = Color(0xFF9BC8FF).copy(alpha = 0.28f)
    )

    /**
     * GLASS_LIGHT — sunlit frosted glass.
     *  · Off-white base with soft blue reflection
     *  · Same prism border, dialled back for daylight
     */
    val GlassLight = ThemePalette(
        name       = "GLASS_LIGHT",
        isLight    = true,
        scrim      = Color(0xFFB4C7E7).copy(alpha = 0.55f),
        appBackground = Color(0xFFEAF1FB),
        homeTint      = Color(0xFFF3F7FE).copy(alpha = 0.75f),
        surfaceTop = Color(0xFFFFFFFF).copy(alpha = 0.72f),
        surfaceBot = Color(0xFFE1EBFA).copy(alpha = 0.72f),
        border     = Color(0xFF3E6FB8).copy(alpha = 0.42f),
        onSurface  = Color(0xFF102341),
        subtle     = Color(0xFF3A4E70).copy(alpha = 0.85f),
        faint      = Color(0xFF6C82A8).copy(alpha = 0.70f),
        divider    = Color(0xFF3A4E70).copy(alpha = 0.14f),
        chipBg     = Color(0xFFFFFFFF).copy(alpha = 0.55f),
        fieldBg    = Color(0xFFFFFFFF).copy(alpha = 0.65f),
        fieldBorder= Color(0xFF3A4E70).copy(alpha = 0.22f),
        accentPrimary   = Color(0xFF1E7BC7),
        accentSecondary = Color(0xFF7A4FD1),
        danger  = Color(0xFFC53147),
        success = Color(0xFF1E9E6C),
        statusBar = Color(0xFF9BC8FF).copy(alpha = 0.42f)
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
        "GLASS_LIGHT", "GLASS-LIGHT", "GLASSLIGHT" -> GlassLight
        "GLASS_DARK", "GLASS-DARK", "GLASSDARK", "GLASS" -> GlassDark
        else    -> Dark
    }
}

/** True while a Liquid-Glass / Aero variant is active — enables prism borders + serif fonts elsewhere. */
val ThemePalette.isGlass: Boolean
    get() = name.startsWith("GLASS", ignoreCase = true)

/**
 * Chromatic prism border — the rainbow-reflection edge that both Windows Vista's Aero cards
 * and Apple's Liquid Glass panels use as their signature. Falls back to the palette's default
 * gradient for non-glass themes so callers can use it unconditionally.
 */
fun ThemePalette.prismBorderBrush(): Brush = if (isGlass) {
    Brush.linearGradient(
        listOf(
            Color(0xFFFF8AA6).copy(alpha = 0.55f),  // rose
            Color(0xFFFFB454).copy(alpha = 0.55f),  // amber
            Color(0xFFA6E3C6).copy(alpha = 0.55f),  // mint
            Color(0xFF8ACBFF).copy(alpha = 0.65f),  // cyan
            Color(0xFFB49CFF).copy(alpha = 0.55f)   // violet
        )
    )
} else borderBrush()

val LocalPalette = staticCompositionLocalOf { AppPalettes.Dark }

@Composable
fun AppThemeProvider(theme: String, lang: String = "en", content: @Composable () -> Unit) {
    val palette = AppPalettes.byName(theme)
    val strings = Localization.getStrings(lang)
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
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalStrings provides strings
    ) {
        content()
    }
}
