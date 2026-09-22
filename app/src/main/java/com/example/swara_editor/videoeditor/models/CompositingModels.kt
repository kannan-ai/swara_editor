package com.example.swara_editor.videoeditor.models

import kotlinx.serialization.Serializable

@Serializable
enum class MaskType(val displayName: String) {
    NONE("None"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    LINEAR("Linear Split"),
    MIRROR("Mirror Split")
}

@Serializable
data class MaskSettings(
    val type: MaskType = MaskType.NONE,
    val centerX: Float = 0.5f,     // Normalized 0.0 to 1.0
    val centerY: Float = 0.5f,     // Normalized 0.0 to 1.0
    val width: Float = 0.6f,       // Normalized size
    val height: Float = 0.6f,
    val rotationDeg: Float = 0f,
    val feather: Float = 0.05f,    // Soft edge transition (0.0 to 0.3)
    val isInverted: Boolean = false
)

@Serializable
data class ChromaKeySettings(
    val isEnabled: Boolean = false,
    val keyColorHex: Long = 0xFF00FF00, // Standard pure green default
    val similarityThreshold: Float = 0.40f, // 0.1 to 0.8 tolerance
    val smoothness: Float = 0.10f,          // Feather edge spill
    val spillSuppression: Float = 0.50f     // Green tint reflection removal
)
