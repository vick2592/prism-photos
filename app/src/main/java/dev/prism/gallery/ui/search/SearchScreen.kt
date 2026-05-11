package dev.prism.gallery.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.prism.gallery.ui.components.MediaGrid

/**
 * Phase 1: Placeholder search screen.
 * Phase 5 will add: date range picker, location search, text-based filename search.
 */
@Composable
fun SearchScreen(
    onMediaClick: (Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val results by viewModel.results.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Search — date range and location coming in Phase 5",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        val grouped = if (results.isNotEmpty()) mapOf("Results" to results) else emptyMap()
        MediaGrid(
            mediaGroups = grouped,
            onMediaClick = onMediaClick,
            contentPadding = contentPadding,
            modifier = Modifier.weight(1f),
        )
    }
}
