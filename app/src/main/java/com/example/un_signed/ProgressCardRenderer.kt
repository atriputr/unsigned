package com.example.un_signed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Renders a 1080×1920 (Instagram-story size) PNG summarising the user's progress.
 * Uses pure android.graphics — no Compose — so it runs on IO thread safely.
 */
object ProgressCardRenderer {

    private const val W = 1080
    private const val H = 1920

    /** Colour palette used inside the card. */
    private object Col {
        val bgTop     = 0xFF06060C.toInt()
        val bgBot     = 0xFF14121F.toInt()
        val stroke    = 0xFFEBC174.toInt()
        val orange    = 0xFFFF8A00.toInt()
        val water     = 0xFF4EA8DE.toInt()
        val exercise  = 0xFF8CD86A.toInt()
        val sleep     = 0xFFB19CFF.toInt()
        val streak    = 0xFFFFC848.toInt()
        val soft      = 0xFFCFCFDA.toInt()
        val faint     = 0xFF6B6A80.toInt()
        val onSurface = 0xFFFFFFFF.toInt()
    }

    /** Build the bitmap. Never throws — best-effort. */
    fun render(context: Context): Bitmap {
        val profile   = FitDataRepository.loadUserProfile()
        val prefs     = FitDataRepository.loadAppPreferences()
        val snapshot  = InsightsEngine.snapshot(profile)
        val weather   = FitDataRepository.loadWeatherCache()
        val skills    = FitDataRepository.loadSkillItems().sortedByDescending { it.totalMinutes }
        val focusToday = FitDataRepository.loadFocusSessions().filter {
            java.time.Instant.ofEpochMilli(it.startedAtEpochMs)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
        }.sumOf { it.totalFocusMinutes }

        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bebas  = safeTypeface(context, R.font.bebas_neue)
        val nokia  = safeTypeface(context, R.font.nokia_kokia)

        // ── Background gradient ───────────────────────────
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), Col.bgTop, Col.bgBot, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bgPaint)

        // Subtle radial glow at top
        val glowPaint = Paint().apply {
            shader = RadialGradient(W / 2f, 220f, 900f,
                intArrayOf(0x33FF8A00.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), glowPaint)

        // ── Header — Brand + date ─────────────────────────
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.stroke; textAlign = Paint.Align.CENTER
            typeface = bebas; textSize = 72f; letterSpacing = 0.35f
        }
        canvas.drawText("UN-SIGNED", W / 2f, 190f, brandPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.faint; textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE; textSize = 30f
        }
        canvas.drawText(
            "PROGRESS SNAPSHOT · ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
            W / 2f, 240f, subPaint
        )

        // ── Name greeting ─────────────────────────────────
        val greetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.onSurface; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 68f
        }
        val name = profile.name.ifBlank { "You" }
        canvas.drawText(name.uppercase(), W / 2f, 380f, greetPaint)

        // Divider
        val divPaint = Paint().apply {
            color = Col.stroke; alpha = 60; strokeWidth = 2f
        }
        canvas.drawLine(120f, 430f, W - 120f, 430f, divPaint)

        // ── Metric ring — water % ─────────────────────────
        val waterCx = W / 2f
        val waterCy = 700f
        val waterR  = 200f
        drawRing(canvas, waterCx, waterCy, waterR, snapshot.waterPercent.coerceAtMost(1f), Col.water, 28f)
        val bigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.onSurface; textAlign = Paint.Align.CENTER
            typeface = nokia; textSize = 130f
        }
        val pct = (snapshot.waterPercent * 100).toInt().coerceAtMost(200)
        canvas.drawText("$pct%", waterCx, waterCy + 45f, bigPaint)

        val ringLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.water; textAlign = Paint.Align.CENTER
            typeface = bebas; textSize = 34f; letterSpacing = 0.30f
        }
        canvas.drawText("HYDRATION", waterCx, waterCy + 90f, ringLabelPaint)

        // ── 2×2 metric grid ───────────────────────────────
        val gridTop = 1000f
        val col1 = 80f
        val col2 = W / 2f + 20f
        val cellW = W - 200f
        val halfW = (cellW - 20f) / 2f
        val cellH = 260f

        // Exercise
        drawMetricCell(canvas, col1, gridTop, halfW, cellH,
            label = "EXERCISE · WEEK",
            value = "${snapshot.exerciseWeekMin}m",
            detail = "of 150 (WHO)",
            accent = Col.exercise,
            bebas = bebas, nokia = nokia,
            streak = snapshot.streakExercise
        )

        // Sleep
        drawMetricCell(canvas, col2, gridTop, halfW, cellH,
            label = "SLEEP · LAST",
            value = if (snapshot.sleepLastHours > 0) "%.1fh".format(snapshot.sleepLastHours) else "—",
            detail = "target %.1fh".format(snapshot.sleepTargetHours),
            accent = Col.sleep,
            bebas = bebas, nokia = nokia,
            streak = snapshot.streakSleep
        )

        // Focus today
        drawMetricCell(canvas, col1, gridTop + cellH + 30f, halfW, cellH,
            label = "FOCUS · TODAY",
            value = "${focusToday}m",
            detail = if (focusToday > 0) "in flow" else "no session yet",
            accent = Col.orange,
            bebas = bebas, nokia = nokia,
            streak = 0
        )

        // Junk-clean streak
        drawMetricCell(canvas, col2, gridTop + cellH + 30f, halfW, cellH,
            label = "JUNK · TODAY",
            value = "${snapshot.junkToday}",
            detail = "avg %.1f/day".format(snapshot.junkWeekAvg),
            accent = if (snapshot.junkToday == 0) Col.exercise else Col.orange,
            bebas = bebas, nokia = nokia,
            streak = snapshot.streakJunkClean,
            streakLabel = "clean"
        )

        // ── vs GLOBAL bands ────────────────────────────────
        val sections = ComparisonEngine.buildAll(profile, prefs)
        val topBands = sections.flatMap { it.comparisons }.take(4)
        if (topBands.isNotEmpty()) {
            val vsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF6ACBEA.toInt(); typeface = bebas; textSize = 28f; letterSpacing = 0.30f
            }
            canvas.drawText("VS GLOBAL", 80f, 1520f, vsPaint)
            var y = 1560f
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Col.onSurface; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL); textSize = 24f
            }
            val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = bebas ?: Typeface.SANS_SERIF; textSize = 20f; letterSpacing = 0.15f
                textAlign = Paint.Align.RIGHT
            }
            topBands.forEach { c ->
                canvas.drawText(c.metric.take(24), 80f, y, labelPaint)
                bandPaint.color = when (c.band) {
                    GlobalNorms.Band.Excellent -> 0xFF8CD86A.toInt()
                    GlobalNorms.Band.Healthy   -> 0xFF6FC1FF.toInt()
                    GlobalNorms.Band.Concern   -> 0xFFFFC848.toInt()
                    GlobalNorms.Band.AtRisk    -> 0xFFFF6666.toInt()
                    else                       -> 0xFF7F7F7F.toInt()
                }
                canvas.drawText(c.bandLabel.uppercase(), W - 80f, y, bandPaint)
                y += 42f
            }
        }

        // ── Top skills ─────────────────────────────────────
        if (skills.isNotEmpty()) {
            val skPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Col.stroke; typeface = bebas; textSize = 28f; letterSpacing = 0.30f
            }
            canvas.drawText("TOP SKILLS", 80f, 1750f, skPaint)
            var y = 1790f
            val skNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Col.onSurface; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL); textSize = 24f
            }
            val skTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Col.orange; typeface = nokia; textSize = 26f; textAlign = Paint.Align.RIGHT
            }
            skills.take(2).forEach { s ->
                canvas.drawText(s.name.take(28), 80f, y, skNamePaint)
                canvas.drawText(formatMin(s.totalMinutes), W - 80f, y, skTimePaint)
                y += 40f
            }
        }

        // ── Footer — weather + streaks ────────────────────
        val footerY = 1820f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.faint; textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE; textSize = 26f
        }
        val streakPieces = mutableListOf<String>()
        if (snapshot.streakWater >= 2) streakPieces += "💧${snapshot.streakWater}d"
        if (snapshot.streakExercise >= 2) streakPieces += "🏃${snapshot.streakExercise}d"
        if (snapshot.streakSleep >= 2) streakPieces += "😴${snapshot.streakSleep}d"
        val streakLine = if (streakPieces.isNotEmpty()) streakPieces.joinToString("  ·  ") else "start a streak today"
        canvas.drawText(streakLine, W / 2f, footerY, footerPaint)

        if (weather.isValid) {
            val wPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Col.faint; textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE; textSize = 22f
            }
            canvas.drawText(
                "${Units.displayTemp(weather.temperatureC, prefs.tempUnit)} · ${weather.condition}",
                W / 2f, footerY + 40f, wPaint
            )
        }

        return bitmap
    }

    /** Write PNG to filesDir/final progress/ and return the file. */
    fun saveToFinalProgress(context: Context, bitmap: Bitmap, filename: String): File {
        val dir = File(context.filesDir, "final progress").also { it.mkdirs() }
        val file = File(dir, filename)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    /** Save arbitrary bytes to the same folder. */
    fun saveBytesToFinalProgress(context: Context, bytes: ByteArray, filename: String): File {
        val dir = File(context.filesDir, "final progress").also { it.mkdirs() }
        val file = File(dir, filename)
        file.writeBytes(bytes)
        return file
    }

    // ── drawing helpers ───────────────────────────────────
    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, r: Float, progress: Float, color: Int, stroke: Float) {
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0xFF1A1A28.toInt(); this.style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND
        }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; this.style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND
        }
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; this.style = Paint.Style.STROKE; strokeWidth = stroke * 2f; strokeCap = Paint.Cap.ROUND
            alpha = 60
        }
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(rect, 0f, 360f, false, track)
        val sweep = (progress.coerceIn(0f, 1f) * 360f)
        if (sweep > 0f) {
            canvas.drawArc(rect, -90f, sweep, false, glow)
            canvas.drawArc(rect, -90f, sweep, false, ring)
        }
    }

    private fun drawMetricCell(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
        label: String, value: String, detail: String,
        accent: Int, bebas: Typeface?, nokia: Typeface?,
        streak: Int, streakLabel: String = "streak"
    ) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000; style = Paint.Style.FILL }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.STROKE; strokeWidth = 2f; alpha = 90 }
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 28f, 28f, bg)
        canvas.drawRoundRect(rect, 28f, 28f, border)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent; typeface = bebas ?: Typeface.SANS_SERIF; textSize = 28f; letterSpacing = 0.30f
        }
        canvas.drawText(label, x + 26f, y + 55f, labelPaint)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.onSurface; typeface = nokia ?: Typeface.MONOSPACE; textSize = 88f
        }
        canvas.drawText(value, x + 26f, y + 150f, valuePaint)

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.soft; typeface = Typeface.SANS_SERIF; textSize = 26f; alpha = 200
        }
        canvas.drawText(detail, x + 26f, y + 195f, detailPaint)

        if (streak >= 2) {
            val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Col.streak; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); textSize = 26f
            }
            canvas.drawText("🔥 $streak d $streakLabel", x + 26f, y + 235f, streakPaint)
        }
    }

    private fun safeTypeface(context: Context, resId: Int): Typeface? = try {
        ResourcesCompat.getFont(context, resId)
    } catch (_: Exception) { null }

    private fun formatMin(m: Int): String {
        if (m < 60) return "${m}m"
        return "${m / 60}h${(m % 60).let { if (it > 0) it.toString() + "m" else "" }}"
    }
}
