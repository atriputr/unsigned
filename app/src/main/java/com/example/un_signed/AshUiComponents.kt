package com.example.un_signed

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// COLOR PALETTE
val SciFiRed = Color(0xFFE41417)
val ButtonMetalBg = Color(0xFF151010)
val GrungeEdgeColor = Color(0xFFBDB0AA)
val IconRingStart = Color(0xFFFF9D00)
val IconRingEnd = Color(0xFFFFE066)
val DividerColor = Color(0xFFE4551A)
val BronzeTextStart = Color(0xFFB07D4F)
val BronzeTextEnd = Color(0xFFC8935F)
val ChevronStart = Color(0xFFF68F2D)
val OrangeFire = Color(0xFFFF8A00)

@Composable
fun AshButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1.0f, label = "scale")
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowPulse"
    )

    Box(
        modifier = modifier
            .width(320.dp)
            .height(98.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val random = Random(42)

            val pathTop = Path()
            val pathBottom = Path()
            val steps = 120
            val stepSize = w / steps
            
            pathTop.moveTo(0f, h/2)
            pathBottom.moveTo(0f, h/2)

            for (i in 0..steps) {
                val x = i * stepSize
                val noiseTop = (random.nextFloat() - 0.5f) * 18f
                val noiseBottom = (random.nextFloat() - 0.5f) * 18f
                val edgeFade = if (i < 15) i / 15f else if (i > steps - 15) (steps - i) / 15f else 1f
                pathTop.lineTo(x, (h * 0.15f) + (noiseTop * edgeFade))
                pathBottom.lineTo(x, (h * 0.85f) + (noiseBottom * edgeFade))
            }
            pathTop.lineTo(w, h/2); pathTop.close()
            pathBottom.lineTo(w, h/2); pathBottom.close()

            drawPath(pathTop, color = GrungeEdgeColor.copy(alpha = 0.35f))
            drawPath(pathBottom, color = GrungeEdgeColor.copy(alpha = 0.35f))

            val metalPath = Path().apply {
                moveTo(5f, h * 0.22f)
                lineTo(w - 5f, h * 0.22f)
                lineTo(w - 5f, h * 0.78f)
                lineTo(5f, h * 0.78f)
                close()
            }
            drawPath(path = metalPath, brush = Brush.verticalGradient(listOf(Color(0xFF221A1A), ButtonMetalBg, Color(0xFF0A0808))))
            
            for (i in 0..20) {
                val y = (h * 0.25f) + (random.nextFloat() * h * 0.5f)
                drawLine(color = Color.White.copy(alpha = 0.05f), start = Offset(15f, y), end = Offset(w - 15f, y), strokeWidth = 1f)
            }
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    drawCircle(brush = Brush.radialGradient(listOf(IconRingStart.copy(alpha = 0.3f), Color.Transparent)), radius = size.width / 1.2f)
                    drawCircle(brush = Brush.sweepGradient(listOf(IconRingStart, IconRingEnd, IconRingStart)), style = Stroke(width = 1.5.dp.toPx()))
                    drawCircle(color = Color.Black, radius = size.width / 2 - 1.5.dp.toPx())
                }
                Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = IconRingStart, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))
            Canvas(modifier = Modifier.width(1.dp).fillMaxHeight(0.35f)) { drawRect(color = DividerColor) }
            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = text,
                color = BronzeTextStart,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                style = TextStyle(brush = Brush.linearGradient(listOf(BronzeTextStart, BronzeTextEnd)), fontFamily = BebasFont)
            )

            Spacer(modifier = Modifier.weight(1f))
            Icon(painter = painterResource(id = R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(24.dp), tint = ChevronStart.copy(alpha = glowPulse))
        }
    }
}

