package com.example.swara_editor.videoeditor.ui

import android.graphics.Bitmap
import android.view.SurfaceView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.shape.CircleShape
import com.example.swara_editor.videoeditor.models.AssetType
import com.example.swara_editor.videoeditor.models.AutoCaptionTrack
import com.example.swara_editor.videoeditor.models.CanvasAspectRatio
import com.example.swara_editor.videoeditor.models.CanvasSettings
import com.example.swara_editor.videoeditor.models.CaptionStyle
import com.example.swara_editor.videoeditor.models.ChromaKeySettings
import com.example.swara_editor.videoeditor.models.ClipTransition
import com.example.swara_editor.videoeditor.models.ColorGradingSettings
import com.example.swara_editor.videoeditor.models.FilterType
import com.example.swara_editor.videoeditor.models.KeyframePoint
import com.example.swara_editor.videoeditor.models.KeyframeTrack
import com.example.swara_editor.videoeditor.models.MaskSettings
import com.example.swara_editor.videoeditor.models.MaskType
import com.example.swara_editor.videoeditor.models.SpeedCurvePoint
import com.example.swara_editor.videoeditor.models.SpeedCurvePreset
import com.example.swara_editor.videoeditor.models.SubtitleWord
import com.example.swara_editor.videoeditor.models.TextWordArtStyle
import com.example.swara_editor.videoeditor.models.TimedTextOverlay
import com.example.swara_editor.videoeditor.models.TransitionType
import com.example.swara_editor.videoeditor.models.VideoLayer
import com.example.swara_editor.videoeditor.models.VideoProject
import com.example.swara_editor.videoeditor.utils.AudioWaveformExtractor
import com.example.swara_editor.videoeditor.utils.AutoCaptionGenerator
import com.example.swara_editor.videoeditor.utils.CompositingGlEffect
import com.example.swara_editor.videoeditor.utils.KeyframeInterpolator
import com.example.swara_editor.videoeditor.utils.MediaProcessor
import com.example.swara_editor.videoeditor.utils.SpeedCurveEngine
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

data class FilterItem(val name: String, val type: FilterType, val tint: Color)

class TimelineScaleState(
    initialDpPerSecond: Float = 60f,
    val minDpPerSecond: Float = 20f,
    val maxDpPerSecond: Float = 300f
) {
    var dpPerSecond by mutableFloatStateOf(initialDpPerSecond)
        private set

    fun onZoomGesture(zoomRatio: Float) {
        dpPerSecond = (dpPerSecond * zoomRatio).coerceIn(minDpPerSecond, maxDpPerSecond)
    }

    fun msToDp(durationMs: Long): Dp {
        val seconds = durationMs / 1000f
        return (seconds * dpPerSecond).dp
    }

    fun pxToMs(offsetPx: Float, density: Float): Long {
        val dp = offsetPx / density
        val seconds = dp / dpPerSecond
        return (seconds * 1000f).toLong()
    }
}

object TimelineSnapEngine {
    private const val SNAP_THRESHOLD_MS = 150L

    fun getSnappedPosition(
        targetMs: Long,
        boundaryPoints: List<Long>,
        onSnapped: () -> Unit = {}
    ): Long {
        var closestPoint: Long? = null
        var minDelta = Long.MAX_VALUE

        for (point in boundaryPoints) {
            val delta = abs(targetMs - point)
            if (delta <= SNAP_THRESHOLD_MS && delta < minDelta) {
                minDelta = delta
                closestPoint = point
            }
        }

        return if (closestPoint != null) {
            onSnapped()
            closestPoint
        } else {
            targetMs
        }
    }
}

@UnstableApi
@Composable
fun VideoPreviewStage(
    exoPlayer: ExoPlayer,
    canvasSettings: CanvasSettings,
    currentTimeMs: Long,
    project: VideoProject,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val canvasModifier = if (canvasSettings.targetRatio.ratio > 0f) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(canvasSettings.targetRatio.ratio)
        } else {
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        }

        Box(
            modifier = canvasModifier
                .clipToBounds()
                .background(Color(0xFF0F0F0F)),
            contentAlignment = Alignment.Center
        ) {
            // 1. Blurred Mirror Layer (Behind source to fill letterboxes)
            if (canvasSettings.enableBackgroundBlur && canvasSettings.targetRatio != CanvasAspectRatio.ORIGINAL) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            (videoSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(false)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 24.dp)
                )
            }

            // 2. Foreground Video Surface (Main Sharp Video)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = if (canvasSettings.targetRatio == CanvasAspectRatio.ORIGINAL) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                        (videoSurfaceView as? SurfaceView)?.setZOrderMediaOverlay(true)
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = if (canvasSettings.targetRatio == CanvasAspectRatio.ORIGINAL) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 3. Social Media Safe-Zone Guide Overlay
            if (canvasSettings.showSafeZoneGuide && canvasSettings.targetRatio == CanvasAspectRatio.PORTRAIT_9_16) {
                SafeZoneGuidelineOverlay()
            }

            // 4. Timed WordArt Text Overlays Stage with Keyframes
            project.textOverlays
                .filter { overlay -> currentTimeMs in overlay.startTimeMs..(overlay.startTimeMs + overlay.durationMs) }
                .forEach { overlay ->
                    val keyframePoints = overlay.keyframeTrack?.keyframes ?: emptyList()
                    val transform = if (keyframePoints.isNotEmpty()) {
                        KeyframeInterpolator.interpolate(keyframePoints, currentTimeMs)
                    } else KeyframePoint(timeMs = currentTimeMs)

                    val textStyle = when (overlay.stylePreset) {
                        TextWordArtStyle.CLASSIC_SHADOW -> TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            shadow = Shadow(color = Color.Black, offset = Offset(4f, 4f), blurRadius = 4f)
                        )
                        TextWordArtStyle.NEON_GLOW -> TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFFF),
                            shadow = Shadow(color = Color(0xFFFF00FF), offset = Offset(0f, 0f), blurRadius = 16f)
                        )
                        TextWordArtStyle.BOLD_OUTLINE -> TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Yellow,
                            shadow = Shadow(color = Color.Black, offset = Offset(3f, 3f), blurRadius = 1f)
                        )
                        TextWordArtStyle.SUNSET_GRADIENT -> TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            shadow = Shadow(color = Color(0xFFE91E63), offset = Offset(4f, 4f), blurRadius = 8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .offset { IntOffset(transform.translationX.toInt(), transform.translationY.toInt()) }
                            .graphicsLayer {
                                scaleX = transform.scale
                                scaleY = transform.scale
                                rotationZ = transform.rotation
                                alpha = transform.alpha
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = overlay.text,
                            style = textStyle,
                            textAlign = TextAlign.Center
                        )
                    }
                }

            // 5. Active Auto Caption Karaoke Stage
            val activeCaption = project.autoCaptions.find { caption ->
                currentTimeMs in caption.startMs..caption.endMs
            }
            if (activeCaption != null) {
                KaraokeCaptionOverlay(
                    activeCaption = activeCaption,
                    currentPositionMs = currentTimeMs
                )
            }
        }
    }
}

