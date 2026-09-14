package com.example.un_signed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope

private data class DarkPlate(
    val yFraction: Float,
    val label: String,
    val plateRes: Int,
    val iconRes: Int
)

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
    quickCallbacks: HomeQuickCallbacks,
    platePressState: PlatePressState = PlatePressState()
) {
    when (themeName.uppercase()) {
        "CREAM" -> HelloKittySkin(titleFont, quickState, quickCallbacks)
        "AMBER" -> LokiAmberSkin(titleFont, quickState, quickCallbacks)
        "DARK"  -> DarkIndustrialSkin(titleFont, quickState, quickCallbacks)
        "GLASS_DARK", "GLASS" -> GlassSkin(dark = true,  quickState = quickState, quickCallbacks = quickCallbacks, platePressState = platePressState)
        "GLASS_LIGHT"          -> GlassSkin(dark = false, quickState = quickState, quickCallbacks = quickCallbacks, platePressState = platePressState)
        else -> Unit
    }
}

@Composable
private fun SelectProfileReel(
    text: String,
    fontFamily: FontFamily,
    redCore: Color,
    redNeon: Color,
    redHalo: Color,
    modifier: Modifier = Modifier
) {
    var textWidthPx by remember { mutableFloatStateOf(400f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clipToBounds()
            // Horizontal edge fade — the reel materialises out of and dissolves into shadow.
            // DstIn keeps DST where SRC is opaque, so we paint Black in the middle (keep) and
            // fade to Transparent at the edges (dissolve).
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fadeFrac = 0.10f
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        fadeFrac to Color.Black,
                        (1f - fadeFrac) to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = with(LocalDensity.current) { this@BoxWithConstraints.maxWidth.toPx() }

        // Right-to-left marquee (classic ticker direction). Linear horizontal motion —
        // the curved-drum feel comes from the sine bow + scale/alpha bloom below, adding
        // extra easing to horizontal speed would fight the visual rhythm.
        val transition = rememberInfiniteTransition(label = "titleReel")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "reelProgress"
        )
        val offsetXPx = containerWidthPx - progress * (textWidthPx + containerWidthPx)

        // Sinusoidal vertical bow — traces the curve of an imaginary drum surface.
        val centerNorm = ((offsetXPx + textWidthPx / 2f) / containerWidthPx).coerceIn(0f, 1f)
        val bow = kotlin.math.sin(centerNorm * kotlin.math.PI).toFloat()   // 0 → 1 → 0
        // Rise slightly through the centre; scale + brightness bloom subtly at the apex.
        val bowDy = -bow * 4f          // pixels lift
        val bowScale = 1f + bow * 0.03f
        val bowAlpha = 0.75f + bow * 0.25f

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .offset { IntOffset(offsetXPx.toInt(), bowDy.toInt()) }
                .graphicsLayer {
                    scaleX = bowScale
                    scaleY = bowScale
                    alpha = bowAlpha
                },
            contentAlignment = Alignment.Center
        ) {
            // Wide outer halo — the "spill" around the letters
            Text(
                text = text,
                color = redHalo.copy(alpha = 0.55f),
                fontSize = 26.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(shadow = Shadow(color = redNeon, blurRadius = 40f)),
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val w = coordinates.size.width.toFloat()
                    if (w > 0f && w != textWidthPx) {
                        textWidthPx = w
                    }
                }
            )
            // Mid glow
            Text(
                text = text,
                color = redNeon.copy(alpha = 0.85f),
                fontSize = 26.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(shadow = Shadow(color = redNeon, blurRadius = 18f))
            )
            // Crisp core
            Text(
                text = text,
                color = redCore,
                fontSize = 26.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(1f, 2f), blurRadius = 3f))
            )
            // Forged-glass specular highlight
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 26.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = (-1).dp),
                style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.35f), blurRadius = 2f))
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  ASHES  ·  DARK theme  (monochrome, low-contrast, minimalist)
//  Pure black bg, three grungy ash plates for menu buttons — no red,
//  no amber, no title. Icons + labels + chevrons are all soft grey so
//  they read as "written in ash" against the plate.
// ══════════════════════════════════════════════════════════════════
@Composable
private fun DarkIndustrialSkin(
    titleFont: FontFamily,
    quickState: HomeQuickState,
    quickCallbacks: HomeQuickCallbacks
) {
    val scope = rememberCoroutineScope()
    val strings = LocalStrings.current

    val ashLabel   = Color(0xFF9A9A9A)
    val ashIcon    = Color(0xFF7A7A7A)

    // Fractions of screen height reserved for the PNG's red gaming borders (top + bottom)
    val topBorderFrac = 0.075f
    val botBorderFrac = 0.075f

    val nokiaFont = remember { FontFamily(Font(R.font.nokia_kokia)) }
    val redCore   = Color(0xFFFF2323)
    val redNeon   = Color(0xFFE41417)
    val redHalo   = Color(0xFFFF6A6A)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenH = maxHeight

        // Opaque black mask over the middle — hides bg_select_profile's baked title + plates,
        // leaves the top and bottom red gaming borders visible.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = screenH * topBorderFrac)
                .height(screenH * (1f - topBorderFrac - botBorderFrac))
                .background(Color.Black)
        )

        // "SELECT PROFILE" — animated horizontal reel title coming from left going to right
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .offset(y = screenH * 0.095f),
            contentAlignment = Alignment.Center
        ) {
            SelectProfileReel(
                text = strings.selectProfile,
                fontFamily = nokiaFont,
                redCore = redCore,
                redNeon = redNeon,
                redHalo = redHalo
            )
        }

        listOf(
            DarkPlate(BTN1_CENTER, strings.idealProfile, R.drawable.btn_plate_1, R.drawable.ic_person),
            DarkPlate(BTN2_CENTER, strings.customProfile, R.drawable.btn_plate_2, R.drawable.ic_gear),
            DarkPlate(BTN3_CENTER, strings.exportProgress, R.drawable.btn_plate_3, R.drawable.ic_cloud)
        ).forEach { btn ->
            // Multiplier < ~1.0 leaves a visible gap between plates (centres are ~7.1% apart)
            val boxHeight = screenH * BTN_HEIGHT_FRAC * 0.9f
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .offset(y = screenH * btn.yFraction - boxHeight / 2f)
                    .height(boxHeight)
            ) {
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
                        .padding(horizontal = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = btn.iconRes),
                        contentDescription = null,
                        tint = ashIcon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = btn.label,
                        color = ashLabel,
                        fontSize = 16.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 1f), blurRadius = 2f)
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = ashIcon,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 20.dp),
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

