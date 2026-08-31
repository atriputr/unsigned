package com.example.un_signed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import kotlinx.coroutines.CoroutineScope

// Button vertical positions must match the interactive View guidelines in activity_profile_selection.xml
// so the themed visuals sit exactly where the clickable Views are.
private const val BTN1_CENTER = 0.1863f    // (0.1567 + 0.2159) / 2
private const val BTN2_CENTER = 0.2571f    // (0.2279 + 0.2864) / 2
private const val BTN3_CENTER = 0.3286f    // (0.2996 + 0.3576) / 2
private const val BTN_HEIGHT_FRAC = 0.075f  // visual button ~7.5% of screen (click zone stays at 5.92%)

/** Live counters passed in from the activity so quick-action buttons can display + increment. */
data class HomeQuickState(
    val sleepActive: Boolean,          // is a sleep session currently in progress?
    val sleepDisturbances: Int,        // disturbance count during active session
    val junkCountToday: Int,
    val waterGlassesToday: Int,
    val waterTargetGlasses: Int
)

/**
 * Gesture-driven callbacks. Contract:
 *   Junk & Water:   single tap = −1, double tap = +1, hold 3s = reset to 0
 *   Sleep (idle):   hold 3s = begin,  hold 8s = open manage popup
 *   Sleep (active): single tap = record disturbance, double tap = end + save, hold 8s = manage popup
 */
data class HomeQuickCallbacks(
    val onSleepBegin: () -> Unit,
    val onSleepDisturbed: () -> Unit,
    val onSleepEnd: () -> Unit,
    val onSleepManage: () -> Unit,
    val onJunkIncrement: () -> Unit,
    val onJunkDecrement: () -> Unit,
    val onJunkOpenDetailed: () -> Unit,     // hold 3s = open catalogued junk-log wizard
    val onWaterIncrement: () -> Unit,
    val onWaterDecrement: () -> Unit,
    val onWaterReset: () -> Unit
)

/**
 * Sleep button gesture bundle — behaviour depends on whether a session is active.
 *
 *   Idle   : hold 3 s → BEGIN sleep      (single threshold; no collision)
 *   Active : single tap → DISTURBED,  double tap → END,  hold 8 s → MANAGE popup
 *
 * (Manage-popup for idle state is reachable via the full Sleep overlay in Peace → Sleep,
 *  so it doesn't need to fight with the 3-second BEGIN gesture on the home button.)
 */
private fun Modifier.sleepQuickGestures(
    scope: CoroutineScope,
    state: HomeQuickState,
    cb: HomeQuickCallbacks
): Modifier {
    val active = state.sleepActive
    return this.quickGestures(
        scope = scope,
        longPress1Ms = if (active) 8000L else 3000L,
        longPress2Ms = null,                              // one hold threshold per state = no collision
        onSingleTap  = if (active) cb.onSleepDisturbed else { {} },
        onDoubleTap  = if (active) cb.onSleepEnd else { {} },
        onLongPress1 = if (active) cb.onSleepManage else cb.onSleepBegin,
        onLongPress2 = {}
    )
}

/** Junk / Water shared counter gestures: single = −1, double = +1, hold 3 s = reset. */
private fun Modifier.counterQuickGestures(
    scope: CoroutineScope,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit
): Modifier = this.quickGestures(
    scope = scope,
    longPress1Ms = 3000L,
    onSingleTap  = onDecrement,
    onDoubleTap  = onIncrement,
    onLongPress1 = onReset
)

// Subtitle helpers so each themed button reads consistently
private fun sleepLabel(state: HomeQuickState, strings: AppStrings) = strings.sleep
private fun sleepSubtitle(state: HomeQuickState, strings: AppStrings) =
    if (state.sleepActive) "${strings.sleepEnd} · ${strings.formatNumbers(state.sleepDisturbances)} ${strings.disturbances}"
    else strings.sleepStartHint
