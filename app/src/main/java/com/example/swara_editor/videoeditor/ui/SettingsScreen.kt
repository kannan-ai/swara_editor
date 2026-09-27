package com.example.swara_editor.videoeditor.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.swara_editor.videoeditor.data.AppSettings
import com.example.swara_editor.videoeditor.data.AppTheme
import com.example.swara_editor.videoeditor.data.UpdateChecker
import com.example.swara_editor.videoeditor.data.UpdateInfo
import com.example.swara_editor.videoeditor.diagnostics.EditorDiagnosticEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateTheme: (AppTheme) -> Unit,
    onUpdateOledEnabled: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunningDiagnostic by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: THEMES & APPEARANCE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Theme Mode", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = settings.theme == AppTheme.LIGHT,
                            onClick = { onUpdateTheme(AppTheme.LIGHT) },
                            label = { Text("Light") },
                            leadingIcon = {
                                if (settings.theme == AppTheme.LIGHT) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                } else {
                                    Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                }
                            }
                        )

                        FilterChip(
                            selected = settings.theme == AppTheme.DARK || settings.theme == AppTheme.OLED,
                            onClick = { onUpdateTheme(AppTheme.DARK) },
                            label = { Text("Dark") },
                            leadingIcon = {
                                if (settings.theme == AppTheme.DARK || settings.theme == AppTheme.OLED) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                } else {
                                    Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                                }
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // OLED MODE OPTION (Only active if Dark theme is enabled)
                    val isDarkModeActive = settings.theme == AppTheme.DARK || settings.theme == AppTheme.OLED

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pure OLED Black (True Dark)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkModeActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDarkModeActive)
                                    "Turns dark pixels completely off to save battery on OLED displays"
                                else
                                    "OLED mode works only when Dark theme is enabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = settings.isOledEnabled || settings.theme == AppTheme.OLED,
                            onCheckedChange = { enabled ->
                                onUpdateOledEnabled(enabled)
                                if (enabled) {
                                    onUpdateTheme(AppTheme.OLED)
                                } else {
                                    onUpdateTheme(AppTheme.DARK)
                                }
                            },
                            enabled = isDarkModeActive
                        )
                    }
                }
            }

            // SECTION 2: EDITOR DEFAULTS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editor Defaults", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Default Export Quality", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = {}, label = { Text("720p") })
                        FilterChip(selected = true, onClick = {}, label = { Text("1080p Full HD") })
                        FilterChip(selected = false, onClick = {}, label = { Text("4K Ultra HD") })
                    }
                }
            }

            // SECTION 3: DIAGNOSTICS & ABOUT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About & Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Swara Editor Engine v0.0.1", style = MaterialTheme.typography.bodySmall)
                    Text("Media3 ExoPlayer & Transformer 1.11.0", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))

                    var isCheckingUpdate by remember { mutableStateOf(false) }
                    var updateCheckInfo by remember { mutableStateOf<UpdateInfo?>(null) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isCheckingUpdate = true
                                scope.launch {
                                    val info = UpdateChecker.checkForUpdates()
                                    isCheckingUpdate = false
                                    updateCheckInfo = info
                                }
                            },
                            enabled = !isCheckingUpdate,
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check for Updates", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isRunningDiagnostic = true
                                scope.launch {
                                    val engine = EditorDiagnosticEngine(context)
                                    val results = engine.runFullDiagnostic()
                                    val passed = results.count { it.isPassed }
                                    isRunningDiagnostic = false
                                    Log.i("SWARA_DIAGNOSTIC", "Diagnostic complete: $passed/9 Passed")
                                }
                            },
                            enabled = !isRunningDiagnostic,
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            if (isRunningDiagnostic) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diagnostics", fontSize = 12.sp)
                            }
                        }
                    }

                    if (updateCheckInfo != null) {
                        val info = updateCheckInfo!!
                        AlertDialog(
                            onDismissRequest = { updateCheckInfo = null },
                            icon = {
                                Icon(
                                    imageVector = if (info.hasUpdate) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            title = {
                                Text(if (info.hasUpdate) "New Update Available (${info.latestVersion})" else "Swara Editor is Up to Date")
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (info.hasUpdate)
                                            "A new version (${info.latestVersion}) is available on GitHub!"
                                        else
                                            "You are running the latest version (${info.currentVersion}). Connected live to GitHub (kannan-ai/swara_editor)."
                                    )
                                    if (info.releaseNotes.isNotBlank()) {
                                        Text(
                                            text = info.releaseNotes.take(300),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                if (info.hasUpdate) {
                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                                            context.startActivity(intent)
                                            updateCheckInfo = null
                                        }
                                    ) {
                                        Text("Download Update")
                                    }
                                } else {
                                    Button(onClick = { updateCheckInfo = null }) {
                                        Text("OK")
                                    }
                                }
                            },
                            dismissButton = {
                                if (info.hasUpdate) {
                                    TextButton(onClick = { updateCheckInfo = null }) {
                                        Text("Later")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
