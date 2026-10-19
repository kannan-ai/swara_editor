package com.example.swara_editor.videoeditor.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ApkSharer {

    suspend fun shareAppApk(context: Context) = withContext(Dispatchers.IO) {
        try {
            val sourceApk = File(context.applicationInfo.sourceDir)
            if (!sourceApk.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "APK file not found!", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            val sharedDir = File(context.cacheDir, "shared_apk")
            if (!sharedDir.exists()) {
                sharedDir.mkdirs()
            }

            val targetApk = File(sharedDir, "Swara_Editor_v0.0.1.apk")
            sourceApk.copyTo(targetApk, overwrite = true)

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "Swara Editor App APK")
                putExtra(Intent.EXTRA_TEXT, "Here is the Swara Editor app APK (v0.0.1)!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Swara Editor APK via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to share APK: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
