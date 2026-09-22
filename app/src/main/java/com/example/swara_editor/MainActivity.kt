package com.example.swara_editor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.runtime.*
import androidx.media3.common.util.UnstableApi
import com.example.swara_editor.ui.theme.Swara_editorTheme
import com.example.swara_editor.videoeditor.data.ProjectRepository
import com.example.swara_editor.videoeditor.data.SettingsRepository
import com.example.swara_editor.videoeditor.diagnostics.EditorDiagnosticEngine
import com.example.swara_editor.videoeditor.models.AssetType
import com.example.swara_editor.videoeditor.models.ProjectAsset
import com.example.swara_editor.videoeditor.models.VideoLayer
import com.example.swara_editor.videoeditor.models.VideoProject
import com.example.swara_editor.videoeditor.ui.ProjectLibraryScreen
import com.example.swara_editor.videoeditor.ui.SettingsScreen
import com.example.swara_editor.videoeditor.ui.VideoEditorStandalone
import com.example.swara_editor.videoeditor.utils.MediaProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@UnstableApi
class DiagnosticReceiver(private val engine: EditorDiagnosticEngine) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        CoroutineScope(Dispatchers.Main).launch {
            val results = engine.runFullDiagnostic()
            val summary = buildString {
                appendLine("=== SWARA EDITOR DIAGNOSTIC REPORT ===")
                results.forEach {
                    val tag = if (it.isPassed) "[PASS]" else "[FAIL]"
                    appendLine("$tag Phase ${it.phaseNumber}: ${it.name} - ${it.message}")
                    if (it.errorDetails != null) {
                        appendLine("       Error: ${it.errorDetails}")
                    }
                }
            }
            Log.i("SWARA_DIAGNOSTIC", summary)
        }
    }
}

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var diagnosticEngine: EditorDiagnosticEngine
    private lateinit var diagnosticReceiver: DiagnosticReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        diagnosticEngine = EditorDiagnosticEngine(applicationContext)
        diagnosticReceiver = DiagnosticReceiver(diagnosticEngine)

        val projectRepository = ProjectRepository(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)

        val filter = IntentFilter("com.example.swara_editor.RUN_DIAGNOSTIC")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(diagnosticReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(diagnosticReceiver, filter)
        }

        setContent {
            var appSettings by remember { mutableStateOf(settingsRepository.getSettings()) }

            Swara_editorTheme(settings = appSettings) {
                var currentEditingProject by remember { mutableStateOf<VideoProject?>(null) }
                var isEditing by remember { mutableStateOf(false) }
                var isShowingSettings by remember { mutableStateOf(false) }
                var projectsList by remember { mutableStateOf(projectRepository.loadAllProjects()) }

                when {
                    isShowingSettings -> {
                        SettingsScreen(
                            settings = appSettings,
                            onUpdateTheme = { newTheme ->
                                settingsRepository.updateTheme(newTheme)
                                appSettings = settingsRepository.getSettings()
                            },
                            onUpdateOledEnabled = { oled ->
                                settingsRepository.updateOledEnabled(oled)
                                appSettings = settingsRepository.getSettings()
                            },
                            onClose = { isShowingSettings = false }
                        )
                    }
                    isEditing -> {
                        VideoEditorStandalone(
                            initialProject = currentEditingProject,
                            onSaveProject = { proj ->
                                projectRepository.saveProject(proj)
                                projectsList = projectRepository.loadAllProjects()
                            },
                            onOpenSettings = { isShowingSettings = true },
                            onClose = {
                                isEditing = false
                                currentEditingProject = null
                                projectsList = projectRepository.loadAllProjects()
                            }
                        )
                    }
                    else -> {
                        ProjectLibraryScreen(
                            projects = projectsList,
                            onNewProjectWithUris = { uris ->
                                val newAssets = uris.map { uri ->
                                    val mimeType = applicationContext.contentResolver.getType(uri)
                                    val isImage = mimeType?.startsWith("image") == true
                                    val duration = if (isImage) 3000L else MediaProcessor.calculateDuration(applicationContext, uri)

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

                                var timelineMs = 0L
                                val newLayers = newAssets.map { asset ->
                                    val layer = VideoLayer(
                                        id = UUID.randomUUID().toString(),
                                        assetId = asset.id,
                                        startTimeMs = timelineMs,
                                        durationMs = asset.durationMs
                                    )
                                    timelineMs += asset.durationMs
                                    layer
                                }

                                val newProj = VideoProject(
                                    id = UUID.randomUUID().toString(),
                                    name = "Project ${projectsList.size + 1}",
                                    assets = newAssets,
                                    layers = newLayers,
                                    durationMs = timelineMs,
                                    coverUri = newAssets.firstOrNull { it.type == AssetType.IMAGE }?.uri
                                )

                                projectRepository.saveProject(newProj)
                                projectsList = projectRepository.loadAllProjects()
                                currentEditingProject = newProj
                                isEditing = true
                            },
                            onOpenProject = { proj ->
                                currentEditingProject = proj
                                isEditing = true
                            },
                            onRenameProject = { projId, newName ->
                                projectRepository.renameProject(projId, newName)
                                projectsList = projectRepository.loadAllProjects()
                                Toast.makeText(applicationContext, "Renamed to '$newName'", Toast.LENGTH_SHORT).show()
                            },
                            onSaveToGallery = { proj ->
                                Toast.makeText(applicationContext, "Saved '${proj.name}' to Gallery", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteProjects = { projIds ->
                                projIds.forEach { projectRepository.deleteProject(it) }
                                projectsList = projectRepository.loadAllProjects()
                                Toast.makeText(applicationContext, "Deleted ${projIds.size} project(s)", Toast.LENGTH_SHORT).show()
                            },
                            onOpenSettings = { isShowingSettings = true }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(diagnosticReceiver)
        } catch (_: Exception) {}
    }
}