@Composable
fun SafeZoneGuidelineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        // Top profile/header boundary (~12%)
        drawLine(Color.Yellow.copy(alpha = 0.6f), Offset(0f, size.height * 0.12f), Offset(size.width, size.height * 0.12f), strokeWidth = 2f, pathEffect = pathEffect)
        // Bottom interaction boundary (~80%)
        drawLine(Color.Yellow.copy(alpha = 0.6f), Offset(0f, size.height * 0.80f), Offset(size.width, size.height * 0.80f), strokeWidth = 2f, pathEffect = pathEffect)
        // Right interaction buttons boundary (~85%)
        drawLine(Color.Yellow.copy(alpha = 0.6f), Offset(size.width * 0.85f, 0f), Offset(size.width * 0.85f, size.height), strokeWidth = 2f, pathEffect = pathEffect)
    }
}

@Composable
fun TimelinePlayheadOverlayScaled(
    currentPositionMs: Long,
    scaleState: TimelineScaleState
) {
    val playheadOffsetDp = scaleState.msToDp(currentPositionMs)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .offset(x = playheadOffsetDp)
            .width(2.dp)
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 10.dp, height = 8.dp)
                .background(Color.White, shape = RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
        )
    }
}

@Composable
fun TimelineClipItem(
    layer: VideoLayer,
    project: VideoProject,
    isSelected: Boolean,
    onClipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFCC00) else Color(0xFF3E3E3E), // Gold/Yellow border when selected
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClipClick)
    ) {
        // Clip thumbnail image / content
        TimelineLayerContent(layer = layer, project = project, isSelected = isSelected)

        // Trim handles visible only on selection
        if (isSelected) {
            // Left Trim Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFFCC00))
            )
            // Right Trim Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFFCC00))
            )
        }
    }
}

@Composable
fun TimelineLayerContent(
    layer: VideoLayer,
    project: VideoProject,
    isSelected: Boolean
) {
    val context = LocalContext.current
    val asset = project.assets.find { it.id == layer.assetId }
    val thumbnailBitmap = produceState<Bitmap?>(initialValue = null, asset?.uri) {
        value = asset?.uri?.let { uri ->
            MediaProcessor.generateThumbnail(context, uri)
        }
    }.value

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (thumbnailBitmap != null) 0.35f else 0f)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                if (layer.volume == 0f) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = "Muted",
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (layer.speed != 1.0f) {
                    Text(
                        text = "${layer.speed}x",
                        color = Color.Yellow,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    text = asset?.name ?: "Clip ${layer.id.take(4)}",
                    color = if (thumbnailBitmap != null) Color.White else textColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TrackRowHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TextOverlayTrackRowScaled(
    overlays: List<TimedTextOverlay>,
    scaleState: TimelineScaleState,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        overlays.forEach { overlay ->
            val leftOffsetDp = scaleState.msToDp(overlay.startTimeMs)
            val itemWidthDp = scaleState.msToDp(overlay.durationMs).coerceAtLeast(40.dp)
            val isSelected = overlay.id == selectedId

            Box(
                modifier = Modifier
                    .offset(x = leftOffsetDp)
                    .width(itemWidthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF673AB7) else Color(0xFF4A148C))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFFFFCC00) else Color(0xFF9C27B0),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(overlay.id) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Text",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = overlay.text,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFFCC00))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFFCC00))
                    )
                }
            }
        }
    }
}

