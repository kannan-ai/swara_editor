package com.example.swara_editor.videoeditor.services

import android.R
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.swara_editor.videoeditor.models.VideoProject
import com.example.swara_editor.videoeditor.utils.VideoExportEngine
import kotlinx.coroutines.*
import java.io.File

class VideoExportService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var exportJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val project = VideoProject(id = "dummy", name = "Exporting...")
        val outputPath = intent?.getStringExtra("outputPath")

        if (outputPath != null) {
            acquireWakeLock()
            startForegroundService(project)
            startExport(project, File(outputPath))
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SwaraEditor:ExportWakeLock").apply {
                acquire(30 * 60 * 1000L) // 30 minutes max timeout
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            wakeLock = null
        }
    }

    private fun startForegroundService(project: VideoProject) {
        val channelId = "video_export_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Video Export Engine",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rendering Video Composition")
            .setContentText("Exporting ${project.name}...")
            .setSmallIcon(R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun startExport(project: VideoProject, outputFile: File) {
        exportJob = serviceScope.launch {
            try {
                val success = VideoExportEngine.exportVideoProject(
                    context = this@VideoExportService,
                    project = project,
                    outputFile = outputFile,
                ) { progress ->
                    updateNotification(project, progress)
                }

                if (success) {
                    notifySuccess()
                } else {
                    notifyFailure()
                }
            } finally {
                releaseWakeLock()
                stopSelf()
            }
        }
    }

    private fun updateNotification(project: VideoProject, progress: Float) {
    }

    private fun notifySuccess() {
    }

    private fun notifyFailure() {
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        exportJob?.cancel()
        serviceScope.cancel()
    }
}
