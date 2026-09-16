package com.example.un_signed

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    /** Optional ISO date the version was published — used by Update Log. */
    val date: String? = null
)

/** Root of the versions_history.json manifest, if it exists on the remote. */
data class UpdateHistoryManifest(
    val history: List<UpdateInfo> = emptyList()
)

class UpdateManager(private val context: Context) {

    private val versionUrl = "https://raw.githubusercontent.com/atriputr/unsigned/main/version.json"
    private val historyUrl = "https://raw.githubusercontent.com/atriputr/unsigned/main/versions_history.json"

    companion object {
        /**
         * Release notes for the CURRENT installed APK — hardcoded so Update Log can always show
         * "what's in this version" without needing a network round-trip. Keep this in sync with
         * the version bumps in build.gradle.kts + git history.
         */
        const val CURRENT_RELEASE_NOTES: String =
            "Glass theme with Liquid×Aero fusion. MotionParallax sensor drift for the wallpaper. " +
            "Fresh glass home skin. Refined update overlay with prism edges. " +
            "Curved-film SELECT PROFILE reel. Home weather strip blended into the ashes UI."

        /**
         * Fallback local history — used when versions_history.json isn't reachable. Each entry
         * points at a versioned APK under /archive/vX.Y/UNSIGNED.apk on the same GitHub repo, so
         * as soon as the user publishes archive builds these become live.
         */
        private const val ARCHIVE_BASE = "https://raw.githubusercontent.com/atriputr/unsigned/main/archive"

        val LOCAL_HISTORY: List<UpdateInfo> = listOf(
            UpdateInfo(
                versionCode = 27, versionName = "4.6",
                apkUrl = "$ARCHIVE_BASE/v4.6/UNSIGNED.apk",
                releaseNotes = "Glass theme (Liquid + Aero fusion). MotionParallax sensor drift. Fresh painted glass skin. Curved-film SELECT PROFILE reel.",
                date = "2025-09-14"
            ),
            UpdateInfo(
                versionCode = 26, versionName = "4.5",
                apkUrl = "$ARCHIVE_BASE/v4.5/UNSIGNED.apk",
                releaseNotes = "Fix Calendar permission launch crash. Safer system calendar query. Stability polish.",
                date = "2025-09-05"
            ),
            UpdateInfo(
                versionCode = 25, versionName = "4.4",
                apkUrl = "$ARCHIVE_BASE/v4.4/UNSIGNED.apk",
                releaseNotes = "HomeWeatherStrip + UpdateOverlay enhancements. In-app update deployment.",
                date = "2025-08-25"
            ),
            UpdateInfo(
                versionCode = 24, versionName = "4.3",
                apkUrl = "$ARCHIVE_BASE/v4.3/UNSIGNED.apk",
                releaseNotes = "HomeWeatherStrip, font refinements & in-app update build.",
                date = "2025-08-18"
            )
        )
    }

    /** Cached history so the Update Log can render instantly once first fetched. */
    private var cachedHistory: List<UpdateInfo>? = null

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Bypass cache by appending a timestamp
            val url = URL("$versionUrl?t=${System.currentTimeMillis()}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)
                
                val currentVersionCode = context.packageManager.getPackageInfo(context.packageName, 0).let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt() else it.versionCode
                }

                if (updateInfo.versionCode > currentVersionCode) {
                    return@withContext updateInfo
                } else {
                    withContext(Dispatchers.Main) {
                        // Toast.makeText(context, "App is up to date (v$currentVersionCode)", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update server error: ${connection.responseCode}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Update check failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        null
    }

    /** Current installed version code (Android P+ uses long). */
    fun currentVersionCode(): Int = try {
        context.packageManager.getPackageInfo(context.packageName, 0).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt() else it.versionCode
        }
    } catch (_: Exception) { 0 }

    fun currentVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) { "" }

    /**
     * Fetches the version history for Update Log. Tries the remote versions_history.json first;
     * on any network failure falls back to the hardcoded [LOCAL_HISTORY] baked into the app.
     * Result is sorted descending by versionCode and cached in-memory for the session.
     */
    suspend fun fetchHistory(): List<UpdateInfo> = withContext(Dispatchers.IO) {
        cachedHistory?.let { return@withContext it }

        val remote: List<UpdateInfo>? = try {
            val url = URL("$historyUrl?t=${System.currentTimeMillis()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 4000; readTimeout = 4000; useCaches = false
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                Gson().fromJson(json, UpdateHistoryManifest::class.java)?.history
            } else null
        } catch (_: Exception) { null }

        val merged = (remote ?: LOCAL_HISTORY).sortedByDescending { it.versionCode }
        cachedHistory = merged
        merged
    }

    /**
     * Rollback = install an older APK. Android forbids downgrade of a same-package signed app,
     * so this workflow triggers a normal download + the OS install prompt; the user will need to
     * uninstall the current build first. The overlay warns about this before invoking.
     */
    fun rollbackTo(info: UpdateInfo) {
        downloadUpdate(info)
    }

    /**
     * Uses system DownloadManager to download the APK.
     * This shows progress in the system status bar.
     */
    fun downloadUpdate(updateInfo: UpdateInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(updateInfo.apkUrl)
        
        val request = DownloadManager.Request(uri).apply {
            setTitle("Downloading Unsigned Update")
            setDescription("Version ${updateInfo.versionName}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "UNSIGNED_Update.apk")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        Toast.makeText(context, "Download started. Check status bar.", Toast.LENGTH_LONG).show()
        val downloadId = downloadManager.enqueue(request)

        // The installation will be handled by a BroadcastReceiver or we can poll for completion.
        // For simplicity in this one-page app, let's poll or use a receiver in the activity.
    }

    fun showUpdateNotification(info: UpdateInfo) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_updates"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Updates", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Update Available")
            .setContentText("Version ${info.versionName} is ready to install.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }

    fun installDownloadedApk(downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            if (statusIdx != -1 && localUriIdx != -1 && 
                DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(statusIdx)) {
                val uriString = cursor.getString(localUriIdx)
                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    val path = uri.path
                    if (path != null) {
                        installApk(File(path))
                    }
                }
            }
        }
        cursor.close()
    }

    fun installDownloadedApkLegacy() {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "UNSIGNED_Update.apk")
        if (file.exists()) {
            installApk(file)
        }
    }

    private fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
