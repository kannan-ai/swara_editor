package com.example.swara_editor.videoeditor.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.example.swara_editor.videoeditor.models.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoExportEngine {
    private const val TAG = "VideoExportEngine"

    @OptIn(UnstableApi::class)
    suspend fun exportVideoProject(
        context: Context,
        project: VideoProject,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val transformer = Transformer.Builder(context).build()
            
            // Simplified export logic using Media3 Transformer
            val mediaItems = project.layers.map { layer ->
                val asset = project.assets.find { it.id == layer.assetId }
                MediaItem.fromUri(asset?.uri ?: Uri.EMPTY)
            }

            if (mediaItems.isEmpty()) return@withContext false

            // For now, just export the first item as a placeholder for full composition
            transformer.start(mediaItems.first(), outputFile.absolutePath)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            false
        }
    }
}

object MediaProcessor {
    fun calculateDuration(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun generateThumbnail(context: Context, uri: Uri, timeUs: Long = 0): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (bmp != null) {
                Bitmap.createScaledBitmap(bmp, 120, 120, true)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun ConvertingVideoView(progress: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(progress = { progress })
            Spacer(modifier = Modifier.height(16.dp))
            Text("Exporting Video... ${(progress * 100).toInt()}%")
        }
    }
}

@Composable
fun ExportSuccessView(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = { Text("Export Complete") },
        text = { Text("Your video has been exported successfully to the gallery.") }
    )
}
