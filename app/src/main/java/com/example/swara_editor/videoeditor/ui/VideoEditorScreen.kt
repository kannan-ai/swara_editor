package com.example.swara_editor.videoeditor.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.swara_editor.videoeditor.models.AssetType
import com.example.swara_editor.videoeditor.models.CanvasAspectRatio
import androidx.media3.common.PlaybackParameters
import com.example.swara_editor.videoeditor.diagnostics.EditorDiagnosticEngine
import com.example.swara_editor.videoeditor.models.CanvasSettings
import com.example.swara_editor.videoeditor.models.CaptionStyle
import com.example.swara_editor.videoeditor.models.ChromaKeySettings
import com.example.swara_editor.videoeditor.models.ClipTransition
import com.example.swara_editor.videoeditor.models.ColorGradingSettings
import com.example.swara_editor.videoeditor.models.KeyframePoint
import com.example.swara_editor.videoeditor.models.KeyframeTrack
import com.example.swara_editor.videoeditor.models.MaskSettings
import com.example.swara_editor.videoeditor.models.MaskType
import com.example.swara_editor.videoeditor.models.SpeedCurvePoint
import com.example.swara_editor.videoeditor.models.SpeedCurvePreset
import com.example.swara_editor.videoeditor.models.TransitionType
import com.example.swara_editor.videoeditor.utils.AutoCaptionGenerator
import com.example.swara_editor.videoeditor.utils.CompositingGlEffect
import com.example.swara_editor.videoeditor.utils.SpeedCurveEngine
import com.example.swara_editor.videoeditor.utils.applyGradingToPlayer
import com.example.swara_editor.videoeditor.utils.buildGradingPipeline
import com.example.swara_editor.videoeditor.models.FilterType
import com.example.swara_editor.videoeditor.models.ProjectAsset
import com.example.swara_editor.videoeditor.models.TextWordArtStyle
import com.example.swara_editor.videoeditor.models.TimedTextOverlay
import com.example.swara_editor.videoeditor.models.VideoLayer
import com.example.swara_editor.videoeditor.models.VideoProject
import com.example.swara_editor.videoeditor.utils.MediaProcessor
import com.example.swara_editor.videoeditor.utils.VideoExportManager
import android.graphics.Bitmap
import android.util.Log
import android.view.SurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
fun buildCanvasEffects(canvasSettings: CanvasSettings): List<Effect> {
    val effects = mutableListOf<Effect>()

    when (canvasSettings.targetRatio) {
        CanvasAspectRatio.PORTRAIT_9_16 -> {
            effects.add(
                Presentation.createForWidthAndHeight(
                    1080,
                    1920,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            )
        }
        CanvasAspectRatio.LANDSCAPE_16_9 -> {
            effects.add(
                Presentation.createForWidthAndHeight(
                    1920,
                    1080,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            )
        }
        CanvasAspectRatio.SQUARE_1_1 -> {
            effects.add(
                Presentation.createForWidthAndHeight(
                    1080,
                    1080,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            )
        }
        CanvasAspectRatio.PORTRAIT_4_5 -> {
            effects.add(
                Presentation.createForWidthAndHeight(
                    1080,
                    1350,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            )
        }
        CanvasAspectRatio.ORIGINAL -> {
            // Passthrough
        }
    }

    return effects
}

@Composable
fun UriImageThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.Gray))
    }
}

