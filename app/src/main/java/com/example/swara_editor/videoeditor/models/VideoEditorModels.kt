package com.example.swara_editor.videoeditor.models

import android.net.Uri
import com.example.swara_editor.videoeditor.data.UriSerializer
import kotlinx.serialization.Serializable

@Serializable
data class VideoProject(
    val id: String,
    val name: String,
    val assets: List<ProjectAsset> = emptyList(),
    val layers: List<VideoLayer> = emptyList(),
    val textOverlays: List<TimedTextOverlay> = emptyList(),
    val autoCaptions: List<AutoCaptionTrack> = emptyList(),
    val transitions: List<ClipTransition> = emptyList(),
    val canvasSettings: CanvasSettings = CanvasSettings(),
    val durationMs: Long = 0L,
    val exportSettings: ExportSettings = ExportSettings(),
    @Serializable(with = UriSerializer::class)
    val coverUri: Uri? = null
)

@Serializable
data class ProjectAsset(
    val id: String,
    @Serializable(with = UriSerializer::class)
    val uri: Uri,
    val name: String,
    val type: AssetType,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    @Serializable(with = UriSerializer::class)
    val thumbnailUri: Uri? = null
)

@Suppress("unused")
@Serializable
enum class AssetType {
    VIDEO, AUDIO, IMAGE
}

@Serializable
data class VideoLayer(
    val id: String,
    val assetId: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val startOffsetMs: Long = 0L,
    val volume: Float = 1.0f,
    val speed: Float = 1.0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val positionX: Float = 0.5f, // Normalized 0-1
    val positionY: Float = 0.5f, // Normalized 0-1
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val colorGrading: ColorGradingSettings = ColorGradingSettings(),
    val keyframeTrack: KeyframeTrack? = null,
    val speedCurve: SpeedCurvePreset? = null,
    val customCurvePoints: List<SpeedCurvePoint> = emptyList(),
    val chromaKey: ChromaKeySettings = ChromaKeySettings(),
    val mask: MaskSettings = MaskSettings()
)

@Serializable
data class TimedTextOverlay(
    val id: String,
    val text: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val fontSize: Int = 28,
    val color: Int = 0xFFFFFFFF.toInt(),
    val stylePreset: TextWordArtStyle = TextWordArtStyle.CLASSIC_SHADOW,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val keyframeTrack: KeyframeTrack? = null
)

@Serializable
enum class TextWordArtStyle {
    CLASSIC_SHADOW, NEON_GLOW, BOLD_OUTLINE, SUNSET_GRADIENT
}

@Serializable
enum class CanvasAspectRatio(
    val label: String,
    val ratio: Float,
    val description: String
) {
    ORIGINAL("Original", 0f, "Fit Source"),
    PORTRAIT_9_16("9:16", 9f / 16f, "Stories / Shorts / Reels"),
    LANDSCAPE_16_9("16:9", 16f / 9f, "Standard Widescreen"),
    SQUARE_1_1("1:1", 1f, "Feed Post"),
    PORTRAIT_4_5("4:5", 4f / 5f, "Portrait Feed")
}

@Serializable
data class CanvasSettings(
    val targetRatio: CanvasAspectRatio = CanvasAspectRatio.PORTRAIT_9_16,
    val enableBackgroundBlur: Boolean = true,
    val blurRadius: Float = 25f,
    val showSafeZoneGuide: Boolean = false
)

@Serializable
data class ExportSettings(
    val resolution: Resolution = Resolution.R_1080P,
    val frameRate: Int = 30,
    val bitRate: Int = 5_000_000,
    val format: String = "mp4"
)

@Suppress("unused")
@Serializable
enum class Resolution(val width: Int, val height: Int) {
    R_720P(1280, 720),
    R_1080P(1920, 1080),
    R_4K(3840, 2160)
}

@Serializable
enum class FilterType {
    NORMAL, WARM, COOL, VINTAGE, CINEMATIC
}

@Serializable
enum class TransitionType(val displayName: String) {
    NONE("None"),
    CROSSFADE("Crossfade"),
    FADE_TO_BLACK("Fade Black"),
    SLIDE_LEFT("Slide Left"),
    WIPE_RIGHT("Wipe Right")
}

@Serializable
data class ClipTransition(
    val id: String,
    val fromLayerId: String,
    val toLayerId: String,
    val type: TransitionType = TransitionType.CROSSFADE,
    val durationMs: Long = 500L
)
