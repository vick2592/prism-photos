package dev.prism.gallery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.prism.gallery.data.model.MediaItem

/**
 * Reusable media grid used by GalleryScreen, SearchScreen, and album detail views.
 * Accepts a map of date-label → items to render sticky date section headers.
 */
@Composable
fun MediaGrid(
    mediaGroups: Map<String, List<MediaItem>>,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    columns: Int = 3,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        mediaGroups.forEach { (dateLabel, items) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                DateHeader(label = dateLabel)
            }
            items(
                items = items,
                key = { it.id },
            ) { item ->
                MediaThumbnail(
                    item = item,
                    onClick = { onMediaClick(item.id) },
                )
            }
        }
    }
}