fun updatePlayerSpeed(player: ExoPlayer, clip: VideoLayer, currentPositionMs: Long) {
    val curvePoints = if (clip.customCurvePoints.isNotEmpty()) {
        clip.customCurvePoints
    } else {
        clip.speedCurve?.points ?: return
    }

    val clipRelativeMs = currentPositionMs - clip.startTimeMs
    val rawDuration = clip.durationMs
    if (rawDuration <= 0L) return

    val progress = (clipRelativeMs.toFloat() / rawDuration.toFloat()).coerceIn(0f, 1f)
    val targetSpeed = SpeedCurveEngine.getSpeedAt(curvePoints, progress)

    if (abs(player.playbackParameters.speed - targetSpeed) > 0.05f) {
        player.playbackParameters = PlaybackParameters(targetSpeed, 1.0f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun VideoEditorStandalone(
    initialProject: VideoProject? = null,
    onSaveProject: (VideoProject) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var project by remember {
        mutableStateOf(initialProject ?: VideoProject(id = UUID.randomUUID().toString(), name = "New Project"))
    }
    
    val handleClose = {
        onSaveProject(project)
        onClose()
    }
    var currentTimeMs by remember { mutableLongStateOf(0L) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var activeAction by remember { mutableStateOf<String?>(null) } // "EDIT", "ADJUST", "FILTER", "TEXT", "AUDIO", "PIP", "TRANSITION"
    var activeTransitionPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedFilter by remember { mutableStateOf(FilterType.NORMAL) }
    var isGeneratingCaptions by remember { mutableStateOf(false) }
    val timelineScaleState = remember { TimelineScaleState() }

    // Export Sheet & Manager State
    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val exportManager = remember { VideoExportManager(context) }

    // Undo / Redo history stacks
    val undoStack = remember { ArrayDeque<VideoProject>() }
    val redoStack = remember { ArrayDeque<VideoProject>() }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }

    fun saveSnapshot() {
        undoStack.addLast(project.copy())
        redoStack.clear()
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(project.copy())
        project = undoStack.removeLast()
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
        Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(project.copy())
        project = redoStack.removeLast()
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
        Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
        }
    }

    // Sync playhead position with ExoPlayer
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentTimeMs = exoPlayer.currentPosition
            }
            delay(100.milliseconds)
        }
    }

    LaunchedEffect(exoPlayer, project, currentTimeMs) {
        val currentClip = project.layers.find { layer ->
            currentTimeMs >= layer.startTimeMs && currentTimeMs <= layer.startTimeMs + layer.durationMs
        }
        if (currentClip != null && (currentClip.speedCurve != null || currentClip.customCurvePoints.isNotEmpty())) {
            updatePlayerSpeed(exoPlayer, currentClip, currentTimeMs)
        }
    }

    fun applyFilter(filter: FilterType) {
        saveSnapshot()
        selectedFilter = filter
        if (!exoPlayer.isPlaying) {
            exoPlayer.seekTo(exoPlayer.currentPosition)
        }
        Toast.makeText(context, "Applied filter: ${filter.name}", Toast.LENGTH_SHORT).show()
    }

    fun updateBrightness(factor: Float) {
        if (!exoPlayer.isPlaying) {
            exoPlayer.seekTo(exoPlayer.currentPosition)
        }
        Toast.makeText(context, "Brightness: ${(factor * 100).toInt()}%", Toast.LENGTH_SHORT).show()
    }

    fun addTextOverlay(text: String, style: TextWordArtStyle) {
        saveSnapshot()
        val playheadPos = exoPlayer.currentPosition
        val overlayDuration = 3000L
        val newOverlay = TimedTextOverlay(
            id = UUID.randomUUID().toString(),
            text = text,
            startTimeMs = playheadPos,
            durationMs = overlayDuration,
            stylePreset = style
        )
        val newTotalDuration = project.durationMs.coerceAtLeast(playheadPos + overlayDuration)
        project = project.copy(
            textOverlays = project.textOverlays + newOverlay,
            durationMs = newTotalDuration
        )
        selectedLayerId = newOverlay.id
        Toast.makeText(context, "Added WordArt text", Toast.LENGTH_SHORT).show()
    }

    fun toggleMuteLayer(layerId: String) {
        saveSnapshot()
        val updatedLayers = project.layers.map { layer ->
            if (layer.id == layerId) {
                val newVol = if (layer.volume == 0f) 1.0f else 0f
                layer.copy(volume = newVol)
            } else layer
        }
        project = project.copy(layers = updatedLayers)

        val targetLayer = updatedLayers.find { it.id == layerId }
        if (targetLayer != null) {
            exoPlayer.volume = targetLayer.volume
            Toast.makeText(
                context,
                if (targetLayer.volume == 0f) "Original audio muted" else "Original audio unmuted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateLayerSpeed(layerId: String, speed: Float) {
        saveSnapshot()
        val updatedLayers = project.layers.map { layer ->
            if (layer.id == layerId) layer.copy(speed = speed, speedCurve = null, customCurvePoints = emptyList()) else layer
        }
        project = project.copy(layers = updatedLayers)
        exoPlayer.setPlaybackSpeed(speed)
        Toast.makeText(context, "Playback speed set to ${speed}x", Toast.LENGTH_SHORT).show()
    }

    fun updateSpeedCurve(layerId: String, preset: SpeedCurvePreset?, points: List<SpeedCurvePoint>) {
        saveSnapshot()
        val updatedLayers = project.layers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(
                    speedCurve = preset,
                    customCurvePoints = points
                )
            } else layer
        }
        project = project.copy(layers = updatedLayers)
    }

    fun splitCurrentClip() {
        val selectedId = selectedLayerId ?: project.layers.firstOrNull()?.id ?: run {
            Toast.makeText(context, "No clip selected to split", Toast.LENGTH_SHORT).show()
            return
        }
        val clip = project.layers.find { it.id == selectedId } ?: return
        val splitTime = exoPlayer.currentPosition

        if (splitTime <= clip.startTimeMs || splitTime >= clip.startTimeMs + clip.durationMs) {
            Toast.makeText(context, "Move playhead inside clip to split", Toast.LENGTH_SHORT).show()
            return
        }

        saveSnapshot()
        val duration1 = splitTime - clip.startTimeMs
        val duration2 = clip.durationMs - duration1

        val part1 = clip.copy(id = UUID.randomUUID().toString(), durationMs = duration1)
        val part2 = clip.copy(id = UUID.randomUUID().toString(), startTimeMs = splitTime, durationMs = duration2, startOffsetMs = clip.startOffsetMs + duration1)

        val index = project.layers.indexOf(clip)
        val newLayers = project.layers.toMutableList().apply {
            removeAt(index)
            add(index, part2)
            add(index, part1)
        }
        project = project.copy(layers = newLayers)
        Toast.makeText(context, "Clip split successfully", Toast.LENGTH_SHORT).show()
    }

    fun deleteSelectedClip(clipId: String) {
        saveSnapshot()
        val currentLayers = project.layers.toMutableList()
        val currentTextOverlays = project.textOverlays.toMutableList()

        val removedLayer = currentLayers.removeAll { id -> id.id == clipId }
        val removedText = currentTextOverlays.removeAll { id -> id.id == clipId }

        if (removedLayer || removedText) {
            selectedLayerId = currentLayers.lastOrNull()?.id ?: currentTextOverlays.lastOrNull()?.id
            project = project.copy(
                layers = currentLayers,
                textOverlays = currentTextOverlays
            )
            Toast.makeText(context, "Layer deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun extractAudioFromClip(videoLayerId: String) {
        val videoLayer = project.layers.find { it.id == videoLayerId } ?: return
        val asset = project.assets.find { it.id == videoLayer.assetId } ?: return

        saveSnapshot()
        val updatedVideoLayer = videoLayer.copy(volume = 0f)

        val newAudioAsset = ProjectAsset(
            id = UUID.randomUUID().toString(),
            uri = asset.uri,
            name = "Extracted Audio",
            type = AssetType.AUDIO,
            durationMs = videoLayer.durationMs,
            width = 0,
            height = 0
        )

        val newAudioLayer = VideoLayer(
            id = UUID.randomUUID().toString(),
            assetId = newAudioAsset.id,
            startTimeMs = videoLayer.startTimeMs,
            durationMs = videoLayer.durationMs,
            startOffsetMs = videoLayer.startOffsetMs
        )

        val updatedLayers = project.layers.map { if (it.id == videoLayer.id) updatedVideoLayer else it } + newAudioLayer
        project = project.copy(
            assets = project.assets + newAudioAsset,
            layers = updatedLayers
        )
        selectedLayerId = newAudioLayer.id
        Toast.makeText(context, "Audio extracted to Audio Track", Toast.LENGTH_SHORT).show()
    }

    fun updateAudioFade(audioLayerId: String, fadeInMs: Long, fadeOutMs: Long) {
        saveSnapshot()
        val updatedLayers = project.layers.map { layer ->
            if (layer.id == audioLayerId) {
                layer.copy(fadeInMs = fadeInMs, fadeOutMs = fadeOutMs)
            } else layer
        }
        project = project.copy(layers = updatedLayers)
        Toast.makeText(context, "Audio fade updated", Toast.LENGTH_SHORT).show()
    }

    fun updateColorGrading(newGrading: ColorGradingSettings) {
        val selectedId = selectedLayerId ?: project.layers.firstOrNull()?.id ?: return
        val updatedLayers = project.layers.map { layer ->
            if (layer.id == selectedId) {
                layer.copy(colorGrading = newGrading)
            } else layer
        }
        project = project.copy(layers = updatedLayers)
        applyGradingToPlayer(exoPlayer, newGrading)
    }

    fun updateChromaKey(layerId: String, chromaSettings: ChromaKeySettings) {
        saveSnapshot()
        val updatedLayers = project.layers.map { layer ->
            if (layer.id == layerId) layer.copy(chromaKey = chromaSettings) else layer
        }
        project = project.copy(layers = updatedLayers)
        Toast.makeText(context, "Chroma Key updated", Toast.LENGTH_SHORT).show()
    }

    fun updateMaskSettings(layerId: String, maskSettings: MaskSettings) {
        saveSnapshot()
        val updatedLayers = project.layers.map { layer ->
            if (layer.id == layerId) layer.copy(mask = maskSettings) else layer
        }
        project = project.copy(layers = updatedLayers)
        Toast.makeText(context, "Mask settings updated", Toast.LENGTH_SHORT).show()
    }

    fun generateAutoCaptions() {
        val primaryUri = project.assets.firstOrNull()?.uri ?: run {
            Toast.makeText(context, "Add video/audio media first!", Toast.LENGTH_SHORT).show()
            return
        }

        saveSnapshot()
        isGeneratingCaptions = true
        scope.launch {
            val generated = AutoCaptionGenerator.transcribeTimelineAudio(context, primaryUri, project.durationMs)
            project = project.copy(autoCaptions = generated)
            isGeneratingCaptions = false
            Toast.makeText(context, "Generated ${generated.size} auto-captions!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateCaptionStyle(newStyle: CaptionStyle) {
        saveSnapshot()
        val updatedCaptions = project.autoCaptions.map { it.copy(style = newStyle) }
        project = project.copy(autoCaptions = updatedCaptions)
        Toast.makeText(context, "Caption style updated", Toast.LENGTH_SHORT).show()
    }

    fun updateTextOverlay(id: String, newText: String, style: TextWordArtStyle) {
        saveSnapshot()
        val updatedOverlays = project.textOverlays.map { overlay ->
            if (overlay.id == id) {
                overlay.copy(text = newText, stylePreset = style)
            } else overlay
        }
        project = project.copy(textOverlays = updatedOverlays)
    }

    fun runDiagnostic() {
        scope.launch {
            val engine = EditorDiagnosticEngine(context)
            val results = engine.runFullDiagnostic()
            val passedCount = results.count { it.isPassed }
            val summary = buildString {
                appendLine("=== SWARA EDITOR DIAGNOSTIC REPORT ===")
                results.forEach {
                    val tag = if (it.isPassed) "[PASS]" else "[FAIL]"
                    appendLine("$tag Phase ${it.phaseNumber}: ${it.name} - ${it.message}")
                }
            }
            Log.i("SWARA_DIAGNOSTIC", summary)
            Toast.makeText(context, "Diagnostics: $passedCount/9 Passed. Check Logcat.", Toast.LENGTH_LONG).show()
        }
    }

    fun updateTransition(fromLayerId: String, toLayerId: String, type: TransitionType, durationMs: Long) {
        saveSnapshot()
        val currentTransitions = project.transitions.toMutableList()
        currentTransitions.removeAll { it.fromLayerId == fromLayerId && it.toLayerId == toLayerId }

        if (type != TransitionType.NONE) {
            currentTransitions.add(
                ClipTransition(
                    id = UUID.randomUUID().toString(),
                    fromLayerId = fromLayerId,
                    toLayerId = toLayerId,
                    type = type,
                    durationMs = durationMs
                )
            )
        }

        project = project.copy(transitions = currentTransitions)
        Toast.makeText(context, "Transition updated: ${type.displayName}", Toast.LENGTH_SHORT).show()
    }

    val hasKeyframeAtPlayhead = remember(selectedLayerId, currentTimeMs, project) {
        val selectedId = selectedLayerId ?: return@remember false
        val videoLayer = project.layers.find { it.id == selectedId }
        val textOverlay = project.textOverlays.find { it.id == selectedLayerId }
        val keyframes = videoLayer?.keyframeTrack?.keyframes ?: textOverlay?.keyframeTrack?.keyframes ?: emptyList()
        keyframes.any { abs(it.timeMs - currentTimeMs) < 150L }
    }

    fun addOrRemoveKeyframe() {
        val selectedId = selectedLayerId ?: run {
            Toast.makeText(context, "Select a clip or text overlay first!", Toast.LENGTH_SHORT).show()
            return
        }
        val videoLayer = project.layers.find { it.id == selectedId }
        val textOverlay = project.textOverlays.find { it.id == selectedId }

        saveSnapshot()

        if (videoLayer != null) {
            val track = videoLayer.keyframeTrack ?: KeyframeTrack(layerId = selectedId)
            val existing = track.keyframes.find { abs(it.timeMs - currentTimeMs) < 150L }
            val newKeyframes = if (existing != null) {
                track.keyframes.filterNot { it == existing }
            } else {
                (track.keyframes + KeyframePoint(timeMs = currentTimeMs)).sortedBy { it.timeMs }
            }
            val updatedLayer = videoLayer.copy(keyframeTrack = track.copy(keyframes = newKeyframes))
            project = project.copy(layers = project.layers.map { if (it.id == selectedId) updatedLayer else it })
            Toast.makeText(context, if (existing != null) "Keyframe removed" else "Keyframe added at ${currentTimeMs}ms", Toast.LENGTH_SHORT).show()
        } else if (textOverlay != null) {
            val track = textOverlay.keyframeTrack ?: KeyframeTrack(layerId = selectedId)
            val existing = track.keyframes.find { abs(it.timeMs - currentTimeMs) < 150L }
            val newKeyframes = if (existing != null) {
                track.keyframes.filterNot { it == existing }
            } else {
                (track.keyframes + KeyframePoint(timeMs = currentTimeMs)).sortedBy { it.timeMs }
            }
            val updatedOverlay = textOverlay.copy(keyframeTrack = track.copy(keyframes = newKeyframes))
            project = project.copy(textOverlays = project.textOverlays.map { if (it.id == selectedId) updatedOverlay else it })
            Toast.makeText(context, if (existing != null) "Keyframe removed" else "Keyframe added at ${currentTimeMs}ms", Toast.LENGTH_SHORT).show()
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                saveSnapshot()
                project = project.copy(coverUri = uri)
                Toast.makeText(context, "Cover thumbnail updated", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(selectedLayerId) {
        val selectedLayer = project.layers.find { it.id == selectedLayerId }
        val grading = selectedLayer?.colorGrading ?: ColorGradingSettings()
        applyGradingToPlayer(exoPlayer, grading)
    }

    LaunchedEffect(project.layers, project.assets) {
        if (selectedLayerId == null && project.layers.isNotEmpty()) {
            selectedLayerId = project.layers.first().id
        }
        exoPlayer.clearMediaItems()
        project.layers.forEach { layer ->
            project.assets.find { it.id == layer.assetId }?.uri?.let { uri ->
                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(layer.startOffsetMs)
                    .setEndPositionMs(layer.startOffsetMs + layer.durationMs)
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(clippingConfig)
                    .build()
                exoPlayer.addMediaItem(mediaItem)
            }
        }
        if (project.layers.isNotEmpty()) {
            exoPlayer.prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val mediaPicker = rememberProMediaPicker { uris ->
        if (uris.isEmpty()) return@rememberProMediaPicker

        saveSnapshot()
        val newAssets = uris.map { uri ->
            val mimeType = context.contentResolver.getType(uri)
            val isImage = mimeType?.startsWith("image") == true
            val duration = if (isImage) 3000L else MediaProcessor.calculateDuration(context, uri)
            
            ProjectAsset(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = uri.lastPathSegment ?: "Media",
                type = if (isImage) AssetType.IMAGE else AssetType.VIDEO,
                durationMs = duration,
                width = 1920,
                height = 1080
            )
        }

        val newLayers = newAssets.map { asset ->
            VideoLayer(
                id = UUID.randomUUID().toString(),
                assetId = asset.id,
                startTimeMs = project.durationMs,
                durationMs = asset.durationMs
            )
        }

        project = project.copy(
            assets = project.assets + newAssets,
            layers = project.layers + newLayers,
            durationMs = project.durationMs + newLayers.sumOf { it.durationMs }
        )

        if (selectedLayerId == null && newLayers.isNotEmpty()) {
            selectedLayerId = newLayers.first().id
        }
        
        Toast.makeText(context, "Added ${uris.size} media items", Toast.LENGTH_SHORT).show()
    }

    val audioPicker = rememberProMediaPicker { uris ->
        if (uris.isEmpty()) return@rememberProMediaPicker

        saveSnapshot()
        val audioAssets = uris.map { uri ->
            ProjectAsset(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = uri.lastPathSegment ?: "Music Track",
                type = AssetType.AUDIO,
                durationMs = MediaProcessor.calculateDuration(context, uri),
                width = 0,
                height = 0
            )
        }

        val audioLayers = audioAssets.map { asset ->
            VideoLayer(
                id = UUID.randomUUID().toString(),
                assetId = asset.id,
                startTimeMs = 0L,
                durationMs = asset.durationMs
            )
        }

        project = project.copy(
            assets = project.assets + audioAssets,
            layers = project.layers + audioLayers
        )

        Toast.makeText(context, "Added ${uris.size} audio tracks", Toast.LENGTH_SHORT).show()
    }

    if (showExportSheet) {
        ExportBottomSheet(
            totalDurationMs = project.durationMs,
            isExporting = isExporting,
            progress = exportProgress,
            onStartExport = { quality ->
                isExporting = true
                val outputFile = File(context.cacheDir, "Swara_Export_${System.currentTimeMillis()}.mp4")
                
                val canvasVideoEffects = buildCanvasEffects(project.canvasSettings)
                val items = project.layers.map { layer ->
                    val asset = project.assets.find { it.id == layer.assetId }
                    val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(layer.startOffsetMs)
                        .setEndPositionMs(layer.startOffsetMs + layer.durationMs)
                        .build()
                    val mediaItem = MediaItem.Builder()
                        .setUri(asset?.uri ?: Uri.EMPTY)
                        .setClippingConfiguration(clippingConfig)
                        .build()

                    val layerGradingEffects = buildGradingPipeline(layer.colorGrading)
                    val compositingEffect = CompositingGlEffect(layer.chromaKey, layer.mask)
                    val combinedEffects = layerGradingEffects + compositingEffect + canvasVideoEffects

                    EditedMediaItem.Builder(mediaItem)
                        .setEffects(Effects(emptyList(), combinedEffects))
                        .build()
                }
                val composition = Composition.Builder(
                    EditedMediaItemSequence.withAudioAndVideoFrom(items)
                ).build()

                exportManager.exportComposition(
                    composition = composition,
                    quality = quality,
                    outputPath = outputFile.absolutePath,
                    scope = scope
                )

                scope.launch {
                    exportManager.exportProgress.collect { prog ->
                        exportProgress = prog
                        if (prog == 100) {
                            isExporting = false
                            showExportSheet = false
                            Toast.makeText(context, "Export complete: ${outputFile.name}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDismiss = { showExportSheet = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(Color.DarkGray)
                                .clickable { coverPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (project.coverUri != null) {
                                UriImageThumbnail(
                                    uri = project.coverUri!!,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Edit Thumbnail",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = project.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = { runDiagnostic() }) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = "Run Diagnostics",
                            tint = Color.Cyan
                        )
                    }
                    KeyframeActionButton(
                        hasKeyframeAtPlayhead = hasKeyframeAtPlayhead,
                        onAddKeyframe = { addOrRemoveKeyframe() },
                        onRemoveKeyframe = { addOrRemoveKeyframe() }
                    )
                    IconButton(onClick = { undo() }, enabled = canUndo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.White else Color.Gray
                        )
                    }
                    IconButton(onClick = { redo() }, enabled = canRedo) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.White else Color.Gray
                        )
                    }
                    IconButton(onClick = { 
                        if (project.layers.isNotEmpty()) {
                            showExportSheet = true
                        } else {
                            Toast.makeText(context, "Add media first!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Export")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 2. Video Preview Stage (Must take all available vertical space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (project.layers.isEmpty()) {
                    Button(onClick = mediaPicker) {
                        Text("Add Media to Start")
                    }
                } else {
                    VideoPreviewStage(
                        exoPlayer = exoPlayer,
                        canvasSettings = project.canvasSettings,
                        currentTimeMs = currentTimeMs,
                        project = project,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3. Middle Action & Property Sub-Panel (Fixed/Wrap Content)
            if (activeAction != null && project.layers.isNotEmpty()) {
                val action = runCatching { EditorAction.valueOf(activeAction!!) }.getOrNull()
                if (action != null) {
                    PropertyPanel(
                        action = action,
                        project = project,
                        selectedLayerId = selectedLayerId,
                        selectedFilter = selectedFilter,
                        activeTransitionPair = activeTransitionPair,
                        onFilterSelected = { filter -> applyFilter(filter) },
                        onAddText = { text, style -> addTextOverlay(text, style) },
                        onUpdateTextOverlay = { id, text, style -> updateTextOverlay(id, text, style) },
                        onAddAudio = { audioPicker() },
                        onToggleMute = { layerId -> toggleMuteLayer(layerId) },
                        onUpdateSpeed = { layerId, speed -> updateLayerSpeed(layerId, speed) },
                        onUpdateSpeedCurve = { layerId, preset, points -> updateSpeedCurve(layerId, preset, points) },
                        onUpdateBrightness = { factor -> updateBrightness(factor) },
                        onUpdateCanvasSettings = { newCanvas -> project = project.copy(canvasSettings = newCanvas) },
                        onUpdateColorGrading = { newGrading -> updateColorGrading(newGrading) },
                        onUpdateChromaKey = { layerId, chroma -> updateChromaKey(layerId, chroma) },
                        onUpdateMaskSettings = { layerId, mask -> updateMaskSettings(layerId, mask) },
                        onUpdateTransition = { fromId, toId, type, durationMs -> updateTransition(fromId, toId, type, durationMs) },
                        onExtractAudio = { layerId -> extractAudioFromClip(layerId) },
                        onUpdateAudioFade = { layerId, fadeIn, fadeOut -> updateAudioFade(layerId, fadeIn, fadeOut) },
                        onGenerateCaptions = { generateAutoCaptions() },
                        isGeneratingCaptions = isGeneratingCaptions,
                        onUpdateCaptionStyle = { newStyle -> updateCaptionStyle(newStyle) },
                        onSplitClip = { splitCurrentClip() },
                        onDeleteClip = { clipId -> deleteSelectedClip(clipId) },
                        onClose = {
                            activeAction = null
                            activeTransitionPair = null
                        }
                    )
                }
            }

            // 4. Scrollable Multi-Track Timeline (Constrained to fixed height 140.dp)
            ScrollableTimelineStudio(
                project = project,
                currentPositionMs = currentTimeMs,
                selectedLayerId = selectedLayerId,
                scaleState = timelineScaleState,
                onSeekTo = { seekPos ->
                    currentTimeMs = seekPos
                    exoPlayer.seekTo(seekPos)
                },
                onClipSelect = { id ->
                    if (selectedLayerId == id) {
                        selectedLayerId = null
                    } else {
                        selectedLayerId = id
                        if (project.textOverlays.any { it.id == id }) {
                            activeAction = EditorAction.TEXT.name
                        } else if (project.layers.any { l -> l.id == id && project.assets.any { a -> a.id == l.assetId && a.type == AssetType.AUDIO } }) {
                            activeAction = EditorAction.AUDIO.name
                        }
                    }
                },
                onTransitionClick = { fromId, toId ->
                    activeTransitionPair = Pair(fromId, toId)
                    activeAction = EditorAction.TRANSITION.name
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // 5. Bottom Navigation Bar (Anchored at the bottom edge)
            EditControlPanel(
                activeAction = activeAction?.let { runCatching { EditorAction.valueOf(it) }.getOrNull() },
                onAction = { action ->
                    if (project.layers.isEmpty() && action != EditorAction.PIP && action != EditorAction.TEXT && action != EditorAction.AUDIO) {
                        Toast.makeText(context, "Add media first!", Toast.LENGTH_SHORT).show()
                    } else if (selectedLayerId == null && action != EditorAction.PIP && action != EditorAction.TEXT && action != EditorAction.AUDIO && action != EditorAction.FILTER && action != EditorAction.ADJUST && action != EditorAction.CANVAS) {
                        Toast.makeText(context, "Select a clip first!", Toast.LENGTH_SHORT).show()
                    } else {
                        activeAction = action.name
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .background(Color(0xFF141414))
            )
        }
    }
}
