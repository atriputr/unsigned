package com.example.un_signed

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * One-stop export flow.
 *   • Renders the progress-card PNG
 *   • Generates HTML + PPTX
 *   • Writes all three into filesDir/"final progress"/ with a timestamped stem
 *   • Provides share intents for whichever slice the user wants
 */
object ProgressExporter {

    data class SavedBundle(
        val stem: String,
        val png: File,
        val html: File,
        val pptx: File
    )

    private fun stampNow(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))

    /** Render everything and write to filesDir/final progress/. Never throws — best-effort. */
    fun saveAll(context: Context, from: LocalDate, to: LocalDate, periodIdx: Int): SavedBundle {
        val stamp = stampNow()
        val stem  = "unsigned_$stamp"

        // 1. Progress card PNG
        val bitmap = ProgressCardRenderer.render(context)
        val pngBytes = java.io.ByteArrayOutputStream().apply {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, this)
        }.toByteArray()
        val pngFile = ProgressCardRenderer.saveToFinalProgress(context, bitmap, "$stem.png")

        // 2. HTML report (via existing generator)
        val html = ReportGenerator.generate(
            sessions       = FitDataRepository.loadSessions(),
            subjects       = FitDataRepository.loadSubjects(),
            courses        = FitDataRepository.loadCourses(),
            practices      = FitDataRepository.loadPractices(),
            habits         = FitDataRepository.loadHabits(),
            calendarTasks  = FitDataRepository.loadCalendarTasks(),
            junkHistory    = FitDataRepository.loadJunkHistory(),
            from = from, to = to, periodIdx = periodIdx
        )
        val htmlFile = ProgressCardRenderer.saveBytesToFinalProgress(context, html.toByteArray(), "$stem.html")

        // 3. PPTX — single slide with the progress card
        val pptxBytes = try { PptxGenerator.build(listOf(pngBytes), "Un-signed Progress · $stamp") } catch (_: Exception) { ByteArray(0) }
        val pptxFile = if (pptxBytes.isNotEmpty())
            ProgressCardRenderer.saveBytesToFinalProgress(context, pptxBytes, "$stem.pptx")
        else File(context.filesDir, "final progress/$stem.pptx")

        return SavedBundle(stem = stem, png = pngFile, html = htmlFile, pptx = pptxFile)
    }

    /** Convert a saved file into a content:// URI via our FileProvider. */
    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Share the PNG (Insta / WhatsApp / any social app friendly). */
    fun shareImage(context: Context, pngFile: File) {
        try {
            val uri = uriFor(context, pngFile)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "My progress this week — via un-signed.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share progress").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    /** Share the PPTX (for slack/mail/drive). */
    fun sharePptx(context: Context, pptxFile: File) {
        try {
            val uri = uriFor(context, pptxFile)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Un-signed Progress · ${LocalDate.now()}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share PPTX").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    /** Share the HTML — useful for cross-platform detailed viewing. */
    fun shareHtml(context: Context, htmlFile: File) {
        try {
            val uri = uriFor(context, htmlFile)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Un-signed Report · ${LocalDate.now()}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    /** Share all three files together (email/slack/drive can handle multi-attachment). */
    fun shareBundle(context: Context, bundle: SavedBundle) {
        try {
            val uris = arrayListOf(uriFor(context, bundle.png), uriFor(context, bundle.pptx), uriFor(context, bundle.html))
            val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "Un-signed Progress Bundle · ${LocalDate.now()}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share progress bundle").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }
}
