package com.example.swara_editor.videoeditor.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max

object AudioWaveformExtractor {

    private val waveformCache = ConcurrentHashMap<String, List<Float>>()

    suspend fun getOrExtractPeaks(
        context: Context,
        audioUri: Uri,
        targetBarCount: Int = 120
    ): List<Float> {
        val cacheKey = audioUri.toString()
        waveformCache[cacheKey]?.let { return it }

        val peaks = extractPeaks(context, audioUri, targetBarCount)
        if (peaks.isNotEmpty()) {
            waveformCache[cacheKey] = peaks
        }
        return peaks
    }

    suspend fun extractPeaks(
        context: Context,
        audioUri: Uri,
        targetBarCount: Int = 120
    ): List<Float> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, audioUri, null)
            val trackIndex = selectAudioTrack(extractor) ?: return@withContext emptyList()
            extractor.selectTrack(trackIndex)

            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList()
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val rawAmplitudes = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            while (!isEos) {
                val inIndex = codec.dequeueInputBuffer(5000L)
                if (inIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEos = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, 5000L)
                while (outIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outIndex)
                    if (outputBuffer != null) {
                        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        val shortBuffer = outputBuffer.asShortBuffer()
                        var maxAmp = 0
                        while (shortBuffer.hasRemaining()) {
                            val sample = abs(shortBuffer.get().toInt())
                            if (sample > maxAmp) maxAmp = sample
                        }
                        // Normalize 16-bit PCM amplitude (0..32767)
                        rawAmplitudes.add((maxAmp / 32767f).coerceIn(0f, 1f))
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            downsampleAmplitudes(rawAmplitudes, targetBarCount)
        } catch (e: Exception) {
            e.printStackTrace()
            try { extractor.release() } catch (_: Exception) {}
            emptyList()
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) return i
        }
        return null
    }

    private fun downsampleAmplitudes(raw: List<Float>, targetCount: Int): List<Float> {
        if (raw.isEmpty()) return emptyList()
        if (raw.size <= targetCount) return raw

        val chunkSize = raw.size / targetCount
        return (0 until targetCount).map { i ->
            val start = i * chunkSize
            val end = (start + chunkSize).coerceAtMost(raw.size)
            var peak = 0f
            for (idx in start until end) {
                peak = max(peak, raw[idx])
            }
            peak.coerceIn(0.08f, 1f) // Keep minor floor so silent regions show small bars
        }
    }
}