// ══════════════════════════════════════════════════════════════════
//  GLASS  ·  Apple Liquid × Windows Aero fusion  (dark + light modes)
//  Frosted translucent surfaces, chromatic prism borders, aurora orbs,
//  refined serif title. Feels like Vista's Aero Glass met SF Pro.
// ══════════════════════════════════════════════════════════════════

// Chromatic prism colours used throughout the glass skin
private val PrismRose   = Color(0xFFFF8AA6)
private val PrismAmber  = Color(0xFFFFB454)
private val PrismMint   = Color(0xFFA6E3C6)
private val PrismCyan   = Color(0xFF8ACBFF)
private val PrismViolet = Color(0xFFB49CFF)

/**
 * State published by the Activity from setOnTouchListener on the three profile-click Views.
 *  · [pressedIdx] = -1 when nothing is pressed, otherwise 0/1/2 for Ideal/Custom/Export.
 *  · [touchFraction] is the finger position normalised to [0..1] on the pressed View.
 * Non-Glass skins ignore this — it's a pure decoration signal.
 */
data class PlatePressState(
    val pressedIdx: Int = -1,
    val touchFraction: Offset = Offset(0.5f, 0.5f)
)

private fun prismBrush(alpha: Float = 0.55f): Brush = Brush.linearGradient(
    listOf(
        PrismRose.copy(alpha = alpha),
        PrismAmber.copy(alpha = alpha),
        PrismMint.copy(alpha = alpha),
        PrismCyan.copy(alpha = alpha + 0.10f),
        PrismViolet.copy(alpha = alpha)
    )
)

