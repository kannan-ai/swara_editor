package com.example.swara_editor.videoeditor.models

import kotlinx.serialization.Serializable

@Serializable
data class SubtitleWord(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

@Serializable
data class CaptionStyle(
    val fontName: String = "SansSerif",
    val fontSizeSp: Float = 22f,
    val textColorHex: Long = 0xFFFFFFFF,
    val highlightColorHex: Long = 0xFFFFD700, // Gold karaoke highlight
    val outlineColorHex: Long = 0xFF000000,
    val outlineWidth: Float = 3f,
    val isKaraokeAnimated: Boolean = true
)

@Serializable
data class AutoCaptionTrack(
    val id: String,
    val fullText: String,
    val startMs: Long,
    val endMs: Long,
    val words: List<SubtitleWord> = emptyList(),
    val style: CaptionStyle = CaptionStyle()
)
