package com.example.swara_editor.videoeditor.models

import kotlinx.serialization.Serializable

@Serializable
data class ColorGradingSettings(
    val brightness: Float = 0.0f,     // -1.0f to 1.0f (0 = default)
    val contrast: Float = 1.0f,       // 0.2f to 2.0f (1 = default)
    val saturation: Float = 1.0f,     // 0.0f (grayscale) to 2.0f (1 = default)
    val temperature: Float = 0.0f,    // -1.0f (cool blue) to 1.0f (warm amber)
    val vignette: Float = 0.0f        // 0.0f (none) to 1.0f (heavy vignette)
)