@Composable
private fun GlassSkin(
    dark: Boolean,
    quickState: HomeQuickState,
    quickCallbacks: HomeQuickCallbacks,
    platePressState: PlatePressState = PlatePressState()
) {
    val strings = LocalStrings.current
    val scope   = rememberCoroutineScope()

    // Palette per mode — colors are internal to this skin so it's fully self-contained.
    val bgTop      = if (dark) Color(0xFF06132A) else Color(0xFFEFF5FE)
    val bgBot      = if (dark) Color(0xFF020614) else Color(0xFFDCE7F8)
    val onSurface  = if (dark) Color(0xFFF4F8FF) else Color(0xFF102341)
    val subtle     = if (dark) Color(0xFFCCD9F5).copy(alpha = 0.75f) else Color(0xFF3A4E70).copy(alpha = 0.80f)
    val faint      = if (dark) Color(0xFFA6BAE0).copy(alpha = 0.55f) else Color(0xFF6C82A8).copy(alpha = 0.70f)
    val panelTop   = if (dark) Color(0xFFFFFFFF).copy(alpha = 0.14f) else Color(0xFFFFFFFF).copy(alpha = 0.80f)
    val panelBot   = if (dark) Color(0xFF3B5FA8).copy(alpha = 0.10f) else Color(0xFFDCE9FB).copy(alpha = 0.80f)
    val highlight  = if (dark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.70f)

    // Live tilt from accelerometer/gravity — drives the parallax between wallpaper and glass.
    val tilt by MotionParallax.rememberTilt(smoothing = 0.88f, maxTiltDeg = 30f)
    // Independent shift magnitudes give the classic depth-layered feel:
    //  · Wallpaper drifts a lot (feels behind glass)
    //  · Glass content nudges opposite direction (feels floating over)
    val bgShiftX = tilt.x * 55f
    val bgShiftY = tilt.y * 55f
    val fgShiftX = -tilt.x * 10f
    val fgShiftY = -tilt.y * 10f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBot)))
    ) {
        StatusBarBox()
        val screenH = maxHeight
        val screenW = maxWidth
        // Button centres sit ~7.1% apart on screen (0.1863 / 0.2571 / 0.3286). Sizing the plate
        // at 0.72× BTN_HEIGHT_FRAC yields ~5.4% plate height, leaving ~1.7% (≈28-40dp on
        // typical devices) of clear breathing room between adjacent plates — enough for the
        // drop-shadow to sit without visually merging into the next plate.
        val btnHeight = screenH * BTN_HEIGHT_FRAC * 0.72f
        val sideMargin = 22.dp

        // ── Wallpaper (dark mode only) with parallax drift ────
        if (dark) {
            Image(
                painter = painterResource(id = R.drawable.glass_bg_dark),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    // Slight overscale so parallax translation never reveals letterbox edges.
                    .graphicsLayer {
                        scaleX = 1.18f
                        scaleY = 1.18f
                        translationX = bgShiftX
                        translationY = bgShiftY
                    }
            )
            // Cool dark tint so the aurora orbs and glass elements read strongly.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF04081A).copy(alpha = 0.55f),
                                Color(0xFF020614).copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

        // ── Aurora orbs backdrop ─────────────────────────────
        AuroraOrbs(dark = dark)

        // ── Title: elegant serif with soft chromatic glow ─────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp)
                .graphicsLayer {
                    translationX = fgShiftX
                    translationY = fgShiftY
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Prism divider mark above title
            Canvas(modifier = Modifier.width(72.dp).height(3.dp)) {
                drawRoundRect(
                    brush = prismBrush(alpha = if (dark) 0.85f else 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.selectProfile,
                color = onSurface,
                fontSize = 27.sp,
                fontFamily = GlassHeadingFont,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 4.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = PrismCyan.copy(alpha = if (dark) 0.55f else 0.35f),
                        blurRadius = if (dark) 22f else 10f
                    )
                )
            )
            Text(
                text = if (dark) "◈  a liquid interface awaits  ◈" else "◈  a luminous interface awaits  ◈",
                color = subtle,
                fontSize = 10.sp,
                fontFamily = GlassContentFont,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ── Three frosted glass profile plates ────────────────
        listOf(
            Triple(BTN1_CENTER, strings.idealProfile, "◉"),
            Triple(BTN2_CENTER, strings.customProfile, "⌘"),
            Triple(BTN3_CENTER, strings.exportProgress, "⇪")
        ).forEachIndexed { idx, (fraction, label, glyph) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sideMargin)
                    .height(btnHeight)
                    .offset(y = screenH * fraction - btnHeight / 2f)
                    .graphicsLayer {
                        translationX = fgShiftX
                        translationY = fgShiftY
                    }
            ) {
                val isPressed = platePressState.pressedIdx == idx
                GlassPlate(
                    label = label,
                    glyph = glyph,
                    dark = dark,
                    pressed = isPressed,
                    touchFraction = if (isPressed) platePressState.touchFraction else null,
                    onSurface = onSurface,
                    subtle = subtle,
                    panelTop = panelTop,
                    panelBot = panelBot,
                    highlight = highlight
                )
            }
        }

        // ── Bottom quick-action bar as frosted pills ──────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 24.dp)
                .graphicsLayer {
                    translationX = fgShiftX
                    translationY = fgShiftY
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassQuickButton(
                label = sleepLabel(quickState, strings),
                subtitle = sleepSubtitle(quickState, strings),
                glyph = sleepEmoji(quickState),
                highlighted = quickState.sleepActive,
                dark = dark,
                onSurface = onSurface, subtle = subtle, faint = faint,
                panelTop = panelTop, panelBot = panelBot, highlight = highlight,
                gestureModifier = Modifier.sleepQuickGestures(scope, quickState, quickCallbacks),
                modifier = Modifier.weight(1f)
            )
            GlassQuickButton(
                label = strings.junk,
                subtitle = junkSubtitle(quickState.junkCountToday, strings),
                glyph = "◈",
                highlighted = false,
                dark = dark,
                onSurface = onSurface, subtle = subtle, faint = faint,
                panelTop = panelTop, panelBot = panelBot, highlight = highlight,
                gestureModifier = Modifier.counterQuickGestures(
                    scope,
                    onIncrement = quickCallbacks.onJunkIncrement,
                    onDecrement = quickCallbacks.onJunkDecrement,
                    onReset = quickCallbacks.onJunkOpenDetailed
                ),
                modifier = Modifier.weight(1f)
            )
            GlassQuickButton(
                label = strings.water,
                subtitle = waterSubtitle(quickState.waterGlassesToday, quickState.waterTargetGlasses, strings),
                glyph = "◇",
                highlighted = false,
                dark = dark,
                onSurface = onSurface, subtle = subtle, faint = faint,
                panelTop = panelTop, panelBot = panelBot, highlight = highlight,
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

/**
 * Slow-drifting radial glows in the prism palette — Vista's Aurora wallpaper +
 * Apple's Liquid Glass depth. Positions animate very slowly so it feels alive
 * without being distracting.
 */
@Composable
private fun AuroraOrbs(dark: Boolean) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )

    val opacityBase = if (dark) 0.38f else 0.22f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Cyan orb — top-left, slowly slides right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrismCyan.copy(alpha = opacityBase), PrismCyan.copy(alpha = 0f)),
                center = Offset(w * (0.15f + drift * 0.10f), h * 0.20f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * (0.15f + drift * 0.10f), h * 0.20f)
        )

        // Violet orb — right side, slowly moves down
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrismViolet.copy(alpha = opacityBase * 0.85f), PrismViolet.copy(alpha = 0f)),
                center = Offset(w * 0.85f, h * (0.35f + drift * 0.08f)),
                radius = w * 0.60f
            ),
            radius = w * 0.60f,
            center = Offset(w * 0.85f, h * (0.35f + drift * 0.08f))
        )

        // Rose orb — bottom-center, breathes in size
        val rBase = w * 0.50f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrismRose.copy(alpha = opacityBase * 0.55f), PrismRose.copy(alpha = 0f)),
                center = Offset(w * 0.50f, h * 0.90f),
                radius = rBase + drift * 60f
            ),
            radius = rBase + drift * 60f,
            center = Offset(w * 0.50f, h * 0.90f)
        )

        // Mint accent — top-right corner, low intensity
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrismMint.copy(alpha = opacityBase * 0.50f), PrismMint.copy(alpha = 0f)),
                center = Offset(w * 0.95f, h * 0.05f),
                radius = w * 0.30f
            ),
            radius = w * 0.30f,
            center = Offset(w * 0.95f, h * 0.05f)
        )
    }
}

