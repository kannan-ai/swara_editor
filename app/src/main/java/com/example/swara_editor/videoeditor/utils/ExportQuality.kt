package com.example.swara_editor.videoeditor.utils

import java.util.Locale

enum class ExportQuality(
    val label: String,
    val width: Int,
    val height: Int,
    val targetBitrateBps: Long
) {
    SD_480P("SD (480p)", 854, 480, 2_000_000L),
    HD_720P("HD (720p)", 1280, 720, 5_000_000L),
    FHD_1080P("Full HD (1080p)", 1920, 1080, 10_000_000L),
    QHD_2K("2K (1440p)", 2560, 1440, 18_000_000L),
    UHD_4K("4K Ultra HD", 3840, 2160, 35_000_000L);

    fun estimateSizeBytes(durationMs: Long): Long {
        val durationSec = durationMs / 1000f
        val audioBitrate = 192_000L
        val totalBitrate = targetBitrateBps + audioBitrate
        return ((totalBitrate * durationSec) / 8).toLong()
    }

    fun formattedEstimatedSize(durationMs: Long): String {
        val bytes = estimateSizeBytes(durationMs)
        val mb = bytes / (1024f * 1024f)
        return if (mb >= 1024) {
            String.format(Locale.US, "%.2f GB", mb / 1024f)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }
}
