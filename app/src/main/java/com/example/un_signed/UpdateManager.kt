package com.example.un_signed

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

class UpdateManager(private val context: Context) {

    private val versionUrl = "https://raw.githubusercontent.com/atriputr/unsigned/main/version.json"

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(versionUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val updateInfo = Gson().fromJson(json, UpdateInfo::class.java)
                
                val currentVersionCode = context.packageManager.getPackageInfo(context.packageName, 0).let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode.toInt() else it.versionCode
                }

                if (updateInfo.versionCode > currentVersionCode) {
                    return@withContext updateInfo
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun downloadAndInstall(updateInfo: UpdateInfo) = withContext(Dispatchers.IO) {
        try {
            val url = URL(updateInfo.apkUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val updateDir = File(context.externalCacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()
            val apkFile = File(updateDir, "update.apk")

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    input.copyTo(output)
                }
            }

            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showUpdateNotification(info: UpdateInfo) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_updates"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Updates", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        // We can't easily trigger the download from the intent without a receiver, 
        // so we'll just have the notification open the app which will show the overlay.
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

    private fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                // Note: The user has to manually come back and trigger the update again or we need to listen for permission change.
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