/**
 * The signature glass plate: translucent frosted panel + a bright specular
 * highlight running along the top edge (Aero glass hallmark) + a chromatic
 * prism border (Apple Liquid Glass edge).
 */
// Human-skin palette used for the "fingertip warmth" reflection under a press.
private val SkinLight = Color(0xFFF4C7A5)   // top layer — bright peach
private val SkinMid   = Color(0xFFD4A78B)   // mid — tan
private val SkinDeep  = Color(0xFFBE8B69)   // deep — bronze-brown

@Composable
private fun GlassPlate(
    label: String,
    glyph: String,
    dark: Boolean,
    pressed: Boolean,
    touchFraction: Offset?,
    onSurface: Color,
    subtle: Color,
    panelTop: Color,
    panelBot: Color,
    highlight: Color
) {
    // Physical press feedback — plate compresses slightly + slight brightness dip.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "plateScale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "pressAlpha"
    )
    // 3D tilt — camera-like rotation biased by where the finger is on the plate.
    val tiltX by animateFloatAsState(
        targetValue = if (pressed && touchFraction != null) (touchFraction.y - 0.5f) * -10f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "tiltX"
    )
    val tiltY by animateFloatAsState(
        targetValue = if (pressed && touchFraction != null) (touchFraction.x - 0.5f) * 10f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "tiltY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = tiltX
                rotationY = tiltY
                cameraDistance = 14f * density
            }
    ) {
        // Soft drop shadow beneath the plate — sells the "floating" feel.
        // Kept intentionally tight (1.dp offset) so it doesn't visually collide with the
        // next plate below when three sit close together.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (pressed) 0.dp else 1.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Color.Black.copy(alpha = if (dark) 0.22f else 0.06f)
                )
        )

        // Actual glass body
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(panelTop, panelBot)))
                .drawBehind {
                    // ── Human-skin fingertip warmth ─────────────────
                    // A soft warm radial glow at exactly the touch position,
                    // fading with pressAlpha so it eases in/out gracefully.
                    if (touchFraction != null && pressAlpha > 0.01f) {
                        val cx = size.width * touchFraction.x
                        val cy = size.height * touchFraction.y
                        val r = size.minDimension * 1.05f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SkinLight.copy(alpha = 0.42f * pressAlpha),
                                    SkinMid.copy(alpha  = 0.24f * pressAlpha),
                                    SkinDeep.copy(alpha = 0.10f * pressAlpha),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = r
                            ),
                            radius = r,
                            center = Offset(cx, cy)
                        )
                    }

                    // ── Diagonal "slide 3D" specular sweep ──────────
                    // A soft luminous band that rides across the plate on press,
                    // biased by touch position → gives the surface a moving-light feel.
                    if (pressAlpha > 0.01f) {
                        val fx = touchFraction?.x ?: 0.5f
                        val bandCentre = size.width * fx
                        val bandHalfW = size.width * 0.25f
                        val band = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.10f * pressAlpha),
                                Color.White.copy(alpha = 0.22f * pressAlpha),
                                Color.White.copy(alpha = 0.10f * pressAlpha),
                                Color.Transparent
                            ),
                            start = Offset(bandCentre - bandHalfW, 0f),
                            end = Offset(bandCentre + bandHalfW, size.height)
                        )
                        drawRect(brush = band)
                    }
                }
                .border(
                    1.2.dp,
                    prismBrush(alpha = if (pressed) (if (dark) 0.85f else 0.65f) else if (dark) 0.65f else 0.45f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glyph inside a small frosted-cyan disc
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PrismCyan.copy(alpha = if (dark) 0.42f else 0.22f),
                                PrismViolet.copy(alpha = if (dark) 0.30f else 0.14f)
                            )
                        )
                    )
                    .border(0.8.dp, prismBrush(alpha = 0.60f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    glyph,
                    color = onSurface,
                    fontSize = 17.sp,
                    fontFamily = GlassHeadingFont,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                color = onSurface,
                fontSize = 16.sp,
                fontFamily = GlassHeadingFont,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    shadow = Shadow(
                        color = if (dark) PrismCyan.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.6f),
                        offset = if (dark) Offset(0f, 0f) else Offset(0f, 1f),
                        blurRadius = if (dark) 6f else 2f
                    )
                )
            )
            Text("›", color = subtle, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        // Specular highlight — the bright horizontal sheen along the very top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .padding(horizontal = 16.dp)
                .offset(y = 2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            highlight,
                            highlight,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun GlassQuickButton(
    label: String,
    subtitle: String,
    glyph: String,
    highlighted: Boolean,
    dark: Boolean,
    onSurface: Color,
    subtle: Color,
    faint: Color,
    panelTop: Color,
    panelBot: Color,
    highlight: Color,
    gestureModifier: Modifier,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
    ) {
        // Drop shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 3.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = if (dark) 0.30f else 0.08f))
        )

        // Main frosted body
        val activeTint = if (highlighted) {
            Brush.verticalGradient(
                listOf(
                    PrismCyan.copy(alpha = if (dark) 0.40f else 0.35f),
                    PrismViolet.copy(alpha = if (dark) 0.30f else 0.20f)
                )
            )
        } else {
            Brush.verticalGradient(listOf(panelTop, panelBot))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(activeTint)
                .border(
                    1.dp,
                    prismBrush(alpha = if (highlighted) 0.75f else if (dark) 0.55f else 0.40f),
                    RoundedCornerShape(18.dp)
                )
                .then(gestureModifier)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                glyph,
                color = onSurface,
                fontSize = 20.sp,
                fontFamily = GlassHeadingFont,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = onSurface,
                fontSize = 11.sp,
                fontFamily = GlassHeadingFont,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Text(
                subtitle,
                color = subtle,
                fontSize = 8.sp,
                fontFamily = GlassContentFont,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }

        // Top specular sheen — the Aero glass hallmark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .padding(horizontal = 14.dp)
                .offset(y = 2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, highlight, highlight, Color.Transparent)
                    )
                )
        )
    }
}
