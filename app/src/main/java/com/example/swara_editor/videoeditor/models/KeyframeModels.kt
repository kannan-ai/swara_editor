package com.example.swara_editor.videoeditor.models

import kotlinx.serialization.Serializable

@Serializable
data class KeyframePoint(
    val timeMs: Long,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val alpha: Float = 1.0f
)

@Serializable
data class KeyframeTrack(
    val layerId: String,
    val keyframes: List<KeyframePoint> = emptyList()
)
