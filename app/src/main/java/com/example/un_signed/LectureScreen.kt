package com.example.un_signed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.math.roundToInt

data class LectureTopic(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isChecked: Boolean = false
)

private val cardBg = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
)
private val cardBorder = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.4f), Color.Transparent, Color.White.copy(alpha = 0.2f))
)

@Composable
fun LectureScreen(
    titleFont: FontFamily,
    contentFont: FontFamily,
    onBack: () -> Unit,
    onClose: () -> Unit,
    initialName: String = "",
    initialDurationMin: Int = 0,
    initialTopics: List<LectureTopic> = emptyList(),
    onSaveAndBegin: (name: String, totalMin: Int, topics: List<LectureTopic>, progress: Float) -> Unit = { _, _, _, _ -> }
) {
    var lectureName by remember { mutableStateOf(initialName) }
    var lectureDuration by remember { mutableStateOf(if (initialDurationMin > 0) initialDurationMin.toString() else "") }
    val topics = remember { mutableStateListOf<LectureTopic>().apply { addAll(initialTopics) } }
    var showTopicDialog by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.5.dp, cardBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = LocalStrings.current.lecture,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = titleFont,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 8f)),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 1. Name
                LectureField(
                    label = LocalStrings.current.lectureName,
                    value = lectureName,
                    onValueChange = { lectureName = it },
                    placeholder = LocalStrings.current.lectureName,
                    titleFont = titleFont,
                    contentFont = contentFont
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Duration (tap opens clock picker)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        LocalStrings.current.totalDurationMin,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        fontFamily = titleFont
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            .clickable { showDurationPicker = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp)
                    ) {
                        val totalMin = lectureDuration.toIntOrNull() ?: 0
                        val h = totalMin / 60
                        val m = totalMin % 60
                        Text(
                            text = when {
                                lectureDuration.isEmpty() -> LocalStrings.current.exampleDuration
                                h > 0 -> "${h}h ${String.format("%02d", m)}m"
                                else  -> "${m}m"
                            },
                            color = if (lectureDuration.isEmpty())
                                Color.White.copy(alpha = 0.22f)
                            else
                                Color.White,
                            fontSize = 18.sp,
                            fontFamily = contentFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                Spacer(modifier = Modifier.height(18.dp))

                // 2. Topics header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        LocalStrings.current.topics,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 17.sp,
                        fontFamily = titleFont
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF09e8ad).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF09e8ad).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { showTopicDialog = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (topics.isEmpty()) LocalStrings.current.addTopics else LocalStrings.current.editTopics,
                            color = Color(0xFF09e8ad),
                            fontSize = 13.sp,
                            fontFamily = titleFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Topic checklist
                if (topics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        topics.forEachIndexed { idx, topic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = topic.isChecked,
                                    onCheckedChange = { checked ->
                                        topics[idx] = topic.copy(isChecked = checked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF09e8ad),
                                        uncheckedColor = Color.White.copy(alpha = 0.35f),
                                        checkmarkColor = Color.Black
                                    )
                                )
                                Text(
                                    text = "${idx + 1}. ${topic.name}",
                                    color = if (topic.isChecked) Color(0xFF09e8ad) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 15.sp,
                                    fontFamily = contentFont,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                Spacer(modifier = Modifier.height(18.dp))

                // 4. Pie chart (visible only when topics exist)
                if (topics.isNotEmpty()) {
                    Text(
                        text = LocalStrings.current.progress,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        fontFamily = titleFont,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LectureDualPie(topics = topics, totalDurationStr = lectureDuration, font = contentFont)
                    Spacer(modifier = Modifier.height(22.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 5. SAVE & BEGIN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFF9500))
                        .clickable {
                            val progress = if (topics.isEmpty()) 0f
                                           else topics.count { it.isChecked }.toFloat() / topics.size
                            onSaveAndBegin(lectureName, lectureDuration.toIntOrNull() ?: 0, topics.toList(), progress)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LocalStrings.current.saveAndBegin,
                        color = Color.Black,
                        fontSize = 20.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "BACK",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 16.sp,
                    fontFamily = titleFont,
                    modifier = Modifier.clickable { onBack() }
                )
            }
        }

        // Duration clock picker
        if (showDurationPicker) {
            DurationClockPicker(
                initialMinutes = lectureDuration.toIntOrNull() ?: 0,
                titleFont = titleFont,
                contentFont = contentFont,
                onConfirm = { totalMin ->
                    lectureDuration = totalMin.toString()
                    showDurationPicker = false
                },
                onDismiss = { showDurationPicker = false }
            )
        }

        // Topic dialog on top
        if (showTopicDialog) {
            TopicInputDialog(
                existingTopics = topics.map { it.name },
                titleFont = titleFont,
                contentFont = contentFont,
                onSave = { names ->
                    val filtered = names.filter { it.isNotBlank() }
                    val checkedMap = topics.associate { it.name to it.isChecked }
                    topics.clear()
                    topics.addAll(filtered.map { n ->
                        LectureTopic(name = n, isChecked = checkedMap[n] ?: false)
                    })
                    showTopicDialog = false
                },
                onDismiss = { showTopicDialog = false }
            )
        }
    }
}

@Composable
private fun LectureField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    titleFont: FontFamily,
    contentFont: FontFamily,
    numeric: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontFamily = titleFont
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = contentFont),
            keyboardOptions = if (numeric)
                KeyboardOptions(keyboardType = KeyboardType.Number)
            else
                KeyboardOptions.Default,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Color.White.copy(alpha = 0.22f),
                        fontSize = 18.sp,
                        fontFamily = contentFont
                    )
                }
                inner()
            }
        )
    }
}

