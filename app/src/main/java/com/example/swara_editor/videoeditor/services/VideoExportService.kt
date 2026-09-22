package com.example.swara_editor.videoeditor.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.swara_editor.videoeditor.models.VideoProject
import com.example.swara_editor.videoeditor.utils.VideoExportEngine
import kotlinx.coroutines.*
import java.io.File

class VideoExportService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var exportJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Placeholder: in a real app, use a proper way to pass the project (e.g. Database or ViewModel)
        val project = VideoProject(id = "dummy", name = "Exporting...")
        val outputPath = intent?.getStringExtra("outputPath")

        if (outputPath != null) {
            startForegroundService(project)
            startExport(project, File(outputPath))
        }

        return START_NOT_STICKY
    }

    private fun startForegroundService(project: VideoProject) {
        val channelId = "video_export_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Exporting Video")
            .setContentText("Project: ${project.name}")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()

        startForeground(1, notification)
    }

    private fun startExport(project: VideoProject, outputFile: File) {
        exportJob = serviceScope.launch {
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
            stopSelf()
        }
    }

    private fun updateNotification(project: VideoProject, progress: Float) {
        // Implementation for updating notification with progress bar
    }

    private fun notifySuccess() {
        // Notify user about success
    }

    private fun notifyFailure() {
        // Notify user about failure
    }

    override fun onDestroy() {
        super.onDestroy()
        exportJob?.cancel()
        serviceScope.cancel()
    }
}
