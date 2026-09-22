package com.example.swara_editor.videoeditor.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swara_editor.videoeditor.models.VideoProject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectLibraryScreen(
    projects: List<VideoProject>,
    onNewProjectWithUris: (List<Uri>) -> Unit,
    onOpenProject: (VideoProject) -> Unit,
    onRenameProject: (String, String) -> Unit = { _, _ -> },
    onSaveToGallery: (VideoProject) -> Unit = {},
    onDeleteProjects: (List<String>) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedProjectIds = remember { mutableStateListOf<String>() }
    var projectToRename by remember { mutableStateOf<VideoProject?>(null) }
    var projectToDeleteSingle by remember { mutableStateOf<VideoProject?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val mediaPicker = rememberProMediaPicker { uris ->
        if (uris.isNotEmpty()) {
            onNewProjectWithUris(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (isSelectionMode) {
                            Text(
                                text = "${selectedProjectIds.size} Selected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Swara Studio Gallery",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${projects.size} Saved Projects & Drafts",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedProjectIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = {
                            if (selectedProjectIds.size == projects.size) {
                                selectedProjectIds.clear()
                            } else {
                                selectedProjectIds.clear()
                                selectedProjectIds.addAll(projects.map { it.id })
                            }
                        }) {
                            Text(
                                if (selectedProjectIds.size == projects.size) "Deselect All" else "Select All",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (projects.isNotEmpty()) {
                        TextButton(onClick = { isSelectionMode = true }) {
                            Text("Select", fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            if (isSelectionMode && selectedProjectIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val selectedProjs = projects.filter { it.id in selectedProjectIds }
                                selectedProjs.forEach { onSaveToGallery(it) }
                            },
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Gallery (${selectedProjectIds.size})", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete (${selectedProjectIds.size})", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = mediaPicker,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Video Project") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (projects.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Saved Projects Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create high performance video edits and save drafts to re-edit them anytime!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = mediaPicker,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create First Project")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects, key = { it.id }) { project ->
                        val isSelected = selectedProjectIds.contains(project.id)
                        ProjectCard(
                            project = project,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelection = {
                                if (isSelected) {
                                    selectedProjectIds.remove(project.id)
                                } else {
                                    selectedProjectIds.add(project.id)
                                }
                            },
                            onOpen = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedProjectIds.remove(project.id) else selectedProjectIds.add(project.id)
                                } else {
                                    onOpenProject(project)
                                }
                            },
                            onRename = { projectToRename = project },
                            onSaveToGallery = { onSaveToGallery(project) },
                            onDelete = { projectToDeleteSingle = project }
                        )
                    }
                }
            }
        }

        // RENAME DIALOG
        if (projectToRename != null) {
            var newNameInput by remember(projectToRename) { mutableStateOf(projectToRename?.name ?: "") }
            AlertDialog(
                onDismissRequest = { projectToRename = null },
                title = { Text("Rename Project") },
                text = {
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { newNameInput = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newNameInput.isNotBlank() && projectToRename != null) {
                                onRenameProject(projectToRename!!.id, newNameInput.trim())
                                projectToRename = null
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectToRename = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // SINGLE DELETE CONFIRMATION DIALOG
        if (projectToDeleteSingle != null) {
            AlertDialog(
                onDismissRequest = { projectToDeleteSingle = null },
                title = { Text("Delete Project?") },
                text = { Text("Are you sure you want to delete '${projectToDeleteSingle?.name}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (projectToDeleteSingle != null) {
                                onDeleteProjects(listOf(projectToDeleteSingle!!.id))
                                projectToDeleteSingle = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { projectToDeleteSingle = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // MULTI DELETE CONFIRMATION DIALOG
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete ${selectedProjectIds.size} Project(s)?") },
                text = { Text("Are you sure you want to delete ${selectedProjectIds.size} project draft(s)? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteProjects(selectedProjectIds.toList())
                            selectedProjectIds.clear()
                            isSelectionMode = false
                            showDeleteConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: VideoProject,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSec = project.durationMs / 1000
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    val formattedDuration = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                if (project.coverUri != null) {
                    UriImageThumbnail(
                        uri = project.coverUri,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isSelectionMode) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp))
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Re-Edit") },
                                    onClick = {
                                        showMenu = false
                                        onOpen()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        showMenu = false
                                        onRename()
                                    },
                                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save to Gallery") },
                                    onClick = {
                                        showMenu = false
                                        onSaveToGallery()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Delete Project", color = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${project.layers.size} Clips | ${project.textOverlays.size} Text",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-Edit", fontSize = 12.sp)
                }
            }
        }
    }
}