@Composable
fun NixieClock(
    fontFamily: FontFamily,
    onClick: () -> Unit,
    onTimerTap: (advanceMode: () -> Unit) -> Unit = {},
    onStopwatchTap: (advanceMode: () -> Unit) -> Unit = {}
) {
    var mode           by remember { mutableStateOf(0) }
    var time           by remember { mutableStateOf(LocalDateTime.now()) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showModeMenu   by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalDateTime.now()
            delay(1000)
        }
    }

    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = {
                Text("Open Alarm Clock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = BebasFont)
            },
            text = {
                Text("Take you to the phone's alarm clock?", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, fontFamily = BebasFont)
            },
            confirmButton = {
                TextButton(onClick = {
                    showAlarmDialog = false
                    try { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) } catch (_: Exception) {}
                }) {
                    Text("YES", color = OrangeFire, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = BebasFont)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAlarmDialog = false
                    mode = (mode + 1) % 6   // SKIP → advance to next mode
                }) {
                    Text("SKIP", color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp, fontFamily = BebasFont)
                }
            }
        )
    }

    if (showModeMenu) {
        Dialog(onDismissRequest = { showModeMenu = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xCC1A1A2E), Color(0xCC0D0D1A))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                    .padding(vertical = 20.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "CLOCK MODE",
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontFamily = BebasFont
                )
                Spacer(Modifier.height(14.dp))

                val entries = listOf(
                    Triple(0, "24-HR",      "24-hour digital clock"),
                    Triple(1, "12-HR",      "12-hour with AM / PM"),
                    Triple(2, "TIMER",      "Set a countdown timer"),
                    Triple(3, "STOPWATCH",  "Lap & split timer"),
                    Triple(4, "ALARM",      "Open phone alarm clock"),
                    Triple(5, "DAY & DATE", "Show today's day and date")
                )

                entries.forEach { (idx, label, sub) ->
                    val isCurrent = mode == idx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrent) OrangeFire.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable {
                                showModeMenu = false
                                when (idx) {
                                    2 -> onTimerTap { mode = 3 }
                                    3 -> onStopwatchTap { mode = 4 }
                                    4 -> { mode = 4; showAlarmDialog = true }
                                    else -> mode = idx
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                label,
                                color = if (isCurrent) OrangeFire else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = BebasFont
                            )
                            Text(
                                sub,
                                color = Color.White.copy(alpha = 0.38f),
                                fontSize = 11.sp,
                                fontFamily = BebasFont
                            )
                        }
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(OrangeFire)
                            )
                        }
                    }

                    if (idx < 5) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.07f))
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        when (mode) {
                            2 -> onTimerTap { mode = 3 }
                            3 -> onStopwatchTap { mode = 4 }
                            4 -> showAlarmDialog = true
                            else -> mode = (mode + 1) % 6
                        }
                    },
                    onLongPress = { showModeMenu = true }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (mode) {
            0 -> {
                val h = time.format(DateTimeFormatter.ofPattern("HH"))
                val m = time.format(DateTimeFormatter.ofPattern("mm"))
                val s = time.format(DateTimeFormatter.ofPattern("ss"))
                ClockLayout(h, m, s, fontFamily)
            }
            1 -> {
                val h    = time.format(DateTimeFormatter.ofPattern("hh"))
                val m    = time.format(DateTimeFormatter.ofPattern("mm"))
                val s    = time.format(DateTimeFormatter.ofPattern("ss"))
                val amPm = time.format(DateTimeFormatter.ofPattern("a")).uppercase()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ClockLayout(h, m, s, fontFamily)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        amPm.forEach { NixieTubeDigit(it.toString(), fontFamily) }
                    }
                }
            }
            2 -> NixieActionLabel("BEGIN A TIMER")
            3 -> NixieActionLabel("BEGIN STOPWATCH")
            4 -> NixieActionLabel("SET AN ALARM")
            5 -> {
                val dayName  = time.format(DateTimeFormatter.ofPattern("EEE")).uppercase()
                val dayNum   = time.format(DateTimeFormatter.ofPattern("dd"))
                val monthAbb = time.format(DateTimeFormatter.ofPattern("MMM")).uppercase()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        dayName.forEach { NixieTubeDigit(it.toString(), fontFamily) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        dayNum.forEach { NixieTubeDigit(it.toString(), fontFamily) }
                        Spacer(Modifier.width(8.dp))
                        monthAbb.forEach { NixieTubeDigit(it.toString(), fontFamily) }
                    }
                }
            }
        }
    }
}

