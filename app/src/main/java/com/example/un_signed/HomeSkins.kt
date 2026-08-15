package com.example.un_signed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Button vertical positions must match the interactive View guidelines in activity_profile_selection.xml
// so the themed visuals sit exactly where the clickable Views are.
private const val BTN1_CENTER = 0.1863f    // (0.1567 + 0.2159) / 2
private const val BTN2_CENTER = 0.2571f    // (0.2279 + 0.2864) / 2
private const val BTN3_CENTER = 0.3286f    // (0.2996 + 0.3576) / 2
private const val BTN_HEIGHT_FRAC = 0.06f  // ~6% of screen height per button

/** Live counters passed in from the activity so quick-action buttons can display + increment. */
data class HomeQuickState(
    val sleepActive: Boolean,       // is a sleep session currently in progress?
    val junkCountToday: Int,
    val waterGlassesToday: Int,
    val waterTargetGlasses: Int
)

data class HomeQuickCallbacks(
    val onSleepToggle: () -> Unit,       // begin OR end (depending on active state)
    val onSleepCancel: () -> Unit,       // long-press: discard in-progress session
    val onJunkIncrement: () -> Unit,
    val onWaterIncrement: () -> Unit
)

/**
 * Full-bleed home skin rendered above the baked background image.
 * Only shown when theme is CREAM or AMBER — DARK keeps the original image.
 * The three "SELECT PROFILE" buttons + bottom quick-action bar are drawn here.
 */
@Composable
fun HomeSkin(
    themeName: String,
    titleFont: FontFamily,
    quickState: HomeQuickState,
    quickCallbacks: HomeQuickCallbacks
) {
    when (themeName.uppercase()) {
        "CREAM" -> HelloKittySkin(titleFont, quickState, quickCallbacks)
        "AMBER" -> LokiAmberSkin(titleFont, quickState, quickCallbacks)
        "DARK"  -> DarkIndustrialSkin(titleFont, quickState, quickCallbacks)
        else -> Unit
    }
}

