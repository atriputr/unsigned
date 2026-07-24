package com.example.un_signed

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun NixieClock(fontFamily: FontFamily, onClick: () -> Unit) {
    var time by remember { mutableStateOf(LocalDateTime.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            time = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    val h = time.format(DateTimeFormatter.ofPattern("HH"))
    val m = time.format(DateTimeFormatter.ofPattern("mm"))
    val s = time.format(DateTimeFormatter.ofPattern("ss"))

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
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
