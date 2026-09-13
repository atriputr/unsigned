package com.example.un_signed

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

val BebasFont  = FontFamily(Font(R.font.bebas_neue))
val JerseyFont = FontFamily(Font(R.font.jersey_10_charted_regular))
val NokiaFont  = FontFamily(Font(R.font.nokia_kokia))

// ── Glass theme fonts ─────────────────────────────────────────
// Distinct, softer feel for the Liquid-Glass / Aero theme:
//  · Headings use the system Serif for that Apple-SF-Pro / Vista-typography vibe
//  · Body uses the system SansSerif (Roboto on Android — clean & modern)
//  · Numeric readouts keep NokiaFont for a mechanical accent
val GlassHeadingFont: FontFamily = FontFamily.Serif
val GlassContentFont: FontFamily = FontFamily.SansSerif
val GlassNumericFont: FontFamily = NokiaFont

/** Returns the heading font matched to the active theme. */
fun titleFontFor(themeName: String): FontFamily =
    if (themeName.startsWith("GLASS", ignoreCase = true)) GlassHeadingFont else BebasFont

/** Returns the body font matched to the active theme. */
fun contentFontFor(themeName: String): FontFamily =
    if (themeName.startsWith("GLASS", ignoreCase = true)) GlassContentFont else BebasFont