@Composable
fun AudioWaveformCanvas(
    peaks: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF00BFA5),
    playedBarColor: Color = Color.White,
    playbackProgress: Float = 0f
) {
    Canvas(modifier = modifier.fillMaxSize().padding(vertical = 4.dp)) {
        if (peaks.isEmpty()) return@Canvas

        val totalBars = peaks.size
        val barWidthPx = (size.width / totalBars) * 0.65f
        val spacePx = (size.width / totalBars) * 0.35f
        val centerY = size.height / 2f

        peaks.forEachIndexed { index, peak ->
            val x = index * (barWidthPx + spacePx) + (barWidthPx / 2f)
            val barHeight = (size.height * peak * 0.85f).coerceAtLeast(4f)
            val isPlayed = (x / size.width) <= playbackProgress

            drawLine(
                color = if (isPlayed) playedBarColor else barColor,
                start = Offset(x, centerY - (barHeight / 2f)),
                end = Offset(x, centerY + (barHeight / 2f)),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AudioTrackRowScaled(
    audioLayers: List<VideoLayer>,
    project: VideoProject,
    scaleState: TimelineScaleState,
    selectedId: String?,
    currentTimeMs: Long = 0L,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        audioLayers.forEach { layer ->
            val asset = project.assets.find { it.id == layer.assetId }
            val leftOffsetDp = scaleState.msToDp(layer.startTimeMs)
            val itemWidthDp = scaleState.msToDp(layer.durationMs).coerceAtLeast(50.dp)
            val isSelected = layer.id == selectedId

            val peaks = produceState<List<Float>>(initialValue = emptyList(), asset?.uri) {
                value = asset?.uri?.let { uri ->
                    AudioWaveformExtractor.getOrExtractPeaks(context, uri, targetBarCount = 80)
                } ?: emptyList()
            }.value

            val trackProgress = if (layer.durationMs > 0L) {
                ((currentTimeMs - layer.startTimeMs).toFloat() / layer.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                modifier = Modifier
                    .offset(x = leftOffsetDp)
                    .width(itemWidthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFF00796B) else Color(0xFF004D40))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(0xFFFFCC00) else Color(0xFF009688),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(layer.id) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (peaks.isNotEmpty()) {
                    AudioWaveformCanvas(
                        peaks = peaks,
                        playbackProgress = trackProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio",
                        tint = Color.Cyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = asset?.name ?: "Audio Track",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFFCC00))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFFFCC00))
                    )
                }
            }
        }
    }
}

@Composable
fun KeyframeTimelineDiamonds(
    keyframes: List<KeyframePoint>,
    scaleState: TimelineScaleState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        keyframes.forEach { kf ->
            Box(
                modifier = Modifier
                    .offset(x = scaleState.msToDp(kf.timeMs) - 5.dp, y = 2.dp)
                    .size(10.dp)
                    .graphicsLayer { rotationZ = 45f }
                    .background(Color(0xFFFFD700), RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun KeyframeActionButton(
    hasKeyframeAtPlayhead: Boolean,
    onAddKeyframe: () -> Unit,
    onRemoveKeyframe: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            if (hasKeyframeAtPlayhead) onRemoveKeyframe() else onAddKeyframe()
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (hasKeyframeAtPlayhead) Icons.Default.RemoveCircle else Icons.Default.AddCircle,
            contentDescription = "Keyframe Toggle",
            tint = if (hasKeyframeAtPlayhead) Color.Red else Color(0xFFFFD700)
        )
    }
}

@Composable
fun TransitionNodeItem(
    transition: ClipTransition?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (transition != null && transition.type != TransitionType.NONE)
                    MaterialTheme.colorScheme.primary
                else Color(0xFF2C2C2C)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Transform,
            contentDescription = "Transition",
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun ScrollableTimelineStudio(
    project: VideoProject,
    currentPositionMs: Long,
    selectedLayerId: String?,
    scaleState: TimelineScaleState,
    onSeekTo: (Long) -> Unit,
    onClipSelect: (String) -> Unit,
    onTransitionClick: (fromLayerId: String, toLayerId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (project.layers.isEmpty() && project.textOverlays.isEmpty()) return

    val scrollState = rememberScrollState()
    val density = LocalDensity.current.density
    val haptic = LocalHapticFeedback.current

    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        scaleState.onZoomGesture(zoomChange)
    }

    // Precalculate snap points (start and end timestamps of all segments)
    val snapPoints = remember(project.layers, project.textOverlays, project.durationMs) {
        val points = mutableSetOf<Long>()
        points.add(0L)
        points.add(project.durationMs)
        project.layers.forEach { layer ->
            points.add(layer.startTimeMs)
            points.add(layer.startTimeMs + layer.durationMs)
        }
        project.textOverlays.forEach { overlay ->
            points.add(overlay.startTimeMs)
            points.add(overlay.startTimeMs + overlay.durationMs)
        }
        points.sorted()
    }

    val videoLayers = project.layers.filter { layer ->
        val asset = project.assets.find { it.id == layer.assetId }
        asset?.type != AssetType.AUDIO
    }
    val audioLayers = project.layers.filter { layer ->
        val asset = project.assets.find { it.id == layer.assetId }
        asset?.type == AssetType.AUDIO
    }
    val textOverlays = project.textOverlays
    val totalDuration = project.durationMs.coerceAtLeast(1000L)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color(0xFF141414)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .transformable(state = transformableState)
                .padding(vertical = 8.dp)
        ) {
            // Horizontally Scrollable Tracks Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(scaleState.msToDp(totalDuration))
                        .wrapContentHeight()
                        .pointerInput(totalDuration) {
                            detectTapGestures { offset ->
                                val rawMs = scaleState.pxToMs(offset.x, density)
                                val snappedMs = TimelineSnapEngine.getSnappedPosition(
                                    targetMs = rawMs,
                                    boundaryPoints = snapPoints,
                                    onSnapped = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                                onSeekTo(snappedMs.coerceIn(0L, totalDuration))
                            }
                        }
                ) {
                    // Column containing Video, Text, and Audio Tracks
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Track 1: Video/Image Clips
                        TrackRowHeader(icon = Icons.Default.Videocam, title = "Video Track")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (videoLayers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No video media", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                } else {
                                    videoLayers.forEachIndexed { index, layer ->
                                        TimelineClipItem(
                                            layer = layer,
                                            project = project,
                                            isSelected = layer.id == selectedLayerId,
                                            onClipClick = { onClipSelect(layer.id) },
                                            modifier = Modifier
                                                .width(scaleState.msToDp(layer.durationMs))
                                                .fillMaxHeight()
                                        )
                                        if (index < videoLayers.size - 1) {
                                            val nextLayer = videoLayers[index + 1]
                                            val transition = project.transitions.find {
                                                it.fromLayerId == layer.id && it.toLayerId == nextLayer.id
                                            }
                                            TransitionNodeItem(
                                                transition = transition,
                                                onClick = { onTransitionClick(layer.id, nextLayer.id) },
                                                modifier = Modifier
                                                    .align(Alignment.CenterVertically)
                                                    .padding(horizontal = 1.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Track 2: Text Overlays
                        if (textOverlays.isNotEmpty()) {
                            TrackRowHeader(icon = Icons.Default.TextFields, title = "Text Track")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                TextOverlayTrackRowScaled(
                                    overlays = textOverlays,
                                    scaleState = scaleState,
                                    selectedId = selectedLayerId,
                                    onSelect = onClipSelect
                                )
                            }
                        }

                        // Track 3: Audio Layers
                        if (audioLayers.isNotEmpty()) {
                            TrackRowHeader(icon = Icons.Default.MusicNote, title = "Audio Track")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                AudioTrackRowScaled(
                                    audioLayers = audioLayers,
                                    project = project,
                                    scaleState = scaleState,
                                    selectedId = selectedLayerId,
                                    currentTimeMs = currentPositionMs,
                                    onSelect = onClipSelect
                                )
                            }
                        }
                    }

                    // Sweeping Playhead Needle across all tracks
                    TimelinePlayheadOverlayScaled(
                        currentPositionMs = currentPositionMs,
                        scaleState = scaleState
                    )
                }
            }
        }
    }
}

@Composable
fun MultiTrackTimeline(
    project: VideoProject,
    currentTimeMs: Long,
    selectedLayerId: String?,
    onLayerSelected: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onTransitionClick: (String, String) -> Unit = { _, _ -> }
) {
    val scaleState = remember { TimelineScaleState() }
    ScrollableTimelineStudio(
        project = project,
        currentPositionMs = currentTimeMs,
        selectedLayerId = selectedLayerId,
        scaleState = scaleState,
        onSeekTo = onSeek,
        onClipSelect = onLayerSelected,
        onTransitionClick = onTransitionClick
    )
}

@Composable
fun EditControlPanel(
    activeAction: EditorAction?,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorActionButton(
                Icons.Default.Edit,
                "Edit",
                activeAction == EditorAction.EDIT
            ) { onAction(EditorAction.EDIT) }
            
            EditorActionButton(
                Icons.Default.Tune,
                "Adjust",
                activeAction == EditorAction.ADJUST
            ) { onAction(EditorAction.ADJUST) }
            
            EditorActionButton(
                Icons.Default.Filter,
                "Filters",
                activeAction == EditorAction.FILTER
            ) { onAction(EditorAction.FILTER) }

            EditorActionButton(
                Icons.Default.AspectRatio,
                "Canvas",
                activeAction == EditorAction.CANVAS
            ) { onAction(EditorAction.CANVAS) }
            
            EditorActionButton(
                Icons.Default.TextFields,
                "Text",
                activeAction == EditorAction.TEXT
            ) { onAction(EditorAction.TEXT) }

            EditorActionButton(
                Icons.Default.MusicNote,
                "Audio",
                activeAction == EditorAction.AUDIO
            ) { onAction(EditorAction.AUDIO) }

            EditorActionButton(
                Icons.Default.Layers,
                "PIP",
                activeAction == EditorAction.PIP
            ) { onAction(EditorAction.PIP) }
        }
    }
}