@Composable
fun NixieActionLabel(text: String, fontFamily: FontFamily = BebasFont) {
    val palette = LocalPalette.current
    val (bg, accent, text2) = when (palette.name) {
        "CREAM" -> Triple(Color(0xFFFFEDF5), Color(0xFFFF6EA8), Color(0xFFE33D8B))
        "AMBER" -> Triple(Color(0xFF1A0B02), Color(0xFFFFB454), Color(0xFFFFD79A))
        else    -> Triple(Color(0xFF110505), SciFiRed, Color(0xFFFF5555))
    }
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accent.copy(alpha = 0.4f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            letterSpacing = 1.sp,
            style = TextStyle(shadow = Shadow(color = accent, blurRadius = 24f))
        )
        Text(
            text = text,
            color = text2,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ClockLayout(h: String, m: String, s: String, fontFamily: FontFamily) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        NixieTubeDigit(h[0].toString(), fontFamily)
        Spacer(modifier = Modifier.width(4.dp))
        NixieTubeDigit(h[1].toString(), fontFamily)
        NixieColon()
        NixieTubeDigit(m[0].toString(), fontFamily)
        Spacer(modifier = Modifier.width(4.dp))
        NixieTubeDigit(m[1].toString(), fontFamily)
        NixieColon()
        NixieTubeDigit(s[0].toString(), fontFamily)
        Spacer(modifier = Modifier.width(4.dp))
        NixieTubeDigit(s[1].toString(), fontFamily)
    }
}

/** Per-theme accent tint for the nixie tube — orange (dark) · baby pink (cream) · warm amber (amber). */
@Composable
private fun nixieAccent(): Triple<Color, Color, Color> {
    // Returns (tubeBg, glowColor, digitColor)
    val palette = LocalPalette.current
    return when (palette.name) {
        "CREAM" -> Triple(Color(0xFFFFEDF5), Color(0xFFFF6EA8), Color(0xFFE33D8B))  // baby-pink
        "AMBER" -> Triple(Color(0xFF0A0603), Color(0xFFFF8C1A), Color(0xFFFF9E38))  // deep amber glow + warm amber digits (NOT yellow)
        else    -> Triple(Color(0xFF0D0D0D), OrangeFire, Color(0xFFFFD500))          // classic orange nixie
    }
}

@Composable
fun NixieTubeDigit(digit: String, fontFamily: FontFamily) {
    val (tubeBg, glow, digitColor) = nixieAccent()
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tubeBg)
            .border(0.5.dp, if (palette.name == "CREAM") Color(0xFFFF6EA8).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            color = glow.copy(alpha = 0.4f),
            fontSize = 46.sp,
            fontFamily = fontFamily,
            style = TextStyle(shadow = Shadow(color = glow, blurRadius = 30f))
        )
        Text(
            text = digit,
            color = digitColor,
            fontSize = 46.sp,
            fontFamily = fontFamily,
            letterSpacing = 2.sp,
            style = TextStyle(shadow = Shadow(color = if (palette.name == "CREAM") Color.White else Color.Black, offset = Offset(2f, 2f), blurRadius = 2f))
        )
    }
}

@Composable
fun NixieColon() {
    val (_, glow, _) = nixieAccent()
    Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.Center) {
        repeat(2) {
            Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(glow))
            if (it == 0) Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun GlassDialogContent(
    titleFont: FontFamily,
    buttonFont: FontFamily,
    onEducationClick: () -> Unit,
    onHealthClick: () -> Unit,
    onSkillClick: () -> Unit,
    onPeaceClick: () -> Unit,
    onFocusClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBriefingClick: () -> Unit = {},
    onCompareClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(LocalPalette.current.scrim).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        val palette = LocalPalette.current
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(width = 1.5.dp, brush = palette.borderBrush(), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBarBox()
            Text(
                text = "IDEAL OPTIONS",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
            )
            GlassButton("1. EDUCATION", buttonFont, onClick = onEducationClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("2. HEALTH", buttonFont, onClick = onHealthClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("3. SKILL", buttonFont, onClick = onSkillClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("4. PEACE", buttonFont, onClick = onPeaceClick)

            Spacer(modifier = Modifier.height(20.dp))
            // Row 1: BRIEF · FOCUS · COMPARE
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(
                    "◐ BRIEF",
                    color = palette.accentPrimary.copy(alpha = 0.90f),
                    fontSize = 12.sp,
                    fontFamily = buttonFont,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBriefingClick() }
                )
                Text(
                    "◇ FOCUS",
                    color = palette.danger.copy(alpha = if (palette.isLight) 0.85f else 0.85f),
                    fontSize = 12.sp,
                    fontFamily = buttonFont,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onFocusClick() }
                )
                Text(
                    "◉ COMPARE",
                    color = palette.accentSecondary,
                    fontSize = 12.sp,
                    fontFamily = buttonFont,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onCompareClick() }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(
                    "◆ PROFILE",
                    color = palette.accentSecondary,
                    fontSize = 12.sp,
                    fontFamily = buttonFont,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onProfileClick() }
                )
                Text(
                    "◈ SETTINGS",
                    color = palette.subtle,
                    fontSize = 12.sp,
                    fontFamily = buttonFont,
                    letterSpacing = 2.sp,
                    modifier = Modifier.clickable { onSettingsClick() }
                )
            }
        }
    }
}

