package dev.prism.gallery.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val slideshowInterval by viewModel.slideshowInterval.collectAsStateWithLifecycle()
    val nonCameraAlbums by viewModel.nonCameraAlbums.collectAsStateWithLifecycle()
    val extraGalleryBucketIds by viewModel.extraGalleryBucketIds.collectAsStateWithLifecycle()
    var showAlbumPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        HorizontalDivider()

        // Grid columns
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Grid columns", style = MaterialTheme.typography.titleMedium)
            Text(
                "Controls how many columns show in the gallery grid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(2, 3, 4).forEach { count ->
                    FilterChip(
                        selected = gridColumns == count,
                        onClick = { viewModel.setGridColumns(count) },
                        label = { Text("$count") },
                    )
                }
            }
        }

        HorizontalDivider()

        // Slideshow interval
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Slideshow interval", style = MaterialTheme.typography.titleMedium)
            Text(
                "How long each photo is shown during a slideshow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(2 to "2s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (secs, label) ->
                    FilterChip(
                        selected = slideshowInterval == secs,
                        onClick = { viewModel.setSlideshowInterval(secs) },
                        label = { Text(label) },
                    )
                }
            }
        }

        HorizontalDivider()

        // Gallery sources
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gallery sources", style = MaterialTheme.typography.titleMedium)
            Text(
                "All DCIM folders (camera roll) always appear. Add non-DCIM albums below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val extraCount = extraGalleryBucketIds.size
            OutlinedButton(onClick = { showAlbumPicker = true }) {
                Text(
                    if (nonCameraAlbums.isEmpty()) "No extra albums on device"
                    else if (extraCount == 0) "Add albums to gallery…"
                    else "$extraCount album${if (extraCount == 1) "" else "s"} added — tap to change"
                )
            }
        }

        HorizontalDivider()

        Text(
            "Prism — Local photo gallery. No cloud. No tracking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }

    // Album picker dialog
    if (showAlbumPicker) {
        AlertDialog(
            onDismissRequest = { showAlbumPicker = false },
            title = { Text("Add albums to gallery") },
            text = {
                if (nonCameraAlbums.isEmpty()) {
                    Text("No non-DCIM albums found on this device.")
                } else {
                    LazyColumn {
                        items(nonCameraAlbums, key = { it.id }) { album ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                            ) {
                                Checkbox(
                                    checked = album.id in extraGalleryBucketIds,
                                    onCheckedChange = { viewModel.toggleGalleryBucket(album.id) },
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(album.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${album.count} item${if (album.count == 1) "" else "s"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlbumPicker = false }) { Text("Done") }
            },
        )
    }
}

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val slideshowInterval by viewModel.slideshowInterval.collectAsStateWithLifecycle()
    val nonCameraAlbums by viewModel.nonCameraAlbums.collectAsStateWithLifecycle()
    val extraGalleryBucketIds by viewModel.extraGalleryBucketIds.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        HorizontalDivider()

        // Grid columns
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Grid columns", style = MaterialTheme.typography.titleMedium)
            Text(
                "Controls how many columns show in the gallery grid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(2, 3, 4).forEach { count ->
                    FilterChip(
                        selected = gridColumns == count,
                        onClick = { viewModel.setGridColumns(count) },
                        label = { Text("$count") },
                    )
                }
            }
        }

        HorizontalDivider()

        // Slideshow interval
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Slideshow interval", style = MaterialTheme.typography.titleMedium)
            Text(
                "How long each photo is shown during a slideshow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(2 to "2s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (secs, label) ->
                    FilterChip(
                        selected = slideshowInterval == secs,
                        onClick = { viewModel.setSlideshowInterval(secs) },
                        label = { Text(label) },
                    )
                }
            }
        }

        HorizontalDivider()

        // Gallery sources
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gallery sources", style = MaterialTheme.typography.titleMedium)
            Text(
                "Camera roll always appears in the gallery. Tap an album below to also show it in the main gallery view.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (nonCameraAlbums.isEmpty()) {
                Text(
                    "No other albums found on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(nonCameraAlbums, key = { it.id }) { album ->
                        FilterChip(
                            selected = album.id in extraGalleryBucketIds,
                            onClick = { viewModel.toggleGalleryBucket(album.id) },
                            label = { Text("${album.name} (${album.count})") },
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            "Prism — Local photo gallery. No cloud. No tracking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

