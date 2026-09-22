package com.example.swara_editor.videoeditor.models

import kotlinx.serialization.Serializable

@Serializable
data class SpeedCurvePoint(
    val progress: Float, // Normalized clip position (0.0 to 1.0)
    val speed: Float     // Multiplier (e.g., 0.2x, 1.0x, 3.0x, 5.0x)
)

@Serializable
enum class SpeedCurvePreset(val displayName: String, val points: List<SpeedCurvePoint>) {
    CUSTOM("Custom", listOf(
        SpeedCurvePoint(0.0f, 1.0f),
        SpeedCurvePoint(1.0f, 1.0f)
    )),
    HERO("Hero (Fast-Slow-Fast)", listOf(
        SpeedCurvePoint(0.0f, 3.0f),
        SpeedCurvePoint(0.3f, 3.0f),
        SpeedCurvePoint(0.5f, 0.3f),
        SpeedCurvePoint(0.7f, 3.0f),
        SpeedCurvePoint(1.0f, 3.0f)
    )),
    BULLET("Bullet (Fast-Slow)", listOf(
        SpeedCurvePoint(0.0f, 4.0f),
        SpeedCurvePoint(0.4f, 4.0f),
        SpeedCurvePoint(0.6f, 0.4f),
        SpeedCurvePoint(1.0f, 0.4f)
    )),
    JUMP_CUT("Montage Jump", listOf(
        SpeedCurvePoint(0.0f, 0.5f),
        SpeedCurvePoint(0.4f, 3.5f),
        SpeedCurvePoint(0.6f, 0.5f),
        SpeedCurvePoint(1.0f, 3.5f)
    ))
}