// ══════════════════════════════════════════════════════════════════
//  DARK INDUSTRIAL  ·  Standard theme
// ══════════════════════════════════════════════════════════════════
@Composable
private fun DarkIndustrialSkin(
    titleFont: FontFamily,
    quickState: HomeQuickState,
    quickCallbacks: HomeQuickCallbacks
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        StatusBarBox()
        // Bottom quick-action bar (Sleep · Junk · Water)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DarkQuickButton(
                label = if (quickState.sleepActive) "SLEEP END" else "SLEEP BEGIN",
                subtitle = if (quickState.sleepActive) "long-press to cancel" else "tap to start",
                emoji = if (quickState.sleepActive) "☾" else "☽",
                highlighted = quickState.sleepActive,
                titleFont = titleFont,
                onTap = quickCallbacks.onSleepToggle,
                onLongPress = if (quickState.sleepActive) quickCallbacks.onSleepCancel else null,
                modifier = Modifier.weight(1f)
            )
            DarkQuickButton(
                label = "JUNK",
                subtitle = "${quickState.junkCountToday} today",
                emoji = "☗",
                highlighted = false,
                titleFont = titleFont,
                onTap = quickCallbacks.onJunkIncrement,
                onLongPress = null,
                modifier = Modifier.weight(1f)
            )
            DarkQuickButton(
                label = "WATER",
                subtitle = "${quickState.waterGlassesToday}/${quickState.waterTargetGlasses}",
                emoji = "◊",
                highlighted = false,
                titleFont = titleFont,
                onTap = quickCallbacks.onWaterIncrement,
                onLongPress = null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DarkQuickButton(
    label: String,
    subtitle: String,
    emoji: String,
    highlighted: Boolean,
    titleFont: FontFamily,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val primary = Color(0xFFDDDDDD)
    val secondary = Color(0xFF888888)

    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) primary else Color.White.copy(alpha = 0.05f))
            .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .pointerInput(highlighted) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, color = if (highlighted) Color.Black else primary, fontSize = 20.sp)
            Text(
                label,
                color = if (highlighted) Color.Black else primary,
                fontSize = 11.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                subtitle,
                color = if (highlighted) Color.Black.copy(alpha = 0.7f) else secondary,
                fontSize = 9.sp,
                fontFamily = titleFont
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  HELLO KITTY  ·  CREAM theme
// ══════════════════════════════════════════════════════════════════
@Composable
private fun HelloKittySkin(
    titleFont: FontFamily,
    quickState: HomeQuickState,
    quickCallbacks: HomeQuickCallbacks
) {
    val bgTop = Color(0xFFFFEEF6)
    val bgBot = Color(0xFFFFC8DE)
    val ink   = Color(0xFF3A1A2A)
    val bow   = Color(0xFFFF3B7C)
    val bowShine = Color(0xFFFFFFFF)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBot)))
    ) {
        StatusBarBox()
        val screenH = maxHeight
        val btnHeight = screenH * BTN_HEIGHT_FRAC
        val sideMargin = 24.dp

        // Sparkle field
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stars = listOf(
                Offset(size.width * 0.08f, size.height * 0.06f) to 6f,
                Offset(size.width * 0.90f, size.height * 0.04f) to 4f,
                Offset(size.width * 0.06f, size.height * 0.40f) to 3f,
                Offset(size.width * 0.94f, size.height * 0.42f) to 5f,
                Offset(size.width * 0.09f, size.height * 0.66f) to 4f,
                Offset(size.width * 0.92f, size.height * 0.62f) to 6f,
                Offset(size.width * 0.14f, size.height * 0.92f) to 3f,
                Offset(size.width * 0.85f, size.height * 0.90f) to 5f
            )
            stars.forEach { (pos, r) -> sparkle(pos, r, Color(0xFFFF6EA8)) }
        }

        // Title banner near the very top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SELECT PROFILE",
                    color = ink,
                    fontSize = 32.sp,
                    fontFamily = titleFont,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(shadow = Shadow(color = bow.copy(alpha = 0.5f), offset = Offset(0f, 3f), blurRadius = 6f))
                )
                Bow(bow = bow, bowShine = bowShine, ink = ink)
            }
        }

        // Three buttons at fixed y-fractions matching the interactive Views
        listOf(
            Triple(BTN1_CENTER, "IDEAL PROFILE", "★"),
            Triple(BTN2_CENTER, "CUSTOM PROFILE", "♥"),
            Triple(BTN3_CENTER, "EXPORT YOUR PROGRESS", "☁")
        ).forEach { (fraction, label, icon) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sideMargin)
                    .height(btnHeight)
                    .offset(y = screenH * fraction - btnHeight / 2f)
            ) {
                KittyButton(label, ink = ink, bow = bow, bowShine = bowShine, titleFont = titleFont, iconLabel = icon)
            }
        }

        // Bottom quick-action bar (Sleep · Junk · Water)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KittyQuickButton(
                label = if (quickState.sleepActive) "SLEEP END" else "SLEEP BEGIN",
                subtitle = if (quickState.sleepActive) "long-press to cancel" else "tap to start",
                emoji = if (quickState.sleepActive) "☾" else "☽",
                highlighted = quickState.sleepActive,
                ink = ink, bow = bow, titleFont = titleFont,
                onTap = quickCallbacks.onSleepToggle,
                onLongPress = if (quickState.sleepActive) quickCallbacks.onSleepCancel else null,
                modifier = Modifier.weight(1f)
            )
            KittyQuickButton(
                label = "JUNK",
                subtitle = "${quickState.junkCountToday} today",
                emoji = "🍭",
                highlighted = false,
                ink = ink, bow = bow, titleFont = titleFont,
                onTap = quickCallbacks.onJunkIncrement,
                onLongPress = null,
                modifier = Modifier.weight(1f)
            )
            KittyQuickButton(
                label = "WATER",
                subtitle = "${quickState.waterGlassesToday}/${quickState.waterTargetGlasses}",
                emoji = "💧",
                highlighted = false,
                ink = ink, bow = bow, titleFont = titleFont,
                onTap = quickCallbacks.onWaterIncrement,
                onLongPress = null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KittyQuickButton(
    label: String,
    subtitle: String,
    emoji: String,
    highlighted: Boolean,
    ink: Color,
    bow: Color,
    titleFont: FontFamily,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) bow else Color.White)
            .border(2.5.dp, if (highlighted) ink else bow, RoundedCornerShape(18.dp))
            .pointerInput(highlighted) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 18.sp)
            Text(
                label,
                color = if (highlighted) Color.White else ink,
                fontSize = 11.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                subtitle,
                color = if (highlighted) Color.White.copy(alpha = 0.85f) else ink.copy(alpha = 0.55f),
                fontSize = 9.sp,
                fontFamily = titleFont
            )
        }
    }
}

