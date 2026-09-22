package com.example.swara_editor.videoeditor.utils

import android.content.Context
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@UnstableApi
class VideoExportManager(private val context: Context) {

    private val _exportProgress = MutableStateFlow(0)
    val exportProgress: StateFlow<Int> = _exportProgress.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun executeExportWithFallback(
        composition: Composition,
        outputPath: String,
        primaryMime: String = MimeTypes.VIDEO_H265,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onFatalError: (Exception) -> Unit = {}
    ) {
        _isExporting.value = true
        _exportProgress.value = 0

        val listener = object : Transformer.Listener {
            override fun onCompleted(comp: Composition, result: ExportResult) {
                _exportProgress.value = 100
                _isExporting.value = false
                onSuccess()
            }

            override fun onError(comp: Composition, result: ExportResult, exception: ExportException) {
                val isCodecFailure = exception.errorCode == ExportException.ERROR_CODE_ENCODER_INIT_FAILED ||
                        exception.errorCode == ExportException.ERROR_CODE_DECODER_INIT_FAILED

                if (isCodecFailure && primaryMime == MimeTypes.VIDEO_H265) {
                    // Fallback 1: Retry with standard H.264 / AVC
                    executeExportWithFallback(
                        composition = composition,
                        outputPath = outputPath,
                        primaryMime = MimeTypes.VIDEO_H264,
                        scope = scope,
                        onSuccess = onSuccess,
                        onFatalError = onFatalError
                    )
                } else {
                    _isExporting.value = false
                    onFatalError(exception)
                }
            }
        }

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(primaryMime)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        try {
            transformer.start(composition, outputPath)

            scope.launch(Dispatchers.Default) {
                val progressHolder = ProgressHolder()
                while (_isExporting.value) {
                    try {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            _exportProgress.value = progressHolder.progress
                        }
                    } catch (_: Exception) {
                        // Ignore progress query exceptions during export
                    }
                    delay(200)
                }
            }
        } catch (e: Exception) {
            _isExporting.value = false
            onFatalError(e)
        }
    }

    fun exportComposition(
        composition: Composition,
        quality: ExportQuality,
        outputPath: String,
        scope: CoroutineScope
    ) {
        executeExportWithFallback(
            composition = composition,
            outputPath = outputPath,
            primaryMime = MimeTypes.VIDEO_H265,
            scope = scope
        )
    }
}