@Composable
fun TopicInputDialog(
    existingTopics: List<String>,
    titleFont: FontFamily,
    contentFont: FontFamily,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val inputs = remember {
        mutableStateListOf<String>().apply {
            addAll(if (existingTopics.isEmpty()) listOf("") else existingTopics)
        }
    }
    var idx by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1C1C2E), Color(0xFF0D0D1A))))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOPIC ${idx + 1}",
                color = Color.White,
                fontSize = 22.sp,
                fontFamily = titleFont
            )
            Text(
                text = "${idx + 1} of ${inputs.size}",
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 12.sp,
                fontFamily = contentFont
            )

            Spacer(modifier = Modifier.height(20.dp))

            BasicTextField(
                value = inputs.getOrElse(idx) { "" },
                onValueChange = { v ->
                    while (inputs.size <= idx) inputs.add("")
                    inputs[idx] = v
                },
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = contentFont),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color(0xFF09e8ad).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (inputs.getOrElse(idx) { "" }.isEmpty()) {
                        Text(
                            "Topic name...",
                            color = Color.White.copy(alpha = 0.22f),
                            fontSize = 18.sp,
                            fontFamily = contentFont
                        )
                    }
                    inner()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DialogNavBtn(
                    text = "← PREV",
                    color = Color.White.copy(alpha = 0.5f),
                    enabled = idx > 0,
                    font = titleFont,
                    modifier = Modifier.weight(1f)
                ) { if (idx > 0) idx-- }

                DialogNavBtn(
                    text = "SAVE",
                    color = Color(0xFFFF9500),
                    enabled = true,
                    font = titleFont,
                    modifier = Modifier.weight(1f)
                ) { onSave(inputs.toList()) }

                DialogNavBtn(
                    text = "NEXT →",
                    color = Color(0xFF09e8ad),
                    enabled = true,
                    font = titleFont,
                    modifier = Modifier.weight(1f)
                ) {
                    if (idx == inputs.size - 1) inputs.add("")
                    idx++
                }
            }
        }
    }
}

@Composable
private fun DialogNavBtn(
    text: String,
    color: Color,
    enabled: Boolean,
    font: FontFamily,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.3f
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (enabled) 0.15f else 0.05f))
            .border(1.dp, color.copy(alpha = if (enabled) 0.5f else 0.15f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color.copy(alpha = alpha),
            fontSize = 12.sp,
            fontFamily = font,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LectureDualPie(
    topics: List<LectureTopic>,
    totalDurationStr: String,
    font: FontFamily
) {
    val checkedCount = topics.count { it.isChecked }
    val total = topics.size
    val totalMin = totalDurationStr.toIntOrNull() ?: 0
    val coveredMin = if (total > 0 && totalMin > 0)
        (checkedCount.toFloat() / total * totalMin).roundToInt() else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PieSlice(
            covered = checkedCount,
            total = total,
            ringColor = Color(0xFF09e8ad),
            label = "TOPICS",
            valueText = "$checkedCount / $total",
            font = font
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(110.dp)
                .background(Color.White.copy(alpha = 0.12f))
        )

        PieSlice(
            covered = coveredMin,
            total = maxOf(totalMin, 1),
            ringColor = Color(0xFFEBC174),
            label = "DURATION",
            valueText = "${coveredMin}m / ${totalMin}m",
            font = font
        )
    }
}

@Composable
private fun PieSlice(
    covered: Int,
    total: Int,
    ringColor: Color,
    label: String,
    valueText: String,
    font: FontFamily
) {
    val sweep = if (total > 0) (covered.toFloat() / total * 360f).coerceIn(0f, 360f) else 0f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(90.dp)) {
            // Background circle
            drawCircle(color = Color.White.copy(alpha = 0.1f))
            // Covered sector
            if (sweep > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = true
                )
            }
            // Donut hole
            drawCircle(color = Color(0xFF0A0A12), radius = size.minDimension / 3.6f)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = valueText,
            color = ringColor,
            fontSize = 14.sp,
            fontFamily = font,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontFamily = font
        )
    }
}