@Composable
private fun KittyButton(label: String, ink: Color, bow: Color, bowShine: Color, titleFont: FontFamily, iconLabel: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Drop shadow behind
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(ink.copy(alpha = 0.22f))
        )
        // Main body
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF0F7))))
                .border(3.dp, bow, RoundedCornerShape(50))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left icon circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bow)
                    .border(2.dp, ink, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconLabel, color = bowShine, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = ink,
                fontSize = 17.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            Text("♥", color = bow, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Bow(bow: Color, bowShine: Color, ink: Color) {
    Canvas(modifier = Modifier.size(width = 92.dp, height = 40.dp).padding(top = 4.dp)) {
        val w = size.width
        val h = size.height

        // Left triangle
        val leftPath = Path().apply {
            moveTo(w * 0.10f, h * 0.20f)
            lineTo(w * 0.44f, h * 0.55f)
            lineTo(w * 0.10f, h * 0.90f)
            close()
        }
        // Right triangle
        val rightPath = Path().apply {
            moveTo(w * 0.90f, h * 0.20f)
            lineTo(w * 0.56f, h * 0.55f)
            lineTo(w * 0.90f, h * 0.90f)
            close()
        }
        drawPath(leftPath, color = bow)
        drawPath(rightPath, color = bow)
        drawPath(leftPath, color = ink, style = Stroke(width = 3f))
        drawPath(rightPath, color = ink, style = Stroke(width = 3f))

        // Center knot
        drawRoundRect(
            color = bow,
            topLeft = Offset(w * 0.44f, h * 0.30f),
            size = Size(w * 0.12f, h * 0.50f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.44f, h * 0.30f),
            size = Size(w * 0.12f, h * 0.50f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            style = Stroke(width = 3f)
        )

        // Sheen on left loop
        drawCircle(bowShine.copy(alpha = 0.6f), 4f, Offset(w * 0.20f, h * 0.35f))
        drawCircle(bowShine.copy(alpha = 0.6f), 4f, Offset(w * 0.80f, h * 0.35f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.sparkle(
    center: Offset, radius: Float, color: Color
) {
    // 4-point star sparkle
    drawLine(
        color = color, start = Offset(center.x - radius * 2, center.y),
        end = Offset(center.x + radius * 2, center.y),
        strokeWidth = radius * 0.6f, cap = StrokeCap.Round
    )
    drawLine(
        color = color, start = Offset(center.x, center.y - radius * 2),
        end = Offset(center.x, center.y + radius * 2),
        strokeWidth = radius * 0.6f, cap = StrokeCap.Round
    )
    drawCircle(color, radius * 0.7f, center)
}

// ══════════════════════════════════════════════════════════════════
//  LOKI  ·  AMBER theme  (dark green + gold, mystical)
// ══════════════════════════════════════════════════════════════════
@Composable
private fun LokiAmberSkin(
    titleFont: FontFamily,
    quickState: HomeQuickState,
    quickCallbacks: HomeQuickCallbacks
) {
    val bgTop = Color(0xFF0B2418)
    val bgBot = Color(0xFF06110B)
    val gold  = Color(0xFFFFB454)
    val goldDeep = Color(0xFFB07A2A)
    val emerald = Color(0xFF2C7350)
    val onGold = Color(0xFF120A03)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(bgTop, bgBot), radius = 1400f))
    ) {
        StatusBarBox()
        val screenH = maxHeight
        val btnHeight = screenH * BTN_HEIGHT_FRAC
        val sideMargin = 20.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val topR = size.width * 0.30f
            val topC = Offset(size.width / 2f, -topR * 0.4f)
            listOf(topR to 0.18f, topR * 0.7f to 0.30f).forEach { (r, alpha) ->
                drawArc(
                    color = gold.copy(alpha = alpha),
                    startAngle = 0f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(topC.x - r, topC.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
            drawCircle(
                color = emerald.copy(alpha = 0.20f),
                radius = size.width * 0.65f,
                center = Offset(size.width / 2f, size.height + 300f)
            )
        }

        // Header — Loki horns + title
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LokiHorns(gold = gold, goldDeep = goldDeep)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "SELECT PROFILE",
                color = gold,
                fontSize = 24.sp,
                fontFamily = titleFont,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                style = TextStyle(shadow = Shadow(color = gold.copy(alpha = 0.6f), blurRadius = 14f))
            )
            Text(
                text = "— GLORIOUS PURPOSE —",
                color = goldDeep,
                fontSize = 9.sp,
                fontFamily = titleFont,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        listOf(
            Triple(BTN1_CENTER, "IDEAL PROFILE", "◇"),
            Triple(BTN2_CENTER, "CUSTOM PROFILE", "◆"),
            Triple(BTN3_CENTER, "EXPORT YOUR PROGRESS", "☓")
        ).forEach { (fraction, label, glyph) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sideMargin)
                    .height(btnHeight)
                    .offset(y = screenH * fraction - btnHeight / 2f)
            ) {
                LokiPlate(label, gold, goldDeep, emerald, onGold, titleFont, glyph = glyph)
            }
        }

        // Bottom quick-action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LokiQuickButton(
                label = if (quickState.sleepActive) "SLEEP END" else "SLEEP BEGIN",
                subtitle = if (quickState.sleepActive) "hold to cancel" else "tap to start",
                glyph = if (quickState.sleepActive) "☾" else "☽",
                highlighted = quickState.sleepActive,
                gold = gold, goldDeep = goldDeep, onGold = onGold, emerald = emerald,
                titleFont = titleFont,
                onTap = quickCallbacks.onSleepToggle,
                onLongPress = if (quickState.sleepActive) quickCallbacks.onSleepCancel else null,
                modifier = Modifier.weight(1f)
            )
            LokiQuickButton(
                label = "JUNK",
                subtitle = "${quickState.junkCountToday} today",
                glyph = "☗",
                highlighted = false,
                gold = gold, goldDeep = goldDeep, onGold = onGold, emerald = emerald,
                titleFont = titleFont,
                onTap = quickCallbacks.onJunkIncrement,
                onLongPress = null,
                modifier = Modifier.weight(1f)
            )
            LokiQuickButton(
                label = "WATER",
                subtitle = "${quickState.waterGlassesToday}/${quickState.waterTargetGlasses}",
                glyph = "◊",
                highlighted = false,
                gold = gold, goldDeep = goldDeep, onGold = onGold, emerald = emerald,
                titleFont = titleFont,
                onTap = quickCallbacks.onWaterIncrement,
                onLongPress = null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LokiQuickButton(
    label: String,
    subtitle: String,
    glyph: String,
    highlighted: Boolean,
    gold: Color,
    goldDeep: Color,
    onGold: Color,
    emerald: Color,
    titleFont: FontFamily,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Outer gold frame → inner emerald/dark plate
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(gold, goldDeep)))
            .padding(2.dp)
            .pointerInput(highlighted) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        if (highlighted) listOf(emerald.copy(alpha = 0.9f), Color(0xFF0A2016))
                        else listOf(Color(0xFF0F2E20), Color(0xFF06170D))
                    )
                )
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(glyph, color = gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                label,
                color = gold,
                fontSize = 11.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                subtitle,
                color = goldDeep,
                fontSize = 9.sp,
                fontFamily = titleFont,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun LokiHorns(gold: Color, goldDeep: Color) {
    Canvas(modifier = Modifier.size(width = 140.dp, height = 60.dp)) {
        val w = size.width
        val h = size.height
        // Left horn
        val leftHorn = Path().apply {
            moveTo(w * 0.32f, h * 0.95f)
            cubicTo(
                w * 0.20f, h * 0.60f,
                w * 0.10f, h * 0.35f,
                w * 0.00f, h * 0.10f
            )
            lineTo(w * 0.02f, h * 0.05f)
            cubicTo(
                w * 0.16f, h * 0.35f,
                w * 0.28f, h * 0.65f,
                w * 0.36f, h * 0.95f
            )
            close()
        }
        // Right horn (mirror)
        val rightHorn = Path().apply {
            moveTo(w * 0.68f, h * 0.95f)
            cubicTo(
                w * 0.80f, h * 0.60f,
                w * 0.90f, h * 0.35f,
                w * 1.00f, h * 0.10f
            )
            lineTo(w * 0.98f, h * 0.05f)
            cubicTo(
                w * 0.84f, h * 0.35f,
                w * 0.72f, h * 0.65f,
                w * 0.64f, h * 0.95f
            )
            close()
        }
        // Center helm band
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(goldDeep, gold, goldDeep)),
            topLeft = Offset(w * 0.32f, h * 0.75f),
            size = Size(w * 0.36f, h * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        drawPath(leftHorn, brush = Brush.verticalGradient(listOf(gold, goldDeep)))
        drawPath(rightHorn, brush = Brush.verticalGradient(listOf(gold, goldDeep)))
        drawPath(leftHorn, color = Color(0xFF3A2408), style = Stroke(width = 1.5f))
        drawPath(rightHorn, color = Color(0xFF3A2408), style = Stroke(width = 1.5f))
    }
}

@Composable
private fun LokiPlate(
    label: String,
    gold: Color,
    goldDeep: Color,
    emerald: Color,
    onGold: Color,
    titleFont: FontFamily,
    glyph: String
) {
    // Outer gold frame + inner dark plate
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(gold, goldDeep)))
            .padding(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0F2E20), Color(0xFF06170D))))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.radialGradient(listOf(gold, goldDeep)))
                    .border(1.dp, Color(0xFF2A1A05), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(glyph, color = onGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = gold,
                fontSize = 15.sp,
                fontFamily = titleFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.weight(1f),
                style = TextStyle(shadow = Shadow(color = gold.copy(alpha = 0.5f), blurRadius = 10f))
            )
            Text("◈", color = emerald, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