private fun sleepEmoji(state: HomeQuickState)    = if (state.sleepActive) "☾" else "☽"
private fun junkSubtitle(count: Int, strings: AppStrings)     = "${strings.formatNumbers(count)} · ${strings.formatNumbers(2)}× ${strings.addTopics.substringAfter("+ ").take(3)} · HOLD LOG"
private fun waterSubtitle(g: Int, tgt: Int, strings: AppStrings) = "${strings.formatNumbers(g)}/${strings.formatNumbers(tgt)} · ${strings.formatNumbers(2)}× ${strings.addTopics.substringAfter("+ ").take(3)}"

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
//  Grunge title + three menu buttons come from bg_select_profile.png
//  (rendered by the ImageView beneath us). For non-English locales we
//  overlay localised text on top with a dark backing patch that hides
//  the baked English pixels; English keeps the PNG pixel-perfect.
// ══════════════════════════════════════════════════════════════════
@Composable
private fun DarkIndustrialSkin(
    titleFont: FontFamily,
    quickState: HomeQuickState,
    quickCallbacks: HomeQuickCallbacks
) {
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current
    val defaults = remember { AppStrings() }
    val needsLocalisedTitle = strings.selectProfile != defaults.selectProfile ||
        strings.idealProfile != defaults.idealProfile ||
        strings.customProfile != defaults.customProfile ||
        strings.exportProgress != defaults.exportProgress

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = maxHeight

        if (needsLocalisedTitle) {
            // Title — three-layer red neon (wide halo → soft glow → crisp core) sits over
            // a soft radial dark blob that fades into the PNG's dark frame on the sides
            // instead of ending in a hard rectangle.
            val redNeon = Color(0xFFE41417)
            val redCore = Color(0xFFFF4A2C)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = screenH * 0.058f)
                    .fillMaxWidth(0.98f)
                    .padding(vertical = 12.dp)
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Black,
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.5f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Widest halo
                Text(
                    text = strings.selectProfile,
                    color = redNeon.copy(alpha = 0.35f),
                    fontSize = 36.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    style = TextStyle(shadow = Shadow(color = redNeon, blurRadius = 44f))
                )
                // Mid glow
                Text(
                    text = strings.selectProfile,
                    color = redNeon.copy(alpha = 0.6f),
                    fontSize = 36.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    style = TextStyle(shadow = Shadow(color = redNeon, blurRadius = 20f))
                )
                // Crisp core
                Text(
                    text = strings.selectProfile,
                    color = redCore,
                    fontSize = 36.sp,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black, offset = Offset(1f, 2f), blurRadius = 3f)
                    )
                )
            }

            // Three buttons: clean grunge plate + icon + label + chevron. Icons and
            // chevron get a soft amber radial halo so they read as "lit" against the
            // dark plate — matches the glow feel of the baked PNG buttons.
            data class Btn(
                val yFraction: Float,
                val label: String,
                val plateRes: Int,
                val iconRes: Int
            )
            val amberBright = Color(0xFFF5B85B)
            val amberDeep   = Color(0xFFD48420)
            val amberGlow   = Color(0xFFFF9A3C)
            listOf(
                Btn(BTN1_CENTER, strings.idealProfile, R.drawable.btn_plate_1, R.drawable.ic_person),
                Btn(BTN2_CENTER, strings.customProfile, R.drawable.btn_plate_2, R.drawable.ic_gear),
                Btn(BTN3_CENTER, strings.exportProgress, R.drawable.btn_plate_3, R.drawable.ic_cloud)
            ).forEach { btn ->
                val boxHeight = screenH * BTN_HEIGHT_FRAC * 1.5f
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .offset(y = screenH * btn.yFraction - boxHeight / 2f)
                        .height(boxHeight)
                ) {
                    // Grunge plate covers the baked button underneath
                    Image(
                        painter = painterResource(id = btn.plateRes),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon with soft amber halo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                amberGlow.copy(alpha = 0.35f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width / 2f
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = btn.iconRes),
                                contentDescription = null,
                                tint = amberBright,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Warm under-glow behind label
                            Text(
                                text = btn.label,
                                color = amberGlow.copy(alpha = 0.5f),
                                fontSize = 18.sp,
                                fontFamily = titleFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                style = TextStyle(shadow = Shadow(color = amberGlow, blurRadius = 18f))
                            )
                            // Crisp bronze-gradient label
                            Text(
                                text = btn.label,
                                color = amberDeep,
                                fontSize = 18.sp,
                                fontFamily = titleFont,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp,
                                style = TextStyle(
                                    brush = Brush.linearGradient(listOf(amberBright, amberDeep)),
                                    shadow = Shadow(color = Color.Black, offset = Offset(1f, 2f), blurRadius = 4f)
                                )
                            )
                        }

                        // Chevron with matching soft amber halo
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                amberGlow.copy(alpha = 0.35f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.width / 2f
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = amberBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DarkQuickButton(
                label = sleepLabel(quickState, strings),
                subtitle = sleepSubtitle(quickState, strings),
                emoji = sleepEmoji(quickState),
                highlighted = quickState.sleepActive,
                titleFont = titleFont,
                gestureModifier = Modifier.sleepQuickGestures(scope, quickState, quickCallbacks),
                modifier = Modifier.weight(1f)
            )
            DarkQuickButton(
                label = strings.junk,
                subtitle = junkSubtitle(quickState.junkCountToday, strings),
                emoji = "☗",
                highlighted = false,
                titleFont = titleFont,
                gestureModifier = Modifier.counterQuickGestures(
                    scope,
                    onIncrement = quickCallbacks.onJunkIncrement,
                    onDecrement = quickCallbacks.onJunkDecrement,
                    onReset = quickCallbacks.onJunkOpenDetailed
                ),
                modifier = Modifier.weight(1f)
            )
            DarkQuickButton(
                label = strings.water,
                subtitle = waterSubtitle(quickState.waterGlassesToday, quickState.waterTargetGlasses, strings),
                emoji = "◊",
                highlighted = false,
                titleFont = titleFont,
                gestureModifier = Modifier.counterQuickGestures(
                    scope,
                    onIncrement = quickCallbacks.onWaterIncrement,
                    onDecrement = quickCallbacks.onWaterDecrement,
                    onReset = quickCallbacks.onWaterReset
                ),
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
    gestureModifier: Modifier,
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
            .then(gestureModifier)
            .padding(horizontal = 6.dp, vertical = 6.dp),
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
                fontSize = 8.sp,
                fontFamily = titleFont,
                letterSpacing = 0.5.sp
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
    val strings = LocalStrings.current
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
                    text = strings.selectProfile,
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
            Triple(BTN1_CENTER, strings.idealProfile, "★"),
            Triple(BTN2_CENTER, strings.customProfile, "♥"),
            Triple(BTN3_CENTER, strings.exportProgress, "☁")
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
        val scope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KittyQuickButton(
                label = sleepLabel(quickState, strings),
                subtitle = sleepSubtitle(quickState, strings),
                emoji = sleepEmoji(quickState),
                highlighted = quickState.sleepActive,
                ink = ink, bow = bow, titleFont = titleFont,
                gestureModifier = Modifier.sleepQuickGestures(scope, quickState, quickCallbacks),
                modifier = Modifier.weight(1f)
            )
            KittyQuickButton(
                label = strings.junk,
                subtitle = junkSubtitle(quickState.junkCountToday, strings),
                emoji = "🍭",
                highlighted = false,
                ink = ink, bow = bow, titleFont = titleFont,
                gestureModifier = Modifier.counterQuickGestures(
                    scope,
                    onIncrement = quickCallbacks.onJunkIncrement,
                    onDecrement = quickCallbacks.onJunkDecrement,
                    onReset = quickCallbacks.onJunkOpenDetailed
                ),
                modifier = Modifier.weight(1f)
            )
            KittyQuickButton(
                label = strings.water,
                subtitle = waterSubtitle(quickState.waterGlassesToday, quickState.waterTargetGlasses, strings),
                emoji = "💧",
                highlighted = false,
                ink = ink, bow = bow, titleFont = titleFont,
                gestureModifier = Modifier.counterQuickGestures(
                    scope,
                    onIncrement = quickCallbacks.onWaterIncrement,
                    onDecrement = quickCallbacks.onWaterDecrement,
                    onReset = quickCallbacks.onWaterReset
                ),
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
    gestureModifier: Modifier,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) bow else Color.White)
            .border(2.5.dp, if (highlighted) ink else bow, RoundedCornerShape(18.dp))
            .then(gestureModifier)
            .padding(horizontal = 6.dp, vertical = 6.dp),
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
                fontSize = 8.sp,
                fontFamily = titleFont,
                letterSpacing = 0.5.sp
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
    val strings = LocalStrings.current
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
                text = strings.selectProfile,
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
            Triple(BTN1_CENTER, strings.idealProfile, "◇"),
            Triple(BTN2_CENTER, strings.customProfile, "◆"),
            Triple(BTN3_CENTER, strings.exportProgress, "☓")
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
        val scope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LokiQuickButton(
                label = sleepLabel(quickState, strings),
                subtitle = sleepSubtitle(quickState, strings),
                glyph = sleepEmoji(quickState),
                highlighted = quickState.sleepActive,
                gold = gold, goldDeep = goldDeep, onGold = onGold, emerald = emerald,
                titleFont = titleFont,
                gestureModifier = Modifier.sleepQuickGestures(scope, quickState, quickCallbacks),
                modifier = Modifier.weight(1f)
            )
            LokiQuickButton(
                label = strings.junk,
                subtitle = junkSubtitle(quickState.junkCountToday, strings),
                glyph = "☗",
                highlighted = false,
                gold = gold, goldDeep = goldDeep, onGold = onGold, emerald = emerald,
                titleFont = titleFont,
                gestureModifier = Modifier.counterQuickGestures(
                    scope,
                    onIncrement = quickCallbacks.onJunkIncrement,
                    onDecrement = quickCallbacks.onJunkDecrement,
                    onReset = quickCallbacks.onJunkOpenDetailed
                ),
                modifier = Modifier.weight(1f)
            )
            LokiQuickButton(
                label = strings.water,
                subtitle = waterSubtitle(quickState.waterGlassesToday, quickState.waterTargetGlasses, strings),
                glyph = "◊",
                highlighted = false,
                gold = gold, goldDeep = goldDeep, onGold = onGold, emerald = emerald,
                titleFont = titleFont,
                gestureModifier = Modifier.counterQuickGestures(
                    scope,
                    onIncrement = quickCallbacks.onWaterIncrement,
                    onDecrement = quickCallbacks.onWaterDecrement,
                    onReset = quickCallbacks.onWaterReset
                ),
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
    gestureModifier: Modifier,
    modifier: Modifier = Modifier
) {
    // Outer gold frame → inner emerald/dark plate
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(gold, goldDeep)))
            .padding(2.dp)
            .then(gestureModifier)
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
                fontSize = 8.sp,
                fontFamily = titleFont,
                letterSpacing = 0.5.sp
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
