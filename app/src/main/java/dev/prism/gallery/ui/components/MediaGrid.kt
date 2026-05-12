package dev.prism.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SCROLLBAR_END_INSET = 8.dp          // gap from screen right edge
private val SCROLLBAR_TOUCH_WIDTH = 20.dp       // touch target width
private val SCROLLBAR_TRACK_WIDTH = 3.dp        // visual track width
private val LABEL_PILL_HEIGHT = 28.dp

/**
 * Reusable media grid with a draggable scrollbar and floating day-label indicator.
 * Used by GalleryScreen, AlbumDetailScreen, and SearchScreen.
 */
@Composable
fun MediaGrid(
    mediaGroups: Map<String, List<MediaItem>>,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    columns: Int = 3,
    onScrollDirectionChange: ((scrollingDown: Boolean) -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val latestScrollCallback by rememberUpdatedState(onScrollDirectionChange)

    // NestedScrollConnection to detect scroll direction for nav-bar auto-hide.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < -4f) latestScrollCallback?.invoke(true)
                else if (dy > 4f) latestScrollCallback?.invoke(false)
                return Offset.Zero   // don't consume — let the grid scroll normally
            }
        }
    }

    // Flat index → date label map: one entry per header + one per photo.
    val itemIndexToLabel = remember(mediaGroups) {
        buildMap<Int, String> {
            var idx = 0
            mediaGroups.forEach { (label, items) ->
                put(idx++, label)
                repeat(items.size) { put(idx++, label) }
            }
        }
    }
    val totalItems = itemIndexToLabel.size

    val currentLabel by remember {
        derivedStateOf { itemIndexToLabel[gridState.firstVisibleItemIndex] ?: "" }
    }

    // Pixel-accurate thumb fraction using actual item offsets.
    val thumbFraction by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount.toFloat()
            if (total <= 0f) return@derivedStateOf 0f
            val firstIdx = gridState.firstVisibleItemIndex.toFloat()
            val firstOffset = gridState.firstVisibleItemScrollOffset.toFloat()
            val avgH = info.visibleItemsInfo
                .map { it.size.height }
                .average()
                .toFloat()
                .coerceAtLeast(1f)
            ((firstIdx + firstOffset / avgH) / total).coerceIn(0f, 1f)
        }
    }

    val thumbHeightFraction by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount.toFloat()
            val visible = info.visibleItemsInfo.size.toFloat()
            if (total == 0f) 1f else (visible / total).coerceIn(0.04f, 1f)
        }
    }

    // Show the month/day label for 1.5 s after scrolling stops.
    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            isScrolling = true
        } else {
            delay(1500L)
            isScrolling = false
        }
    }

    Box(modifier = modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            mediaGroups.forEach { (dateLabel, items) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DateHeader(label = dateLabel)
                }
                items(items = items, key = { it.id }) { item ->
                    MediaThumbnail(
                        item = item,
                        onClick = { onMediaClick(item.id) },
                    )
                }
            }
        }

        // Scrollbar — only rendered when there is more than one screen of content.
        if (totalItems > 1) {
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .padding(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding(),
                        end = SCROLLBAR_END_INSET,
                    ),
            ) {
                val density = LocalDensity.current
                val trackHeightPx = constraints.maxHeight.toFloat()
                val minThumbPx = with(density) { 40.dp.toPx() }
                val thumbPx = (trackHeightPx * thumbHeightFraction).coerceAtLeast(minThumbPx)
                val thumbTopPx = ((trackHeightPx - thumbPx) * thumbFraction)
                    .coerceIn(0f, (trackHeightPx - thumbPx).coerceAtLeast(0f))

                val thumbTopDp: Dp = with(density) { thumbTopPx.toDp() }
                val thumbHeightDp: Dp = with(density) { thumbPx.toDp() }

                val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

                // Track + thumb: 3dp visual inside a SCROLLBAR_TOUCH_WIDTH-dp tap/drag target.
                Canvas(
                    modifier = Modifier
                        .width(SCROLLBAR_TOUCH_WIDTH)
                        .fillMaxHeight()
                        .align(Alignment.TopEnd)
                        .pointerInput(totalItems, trackHeightPx) {
                            detectTapGestures { offset ->
                                val fraction = (offset.y / size.height).coerceIn(0f, 1f)
                                val idx = (fraction * totalItems).toInt().coerceIn(0, totalItems - 1)
                                scope.launch { gridState.scrollToItem(idx) }
                            }
                        }
                        .pointerInput(totalItems, trackHeightPx) {
                            detectVerticalDragGestures { change, _ ->
                                change.consume()
                                val fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                                val idx = (fraction * totalItems).toInt().coerceIn(0, totalItems - 1)
                                scope.launch { gridState.scrollToItem(idx) }
                            }
                        },
                ) {
                    val trackW = SCROLLBAR_TRACK_WIDTH.toPx()
                    val trackX = size.width - trackW
                    val r = CornerRadius(trackW / 2f)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(trackX, 0f),
                        size = Size(trackW, size.height),
                        cornerRadius = r,
                    )
                    drawRoundRect(
                        color = thumbColor,
                        topLeft = Offset(trackX, thumbTopPx),
                        size = Size(trackW, thumbPx),
                        cornerRadius = r,
                    )
                }

                // Day/month label pill floats to the left of the track while scrolling.
                AnimatedVisibility(
                    visible = isScrolling && currentLabel.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            y = (thumbTopDp + thumbHeightDp / 2 - LABEL_PILL_HEIGHT / 2)
                                .coerceAtLeast(0.dp),
                        ),
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 20.dp, bottomStart = 20.dp,
                            topEnd = 4.dp, bottomEnd = 4.dp,
                        ),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 3.dp,
                        // Offset by touch-width + a small gap so pill clears the thumb
                        modifier = Modifier.padding(end = (SCROLLBAR_TOUCH_WIDTH + 4.dp)),
                    ) {
                        Text(
                            text = currentLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