@Composable
fun EducationOptionsOverlay(
    titleFont: FontFamily,
    buttonFont: FontFamily,
    onLectureClick: () -> Unit = {},
    onSubjectClick: () -> Unit = {},
    onCourseClick: () -> Unit = {},
    onPracticeClick: () -> Unit = {},
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(LocalPalette.current.scrim).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        val palette = LocalPalette.current
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(width = 1.5.dp, brush = palette.borderBrush(), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBarBox()
            Text(
                text = "EDUCATION",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
            )
            GlassButton("LECTURE", buttonFont, onClick = onLectureClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("SUBJECT", buttonFont, onClick = onSubjectClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("COURSE", buttonFont, onClick = onCourseClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("PRACTICE", buttonFont, onClick = onPracticeClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BACK",
                color = palette.faint,
                fontSize = 18.sp,
                fontFamily = buttonFont,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
}

@Composable
fun HealthOptionsOverlay(
    titleFont: FontFamily,
    buttonFont: FontFamily,
    junkCount: Int,
    onJunkCountChange: (Int) -> Unit,
    onWaterClick: () -> Unit = {},
    onExerciseClick: () -> Unit = {},
    onMedsClick: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(LocalPalette.current.scrim).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        val palette = LocalPalette.current
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(width = 1.5.dp, brush = palette.borderBrush(), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBarBox()
            Text(
                text = "HEALTH",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
            )
            GlassButton("WATER", buttonFont, onClick = onWaterClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("EXERCISE", buttonFont, onClick = onExerciseClick)
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.chipBg)
                    .border(1.dp, palette.fieldBorder, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap       = { onJunkCountChange((junkCount - 1).coerceAtLeast(0)) },
                            onDoubleTap = { onJunkCountChange(junkCount + 1) }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("JUNK COUNT", color = palette.onSurface, fontSize = 22.sp, fontFamily = buttonFont, letterSpacing = 1.sp)
                    Text(
                        "$junkCount",
                        color = palette.accentPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(shadow = Shadow(color = palette.accentPrimary, blurRadius = 16f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.danger)
                    .border(1.5.dp, palette.onSurface.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SAVE",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("MEDS", buttonFont, onClick = onMedsClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BACK",
                color = palette.faint,
                fontSize = 18.sp,
                fontFamily = buttonFont,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
}

@Composable
fun MedsOptionsOverlay(
    titleFont: FontFamily,
    buttonFont: FontFamily,
    onMedsLogClick: () -> Unit = {},
    onSupplimentsClick: () -> Unit = {},
    onSevereClick: () -> Unit = {},
    onDietClick: () -> Unit = {},
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(LocalPalette.current.scrim).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        val palette = LocalPalette.current
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(width = 1.5.dp, brush = palette.borderBrush(), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBarBox()
            Text(
                text = "MEDS",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
            )
            GlassButton("MEDS LOG", buttonFont, onClick = onMedsLogClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("SUPPLIMENTS", buttonFont, onClick = onSupplimentsClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("SEVERE", buttonFont, onClick = onSevereClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("DIET", buttonFont, onClick = onDietClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BACK",
                color = palette.faint,
                fontSize = 18.sp,
                fontFamily = buttonFont,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
}

@Composable
fun SkillOptionsOverlay(
    titleFont: FontFamily,
    buttonFont: FontFamily,
    onHobbyClick: () -> Unit = {},
    onMinorClick: () -> Unit = {},
    onMajorClick: () -> Unit = {},
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(LocalPalette.current.scrim).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        val palette = LocalPalette.current
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(width = 1.5.dp, brush = palette.borderBrush(), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBarBox()
            Text(
                text = "SKILL",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
            )
            GlassButton("HOBBY", buttonFont, onClick = onHobbyClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("MINOR SKILL TO GROW", buttonFont, onClick = onMinorClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("MAJOR SKILL FOR LIFE", buttonFont, onClick = onMajorClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BACK",
                color = palette.faint,
                fontSize = 18.sp,
                fontFamily = buttonFont,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
}

@Composable
fun PeaceOptionsOverlay(
    titleFont: FontFamily,
    buttonFont: FontFamily,
    onCyclingClick: () -> Unit = {},
    onYogaClick: () -> Unit = {},
    onWalkingClick: () -> Unit = {},
    onSleepClick: () -> Unit = {},
    onHabitsClick: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(LocalPalette.current.scrim).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        val palette = LocalPalette.current
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surfaceBrush())
                .border(width = 1.5.dp, brush = palette.borderBrush(), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBarBox()
            Text(
                text = "PEACE",
                color = palette.onSurface,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = TextStyle(shadow = Shadow(color = palette.accentPrimary.copy(alpha = 0.35f), blurRadius = 8f))
            )
            GlassButton("CYCLING", buttonFont, onClick = onCyclingClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("YOGA", buttonFont, onClick = onYogaClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("WALKING", buttonFont, onClick = onWalkingClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("SLEEP", buttonFont, onClick = onSleepClick)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("HABITS", buttonFont, onClick = onHabitsClick)

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BACK",
                color = palette.faint,
                fontSize = 18.sp,
                fontFamily = buttonFont,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
}

@Composable
fun GlassButton(text: String, fontFamily: FontFamily, onClick: () -> Unit = {}) {
    val palette = LocalPalette.current
    val surfaceBrush = if (palette.isLight) {
        Brush.linearGradient(listOf(palette.onSurface.copy(alpha = 0.06f), palette.onSurface.copy(alpha = 0.02f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceBrush)
            .border(width = 1.dp, color = palette.fieldBorder, shape = RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = palette.onSurface,
            fontSize = 22.sp,
            fontFamily = fontFamily,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 24.dp)
        )
    }
}

@Composable
fun UpcomingEventsList(
    allTasks: Map<LocalDate, List<CalendarTask>>,
    fontFamily: FontFamily,
    onEventClick: (LocalDate) -> Unit,
    activeLectureName: String? = null,
    activeLectureProgress: Float = 0f,
    onLectureClick: () -> Unit = {}
) {
    val today = LocalDate.now()
    val upcomingTasks = allTasks.filterKeys { !it.isBefore(today) }
        .flatMap { (date, tasks) -> tasks.map { date to it } }
        .sortedBy { it.first }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Lecture banner (hidden when complete) ─────────────────────
        if (activeLectureName != null && activeLectureProgress < 1f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLectureClick() }
                    .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 10.dp)
            ) {
                Text(
                    text = "YOU HAVE A LECTURE  ›",
                    color = Color(0xFFEBC174),
                    fontSize = 15.sp,
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                // RED → YELLOW → GREEN spectrum thin bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val r = CornerRadius(h / 2)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f),
                        size = size,
                        cornerRadius = r
                    )
                    val fillW = (w * activeLectureProgress).coerceAtLeast(if (activeLectureProgress > 0f) 1f else 0f)
                    if (fillW > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF2200),
                                    Color(0xFFFFCC00),
                                    Color(0xFF00E676)
                                ),
                                startX = 0f,
                                endX = w
                            ),
                            size = Size(fillW, h),
                            cornerRadius = r
                        )
                    }
                }
            }
        }

        // ── Events list or empty placeholder ─────────────────────────
        if (upcomingTasks.isEmpty() && (activeLectureName == null || activeLectureProgress >= 1f)) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "no events on record",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 18.sp,
                    fontFamily = fontFamily,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        } else if (upcomingTasks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(upcomingTasks) { (date, task) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onEventClick(date) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.text,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 16.sp,
                                fontFamily = fontFamily
                            )
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                color = Color(0xFFEBC174).copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontFamily = fontFamily
                            )
                        }
                        if (task.isDone) {
                            Text("✓", color = Color(0xFF09e8ad), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBarBox() {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    
    if (topInset > 0) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { topInset.toDp() })
                .background(palette.statusBar)
        )
    }
}
