package dev.prism.gallery.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

