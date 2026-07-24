package com.example.un_signed

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                style = androidx.compose.ui.text.TextStyle(brush = Brush.linearGradient(listOf(BronzeTextStart, BronzeTextEnd)))
            )

            Spacer(modifier = Modifier.weight(1f))
            Icon(painter = painterResource(id = R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.size(24.dp), tint = ChevronStart.copy(alpha = glowPulse))
        }
    }
}

// Modes: 0=24h  1=12h  2=Timer  3=Stopwatch  4=Alarm  5=Day+Date  → 6th tap wraps to 0
@Composable
fun NixieClock(
    fontFamily: FontFamily,
    onClick: () -> Unit,
    onTimerTap: (advanceMode: () -> Unit) -> Unit = {},
    onStopwatchTap: (advanceMode: () -> Unit) -> Unit = {}
) {
    var mode by remember { mutableStateOf(0) }
    var time by remember { mutableStateOf(LocalDateTime.now()) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    if (showAlarmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = {
                Text("Open Alarm Clock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text("Take you to the phone's alarm clock?", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showAlarmDialog = false
                    try { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) } catch (_: Exception) {}
                }) {
                    Text("YES", color = OrangeFire, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showAlarmDialog = false
                    mode = (mode + 1) % 6   // SKIP → advance to next mode
                }) {
                    Text("SKIP", color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when (mode) {
                    2 -> onTimerTap { mode = 3 }
                    3 -> onStopwatchTap { mode = 4 }
                    4 -> showAlarmDialog = true
                    else -> { mode = (mode + 1) % 6 }
                }
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

// Uses system font so letters are always crisp — pixel font (jerseyFont) garbles at small sizes
@Composable
fun NixieActionLabel(text: String) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF110505))
            .border(1.dp, SciFiRed.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = SciFiRed.copy(alpha = 0.4f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            letterSpacing = 1.sp,
            style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = SciFiRed, blurRadius = 24f))
        )
        Text(
            text = text,
            color = Color(0xFFFF5555),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
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

@Composable
fun NixieTubeDigit(digit: String, fontFamily: FontFamily) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0D0D))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            color = OrangeFire.copy(alpha = 0.4f),
            fontSize = 46.sp,
            fontFamily = fontFamily,
            style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = OrangeFire, blurRadius = 30f))
        )
        Text(
            text = digit,
            color = Color(0xFFFFD500),
            fontSize = 46.sp,
            fontFamily = fontFamily,
            letterSpacing = 2.sp,
            style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 2f))
        )
    }
}

@Composable
fun NixieColon() {
    Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.Center) {
        repeat(2) {
            Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(OrangeFire))
            if (it == 0) Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun GlassDialogContent(titleFont: FontFamily, buttonFont: FontFamily, onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))))
                .border(width = 1.5.dp, brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent, Color.White.copy(alpha = 0.2f))), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "IDEAL OPTIONS",
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = titleFont,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 32.dp),
                style = androidx.compose.ui.text.TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f))
            )
            GlassButton("1. EDUCATION", buttonFont)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("2. HEALTH", buttonFont)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("3. SKILL", buttonFont)
            Spacer(modifier = Modifier.height(16.dp))
            GlassButton("4. PEACE", buttonFont)
        }
    }
}

@Composable
fun GlassButton(text: String, fontFamily: FontFamily) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(14.dp))
            .clickable { },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White,
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
    onEventClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val upcomingTasks = allTasks.filterKeys { !it.isBefore(today) }
        .flatMap { (date, tasks) -> tasks.map { date to it } }
        .sortedBy { it.first }

    if (upcomingTasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(
                text = "no events on record",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 18.sp,
                fontFamily = fontFamily,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                            text = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")),
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
