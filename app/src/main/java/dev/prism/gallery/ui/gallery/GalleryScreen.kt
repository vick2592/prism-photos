package dev.prism.gallery.ui.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.prism.gallery.data.model.MediaItem
import dev.prism.gallery.ui.components.MediaGrid

@Composable
fun GalleryScreen(
    onMediaClick: (Long) -> Unit,
    onSlideshowClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    onScrollDirectionChange: ((scrollingDown: Boolean) -> Unit)? = null,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Clear selection on back press when selecting
    BackHandler(enabled = selectedIds.isNotEmpty()) { viewModel.clearSelection() }

    // Flat list of all currently visible items (needed for trash/delete actions)
    val allItems: List<MediaItem> = when (val s = uiState) {
        is GalleryUiState.Success -> s.mediaGroups.values.flatten()
        else -> emptyList()
    }

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.loadMedia()
        }
    }

    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.loadMedia()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    when (val state = uiState) {
        is GalleryUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is GalleryUiState.PermissionRequired -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Storage permission is required to view your photos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        is GalleryUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Error: ${state.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        is GalleryUiState.Success -> {
            if (state.mediaGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No photos yet. Take some!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    MediaGrid(
                        mediaGroups = state.mediaGroups,
                        onMediaClick = { id ->
                            if (selectedIds.isNotEmpty()) viewModel.toggleSelection(id)
                            else onMediaClick(id)
                        },
                        contentPadding = contentPadding,
                        columns = gridColumns,
                        onScrollDirectionChange = onScrollDirectionChange,
                        selectedIds = selectedIds,
                        onMediaLongClick = { id -> viewModel.toggleSelection(id) },
                    )

                    // Selection action bar — appears at the top when items are selected
                    if (selectedIds.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp),
                        ) {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Clear selection")
                            }
                            Text(
                                text = "${selectedIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            // Share
                            IconButton(onClick = {
                                val uris = allItems
                                    .filter { it.id in selectedIds }
                                    .map { it.uri }
                                    .let { java.util.ArrayList(it) }
                                val mime = if (uris.size == 1) {
                                    allItems.first { it.id in selectedIds }.mimeType
                                } else "image/*"
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = mime
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share ${uris.size} items"))
                                viewModel.clearSelection()
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                            // Trash
                            IconButton(onClick = { viewModel.trashSelected(allItems) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Move to Trash")
                            }
                            // Permanent delete
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Filled.DeleteForever,
                                    contentDescription = "Delete permanently",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (!selectedIds.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = onSlideshowClick,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    end = 16.dp,
                                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                                ),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Slideshow")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete permanently?") },
            text = { Text("${selectedIds.size} item(s) will be removed from this device and cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelectedPermanently(allItems)
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}