@Composable
fun EditorActionButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun WordArtStylePicker(
    selectedStyle: TextWordArtStyle,
    onStyleSelected: (TextWordArtStyle) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(TextWordArtStyle.entries) { style ->
            val isSelected = style == selectedStyle
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray
                ),
                modifier = Modifier
                    .width(90.dp)
                    .height(64.dp)
                    .clickable { onStyleSelected(style) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val sampleStyle = when (style) {
                        TextWordArtStyle.CLASSIC_SHADOW -> TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            shadow = Shadow(color = Color.Black, offset = Offset(3f, 3f), blurRadius = 4f)
                        )
                        TextWordArtStyle.NEON_GLOW -> TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFFF),
                            shadow = Shadow(color = Color(0xFFFF00FF), offset = Offset(0f, 0f), blurRadius = 10f)
                        )
                        TextWordArtStyle.BOLD_OUTLINE -> TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Yellow,
                            shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 1f)
                        )
                        TextWordArtStyle.SUNSET_GRADIENT -> TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722),
                            shadow = Shadow(color = Color(0xFFE91E63), offset = Offset(3f, 3f), blurRadius = 6f)
                        )
                    }
                    Text(text = "Aa", style = sampleStyle)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = style.name.replace("_", " "),
                        fontSize = 9.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustmentSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = String.format(Locale.US, "%.2f", value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AdjustPropertyPanel(
    settings: ColorGradingSettings,
    onSettingsChanged: (ColorGradingSettings) -> Unit
) {
    var activeParam by remember { mutableStateOf("Brightness") }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Brightness", "Contrast", "Saturation", "Warmth", "Vignette").forEach { param ->
                FilterChip(
                    selected = activeParam == param,
                    onClick = { activeParam = param },
                    label = { Text(param, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (activeParam) {
            "Brightness" -> AdjustmentSlider(
                value = settings.brightness,
                range = -0.5f..0.5f,
                label = "Brightness (-0.5 to +0.5)",
                onValueChange = { onSettingsChanged(settings.copy(brightness = it)) }
            )
            "Contrast" -> AdjustmentSlider(
                value = settings.contrast,
                range = 0.5f..1.5f,
                label = "Contrast (0.5 to 1.5)",
                onValueChange = { onSettingsChanged(settings.copy(contrast = it)) }
            )
            "Saturation" -> AdjustmentSlider(
                value = settings.saturation,
                range = 0.0f..2.0f,
                label = "Saturation (0.0 Grayscale to 2.0)",
                onValueChange = { onSettingsChanged(settings.copy(saturation = it)) }
            )
            "Warmth" -> AdjustmentSlider(
                value = settings.temperature,
                range = -1.0f..1.0f,
                label = "Warmth (-1.0 Cool to +1.0 Amber)",
                onValueChange = { onSettingsChanged(settings.copy(temperature = it)) }
            )
            "Vignette" -> AdjustmentSlider(
                value = settings.vignette,
                range = 0.0f..1.0f,
                label = "Vignette (0.0 to 1.0)",
                onValueChange = { onSettingsChanged(settings.copy(vignette = it)) }
            )
        }
    }
}

@Composable
fun SpeedCurveEditorCanvas(
    points: List<SpeedCurvePoint>,
    onPointMoved: (index: Int, newProgress: Float, newSpeed: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedPointIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color(0xFF1B1B1B), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width
                            val height = size.height
                            draggedPointIndex = points.indexOfFirst { pt ->
                                val x = pt.progress * width
                                val y = height - ((pt.speed / 5.0f).coerceIn(0f, 1f) * height)
                                hypot(offset.x - x, offset.y - y) < 40f
                            }.takeIf { it != -1 }
                        },
                        onDrag = { change, _ ->
                            draggedPointIndex?.let { idx ->
                                val width = size.width
                                val height = size.height
                                val newProgress = (change.position.x / width).coerceIn(0f, 1f)
                                val newSpeed = ((1.0f - (change.position.y / height)) * 5.0f).coerceIn(0.2f, 5.0f)
                                onPointMoved(idx, newProgress, newSpeed)
                            }
                        },
                        onDragEnd = { draggedPointIndex = null }
                    )
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw 1.0x Normal Speed Reference Baseline
            val normalSpeedY = h - ((1.0f / 5.0f) * h)
            drawLine(
                color = Color.DarkGray,
                start = Offset(0f, normalSpeedY),
                end = Offset(w, normalSpeedY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // 2. Draw Speed Curve Path
            val curvePath = Path()
            points.forEachIndexed { index, pt ->
                val x = pt.progress * w
                val y = h - ((pt.speed / 5.0f).coerceIn(0f, 1f) * h)
                if (index == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
            }
            drawPath(
                path = curvePath,
                color = Color(0xFF00E676), // Neon Green
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Draw Anchor Handles
            points.forEachIndexed { index, pt ->
                val x = pt.progress * w
                val y = h - ((pt.speed / 5.0f).coerceIn(0f, 1f) * h)
                val isSelected = index == draggedPointIndex

                drawCircle(
                    color = if (isSelected) Color.White else Color(0xFF00E676),
                    radius = if (isSelected) 8.dp.toPx() else 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}

@Composable
fun SpeedControlSubPanel(
    selectedLayer: VideoLayer,
    onUpdateSpeed: (String, Float) -> Unit,
    onUpdateSpeedCurve: (String, SpeedCurvePreset?, List<SpeedCurvePoint>) -> Unit
) {
    var mode by remember(selectedLayer.id) { mutableStateOf(if (selectedLayer.speedCurve != null) "Curve" else "Constant") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = mode == "Constant",
                onClick = {
                    mode = "Constant"
                    onUpdateSpeedCurve(selectedLayer.id, null, emptyList())
                },
                label = { Text("Constant Speed", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = mode == "Curve",
                onClick = { mode = "Curve" },
                label = { Text("Speed Curve (Ramp)", style = MaterialTheme.typography.labelSmall) }
            )
        }

        if (mode == "Constant") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Speed:", style = MaterialTheme.typography.labelMedium)
                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                    FilterChip(
                        selected = selectedLayer.speed == spd && selectedLayer.speedCurve == null,
                        onClick = { onUpdateSpeed(selectedLayer.id, spd) },
                        label = { Text("${spd}x", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        } else {
            val activePoints = if (selectedLayer.customCurvePoints.isNotEmpty()) {
                selectedLayer.customCurvePoints
            } else {
                selectedLayer.speedCurve?.points ?: SpeedCurvePreset.HERO.points
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(SpeedCurvePreset.entries) { preset ->
                    val isSelected = selectedLayer.speedCurve == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdateSpeedCurve(selectedLayer.id, preset, preset.points) },
                        label = { Text(preset.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            SpeedCurveEditorCanvas(
                points = activePoints,
                onPointMoved = { idx, newProg, newSpd ->
                    val mutablePoints = activePoints.toMutableList()
                    if (idx in mutablePoints.indices) {
                        mutablePoints[idx] = mutablePoints[idx].copy(progress = newProg, speed = newSpd)
                        onUpdateSpeedCurve(selectedLayer.id, SpeedCurvePreset.CUSTOM, mutablePoints)
                    }
                }
            )
        }
    }
}

@Composable
fun KaraokeCaptionOverlay(
    activeCaption: AutoCaptionTrack?,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    if (activeCaption == null) return

    val style = activeCaption.style
    val words = activeCaption.words

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            words.forEach { wordItem ->
                val isCurrentWord = style.isKaraokeAnimated &&
                        currentPositionMs in wordItem.startMs..wordItem.endMs
                val isPastWord = style.isKaraokeAnimated && currentPositionMs > wordItem.endMs

                val targetColor = when {
                    isCurrentWord -> Color(style.highlightColorHex)
                    isPastWord -> Color.White
                    else -> Color.White.copy(alpha = 0.5f)
                }

                val scale by animateFloatAsState(
                    targetValue = if (isCurrentWord) 1.18f else 1.0f,
                    label = "KaraokeScale"
                )

                Text(
                    text = "${wordItem.word} ",
                    fontSize = style.fontSizeSp.sp,
                    fontWeight = if (isCurrentWord) FontWeight.Black else FontWeight.Bold,
                    color = targetColor,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
        }
    }
}

@Composable
fun AutoCaptionControlPanel(
    onGenerateCaptions: () -> Unit,
    isGenerating: Boolean,
    onStyleChanged: (CaptionStyle) -> Unit,
    currentStyle: CaptionStyle
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto Subtitles (Speech-to-Text)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

            Button(
                onClick = onGenerateCaptions,
                enabled = !isGenerating,
                modifier = Modifier.height(36.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Captions", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("Karaoke Highlight Style", style = MaterialTheme.typography.labelSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = currentStyle.isKaraokeAnimated,
                onClick = { onStyleChanged(currentStyle.copy(isKaraokeAnimated = !currentStyle.isKaraokeAnimated)) },
                label = { Text("Karaoke Bounce", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = currentStyle.highlightColorHex == 0xFFFFD700,
                onClick = { onStyleChanged(currentStyle.copy(highlightColorHex = 0xFFFFD700)) },
                label = { Text("Gold", style = MaterialTheme.typography.labelSmall) }
            )
            FilterChip(
                selected = currentStyle.highlightColorHex == 0xFF00E676,
                onClick = { onStyleChanged(currentStyle.copy(highlightColorHex = 0xFF00E676)) },
                label = { Text("Neon Green", style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@Composable
fun PropertyPanel(
    action: EditorAction,
    project: VideoProject,
    selectedLayerId: String?,
    selectedFilter: FilterType,
    activeTransitionPair: Pair<String, String>? = null,
    onFilterSelected: (FilterType) -> Unit,
    onAddText: (String, TextWordArtStyle) -> Unit,
    onUpdateTextOverlay: (id: String, text: String, style: TextWordArtStyle) -> Unit = { _, _, _ -> },
    onAddAudio: () -> Unit,
    onToggleMute: (String) -> Unit,
    onUpdateSpeed: (String, Float) -> Unit,
    onUpdateSpeedCurve: (String, SpeedCurvePreset?, List<SpeedCurvePoint>) -> Unit = { _, _, _ -> },
    onUpdateBrightness: (Float) -> Unit,
    onUpdateCanvasSettings: (CanvasSettings) -> Unit = {},
    onUpdateColorGrading: (ColorGradingSettings) -> Unit = {},
    onUpdateChromaKey: (String, ChromaKeySettings) -> Unit = { _, _ -> },
    onUpdateMaskSettings: (String, MaskSettings) -> Unit = { _, _ -> },
    onUpdateTransition: (fromId: String, toId: String, type: TransitionType, durationMs: Long) -> Unit = { _, _, _, _ -> },
    onExtractAudio: (String) -> Unit = {},
    onUpdateAudioFade: (String, Long, Long) -> Unit = { _, _, _ -> },
    onGenerateCaptions: () -> Unit = {},
    isGeneratingCaptions: Boolean = false,
    onUpdateCaptionStyle: (CaptionStyle) -> Unit = {},
    onSplitClip: () -> Unit,
    onDeleteClip: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val primaryAsset = project.assets.firstOrNull()
    val thumbnailBitmap = produceState<Bitmap?>(initialValue = null, primaryAsset?.uri) {
        value = primaryAsset?.uri?.let { uri ->
            MediaProcessor.generateThumbnail(context, uri)
        }
    }.value

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            when (action) {
                EditorAction.EDIT -> {
                    val selectedTextOverlay = project.textOverlays.find { it.id == selectedLayerId }
                    val selectedLayer = project.layers.find { it.id == selectedLayerId } ?: project.layers.firstOrNull()

                    if (selectedTextOverlay != null) {
                        var textInput by remember(selectedTextOverlay.id) { mutableStateOf(selectedTextOverlay.text) }
                        var selectedStyle by remember(selectedTextOverlay.id) { mutableStateOf(selectedTextOverlay.stylePreset) }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = {
                                        textInput = it
                                        if (it.isNotBlank()) {
                                            onUpdateTextOverlay(selectedTextOverlay.id, it, selectedStyle)
                                        }
                                    },
                                    label = { Text("Editing Selected Text Clip") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = false,
                                    onClick = { onDeleteClip(selectedTextOverlay.id) },
                                    label = { Text("Delete", color = Color.Red) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                )
                            }
                            WordArtStylePicker(
                                selectedStyle = selectedStyle,
                                onStyleSelected = { newStyle ->
                                    selectedStyle = newStyle
                                    if (textInput.isNotBlank()) {
                                        onUpdateTextOverlay(selectedTextOverlay.id, textInput, newStyle)
                                    }
                                }
                            )
                        }
                    } else {
                        val isMuted = selectedLayer?.volume == 0f
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(selected = false, onClick = {}, label = { Text("Trim") })
                                FilterChip(
                                    selected = false,
                                    onClick = { onSplitClip() },
                                    label = { Text("Split") }
                                )
                                FilterChip(
                                    selected = isMuted,
                                    onClick = {
                                        if (selectedLayer != null) {
                                            onToggleMute(selectedLayer.id)
                                        }
                                    },
                                    label = { Text(if (isMuted) "Unmute" else "Mute") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (selectedLayer != null) {
                                            onExtractAudio(selectedLayer.id)
                                        }
                                    },
                                    label = { Text("Extract Audio") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        if (selectedLayer != null) {
                                            onDeleteClip(selectedLayer.id)
                                        }
                                    },
                                    label = { Text("Delete", color = Color.Red) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                )
                            }

                            if (selectedLayer != null) {
                                SpeedControlSubPanel(
                                    selectedLayer = selectedLayer,
                                    onUpdateSpeed = onUpdateSpeed,
                                    onUpdateSpeedCurve = onUpdateSpeedCurve
                                )
                            }
                        }
                    }
                }
                EditorAction.ADJUST -> {
                    val selectedLayer = project.layers.find { it.id == selectedLayerId } ?: project.layers.firstOrNull()
                    val currentGrading = selectedLayer?.colorGrading ?: ColorGradingSettings()
                    AdjustPropertyPanel(
                        settings = currentGrading,
                        onSettingsChanged = { newGrading ->
                            onUpdateColorGrading(newGrading)
                        }
                    )
                }
                EditorAction.FILTER -> {
                    val filters = listOf(
                        FilterItem("Normal", FilterType.NORMAL, Color.Transparent),
                        FilterItem("Warm", FilterType.WARM, Color(0x33FF9800)),
                        FilterItem("Cool", FilterType.COOL, Color(0x3303A9F4)),
                        FilterItem("Vintage", FilterType.VINTAGE, Color(0x33795548)),
                        FilterItem("Cinematic", FilterType.CINEMATIC, Color(0x33673AB7))
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(filters) { item ->
                            val isSelected = selectedFilter == item.type
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onFilterSelected(item.type) }
                                    .width(64.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(Color.DarkGray)
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbnailBitmap != null) {
                                        Image(
                                            bitmap = thumbnailBitmap.asImageBitmap(),
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(item.name.take(2), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (item.tint != Color.Transparent) {
                                        Box(modifier = Modifier.fillMaxSize().background(item.tint))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                EditorAction.CANVAS -> {
                    val settings = project.canvasSettings
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Canvas Aspect Ratio & Framing", style = MaterialTheme.typography.bodySmall)

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(CanvasAspectRatio.entries) { ratio ->
                                val isSelected = settings.targetRatio == ratio
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUpdateCanvasSettings(settings.copy(targetRatio = ratio)) },
                                    label = { Text("${ratio.label} (${ratio.description})", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = settings.enableBackgroundBlur,
                                onClick = { onUpdateCanvasSettings(settings.copy(enableBackgroundBlur = !settings.enableBackgroundBlur)) },
                                label = { Text("Blur Background", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.BlurOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            )

                            if (settings.targetRatio == CanvasAspectRatio.PORTRAIT_9_16) {
                                FilterChip(
                                    selected = settings.showSafeZoneGuide,
                                    onClick = { onUpdateCanvasSettings(settings.copy(showSafeZoneGuide = !settings.showSafeZoneGuide)) },
                                    label = { Text("Safe Zone Guide", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.GridOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                EditorAction.TEXT -> {
                    val selectedOverlay = project.textOverlays.find { it.id == selectedLayerId }
                    var textInput by remember(selectedOverlay?.id) { mutableStateOf(selectedOverlay?.text ?: "") }
                    var selectedStyle by remember(selectedOverlay?.id) { mutableStateOf(selectedOverlay?.stylePreset ?: TextWordArtStyle.CLASSIC_SHADOW) }
                    var textSubTab by remember { mutableIntStateOf(0) } // 0: Add Text, 1: Auto Captions
                    val currentCaptionStyle = project.autoCaptions.firstOrNull()?.style ?: CaptionStyle()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedOverlay != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Editing Selected Text Clip", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { onDeleteClip(selectedOverlay.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Text", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            OutlinedTextField(
                                value = textInput,
                                onValueChange = {
                                    textInput = it
                                    if (it.isNotBlank()) {
                                        onUpdateTextOverlay(selectedOverlay.id, it, selectedStyle)
                                    }
                                },
                                label = { Text("Edit Text Content") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                singleLine = true
                            )

                            WordArtStylePicker(
                                selectedStyle = selectedStyle,
                                onStyleSelected = { newStyle ->
                                    selectedStyle = newStyle
                                    if (textInput.isNotBlank()) {
                                        onUpdateTextOverlay(selectedOverlay.id, textInput, newStyle)
                                    }
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = textSubTab == 0,
                                    onClick = { textSubTab = 0 },
                                    label = { Text("Add WordArt Text", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                    }
                                )
                                FilterChip(
                                    selected = textSubTab == 1,
                                    onClick = { textSubTab = 1 },
                                    label = { Text("Auto Subtitles", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                    }
                                )
                            }

                            if (textSubTab == 0) {
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = { Text("Enter WordArt text...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    singleLine = true
                                )
                                WordArtStylePicker(
                                    selectedStyle = selectedStyle,
                                    onStyleSelected = { selectedStyle = it }
                                )
                                Button(
                                    onClick = {
                                        if (textInput.isNotBlank()) {
                                            onAddText(textInput, selectedStyle)
                                            textInput = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Text Overlay", style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                AutoCaptionControlPanel(
                                    onGenerateCaptions = onGenerateCaptions,
                                    isGenerating = isGeneratingCaptions,
                                    onStyleChanged = onUpdateCaptionStyle,
                                    currentStyle = currentCaptionStyle
                                )
                            }
                        }
                    }
                }
                EditorAction.AUDIO -> {
                    val selectedAudio = project.layers.find { it.id == selectedLayerId && project.assets.find { a -> a.id == it.assetId }?.type == AssetType.AUDIO }
                    val audioAsset = selectedAudio?.let { layer -> project.assets.find { it.id == layer.assetId } }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedAudio != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Editing: ${audioAsset?.name ?: "Audio Track"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { onDeleteClip(selectedAudio.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Audio", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isMuted = selectedAudio.volume == 0f
                                FilterChip(
                                    selected = isMuted,
                                    onClick = { onToggleMute(selectedAudio.id) },
                                    label = { Text(if (isMuted) "Unmute" else "Mute") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                )
                                Text("Speed:", style = MaterialTheme.typography.labelMedium)
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                    FilterChip(
                                        selected = selectedAudio.speed == spd,
                                        onClick = { onUpdateSpeed(selectedAudio.id, spd) },
                                        label = { Text("${spd}x", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            var fadeInSec by remember(selectedAudio.id) { mutableFloatStateOf(selectedAudio.fadeInMs / 1000f) }
                            var fadeOutSec by remember(selectedAudio.id) { mutableFloatStateOf(selectedAudio.fadeOutMs / 1000f) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fade In: ${String.format(Locale.US, "%.1fs", fadeInSec)}", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = fadeInSec,
                                    onValueChange = {
                                        fadeInSec = it
                                        onUpdateAudioFade(selectedAudio.id, (it * 1000).toLong(), selectedAudio.fadeOutMs)
                                    },
                                    valueRange = 0f..3f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fade Out: ${String.format(Locale.US, "%.1fs", fadeOutSec)}", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = fadeOutSec,
                                    onValueChange = {
                                        fadeOutSec = it
                                        onUpdateAudioFade(selectedAudio.id, selectedAudio.fadeInMs, (it * 1000).toLong())
                                    },
                                    valueRange = 0f..3f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        Button(
                            onClick = onAddAudio,
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Music / Audio File (*.mp3, *.wav)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                EditorAction.PIP -> {
                    val selectedLayer = project.layers.find { it.id == selectedLayerId } ?: project.layers.firstOrNull()
                    if (selectedLayer != null) {
                        ChromaAndMaskControlPanel(
                            chromaSettings = selectedLayer.chromaKey,
                            maskSettings = selectedLayer.mask,
                            onChromaChanged = { newChroma -> onUpdateChromaKey(selectedLayer.id, newChroma) },
                            onMaskChanged = { newMask -> onUpdateMaskSettings(selectedLayer.id, newMask) }
                        )
                    } else {
                        Text("Select an overlay layer to adjust Chroma Key & Masking", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                EditorAction.TRANSITION -> {
                    val pair = activeTransitionPair
                    val currentTransition = if (pair != null) {
                        project.transitions.find { it.fromLayerId == pair.first && it.toLayerId == pair.second }
                    } else project.transitions.firstOrNull()

                    TransitionPickerPanel(
                        transition = currentTransition,
                        onSelectTransition = { type, durMs ->
                            if (pair != null) {
                                onUpdateTransition(pair.first, pair.second, type, durMs)
                            } else if (currentTransition != null) {
                                onUpdateTransition(currentTransition.fromLayerId, currentTransition.toLayerId, type, durMs)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TransitionPickerPanel(
    transition: ClipTransition?,
    onSelectTransition: (TransitionType, Long) -> Unit
) {
    val currentType = transition?.type ?: TransitionType.NONE
    var durationSec by remember(transition?.id) { mutableFloatStateOf((transition?.durationMs ?: 500L) / 1000f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Clip Transition Effect", style = MaterialTheme.typography.bodySmall)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(TransitionType.entries) { type ->
                val isSelected = currentType == type
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTransition(type, (durationSec * 1000).toLong()) },
                    label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (currentType != TransitionType.NONE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Duration: ${String.format(Locale.US, "%.1fs", durationSec)}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = durationSec,
                    onValueChange = {
                        durationSec = it
                        onSelectTransition(currentType, (it * 1000).toLong())
                    },
                    valueRange = 0.3f..1.0f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ChromaAndMaskControlPanel(
    chromaSettings: ChromaKeySettings,
    maskSettings: MaskSettings,
    onChromaChanged: (ChromaKeySettings) -> Unit,
    onMaskChanged: (MaskSettings) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        TabRow(selectedTabIndex = activeTab, containerColor = Color(0xFF1E1E1E), modifier = Modifier.height(36.dp)) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Chroma Key", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("Mask", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Chroma Key", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = chromaSettings.isEnabled,
                    onCheckedChange = { onChromaChanged(chromaSettings.copy(isEnabled = it)) }
                )
            }

            if (chromaSettings.isEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        0x00FF00L to "Green",
                        0x0000FFL to "Blue",
                        0xFFFFFFL to "White"
                    ).forEach { (hex, _) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color((0xFF000000L or hex).toInt()))
                                .border(
                                    width = if (chromaSettings.keyColorHex == hex) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { onChromaChanged(chromaSettings.copy(keyColorHex = hex)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Intensity: ${(chromaSettings.similarityThreshold * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = chromaSettings.similarityThreshold,
                        onValueChange = { onChromaChanged(chromaSettings.copy(similarityThreshold = it)) },
                        valueRange = 0.1f..0.8f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(MaskType.entries) { type ->
                    FilterChip(
                        selected = maskSettings.type == type,
                        onClick = { onMaskChanged(maskSettings.copy(type = type)) },
                        label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (maskSettings.type != MaskType.NONE) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Invert Mask", style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = maskSettings.isInverted,
                        onCheckedChange = { onMaskChanged(maskSettings.copy(isInverted = it)) }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Feather Softness", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = maskSettings.feather,
                        onValueChange = { onMaskChanged(maskSettings.copy(feather = it)) },
                        valueRange = 0.0f..0.3f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

enum class EditorAction {
    EDIT, ADJUST, FILTER, CANVAS, TEXT, AUDIO, PIP, TRANSITION
}
