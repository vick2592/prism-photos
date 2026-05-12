package dev.prism.gallery.ui.viewer

import android.app.WallpaperManager
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlin.math.abs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yalantis.ucrop.UCrop
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    mediaId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToSlideshow: () -> Unit = {},
    viewModel: ViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaId) { viewModel.load(mediaId) }

    if (state.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(
        initialPage = state.startIndex,
        pageCount = { state.items.size },
    )

    // uiVisible is toggled by the user (single tap).
    // isZoomedIn is set automatically when ZoomableImage scale > 1.05.
    // overlaysVisible combines both so they auto-hide on zoom-in too.
    var uiVisible by remember { mutableStateOf(true) }
    var isZoomedIn by remember { mutableStateOf(false) }
    val overlaysVisible = uiVisible && !isZoomedIn
    var showMoreMenu by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf(MediaExif()) }
    val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Captured at the moment the user taps Edit so the launcher callback has the correct values.
    var editingDisplayName by remember { mutableStateOf("") }
    var editingSourceUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var editingDateTaken by remember { mutableStateOf(0L) }

    // After a crop saves, hold the new MediaStore ID here until the updated items list
    // arrives so we can scroll the pager to the crop's actual position.
    var pendingScrollToId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.items, pendingScrollToId) {
        val targetId = pendingScrollToId ?: return@LaunchedEffect
        val idx = state.items.indexOfFirst { it.id == targetId }
        if (idx >= 0) {
            pagerState.scrollToPage(idx)
            pendingScrollToId = null
        }
    }

    // Reset zoom-in flag when the user swipes to a new page.
    LaunchedEffect(pagerState.currentPage) { isZoomedIn = false }

    // UCrop result launcher
    val editLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val outputUri = UCrop.getOutput(result.data!!)
            if (outputUri != null) {
                val displayName = editingDisplayName
                val sourceUri = editingSourceUri
                val dateTaken = editingDateTaken
                scope.launch {
                    val saved = if (sourceUri != null) withContext(Dispatchers.IO) {
                        EditHelper.saveToMediaStore(context, outputUri, displayName, sourceUri, dateTaken)
                    } else null
                    EditHelper.cleanupCacheFile(outputUri)
                    if (saved != null) {
                        // Parse the new item's MediaStore ID from the returned URI and
                        // queue a pager scroll to it. The LaunchedEffect above fires when
                        // the reactive list update arrives and finds the crop in the list.
                        pendingScrollToId = ContentUris.parseId(saved)
                        viewModel.refreshMedia()
                        Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val currentItem = state.items.getOrNull(pagerState.currentPage) ?: return
    val isFavorite = currentItem.id in state.favoriteIds

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val pageOffset = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val fraction = 1f - pageOffset.coerceIn(0f, 1f)
            val item = state.items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = 0.90f + 0.10f * fraction
                        scaleX = s
                        scaleY = s
                        alpha = 0.5f + 0.5f * fraction
                    },
            ) {
                if (item.isVideo && page == pagerState.currentPage) {
                    VideoPlayerScreen(
                        uri = item.uri,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (item.isVideo) {
                    // Thumbnail for adjacent video pages — PlayerView would consume swipes
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ZoomableImage(
                        item = item,
                        onTap = { uiVisible = !uiVisible },
                        onDismiss = onNavigateBack,
                        onZoomStateChange = { zoomed ->
                            if (page == pagerState.currentPage) isZoomedIn = zoomed
                        },
                    )
                }
            }
        }

        // Top bar — back button
        AnimatedVisibility(
            visible = overlaysVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.statusBarsPadding().padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // Top bar — 3-dot overflow menu (auto-hides with overlays and on zoom-in)
        AnimatedVisibility(
            visible = overlaysVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(modifier = Modifier.statusBarsPadding().padding(8.dp)) {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Slideshow") },
                        onClick = { showMoreMenu = false; onNavigateToSlideshow() },
                    )
                    if (!currentItem.isVideo) {
                        DropdownMenuItem(
                            text = { Text("Set as wallpaper") },
                            onClick = {
                                showMoreMenu = false
                                try {
                                    val intent = WallpaperManager.getInstance(context)
                                        .getCropAndSetWallpaperIntent(currentItem.uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot set as wallpaper", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete from device", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMoreMenu = false; showDeleteDialog = true },
                    )
                }
            }
        }

        // Bottom action bar
        AnimatedVisibility(
            visible = overlaysVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
            ) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = currentItem.mimeType
                        putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                }) {
                    Icon(Icons.Filled.Share, "Share", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { viewModel.toggleFavorite(currentItem.id) }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                        tint = if (isFavorite) Color(0xFFE91E63) else Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                if (!currentItem.isVideo) {
                    IconButton(onClick = {
                        editingDisplayName = currentItem.displayName
                        editingSourceUri = currentItem.uri
                        editingDateTaken = currentItem.dateTaken
                        val intent = EditHelper.buildCropIntent(
                            context = context,
                            sourceUri = currentItem.uri,
                            displayName = currentItem.displayName,
                        )
                        editLauncher.launch(intent)
                    }) {
                        Icon(Icons.Filled.Edit, "Edit", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                IconButton(onClick = {
                    scope.launch {
                        currentExif = readExif(
                            context = context,
                            uri = currentItem.uri,
                            sizeBytes = currentItem.size,
                            mimeType = currentItem.mimeType,
                        )
                        showInfoSheet = true
                    }
                }) {
                    Icon(Icons.Filled.Info, "Info", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { showTrashDialog = true }) {
                    Icon(Icons.Filled.Delete, "Delete", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    if (showInfoSheet) {
        MediaInfoSheet(
            exif = currentExif,
            displayName = currentItem.displayName,
            sheetState = infoSheetState,
            onDismiss = { showInfoSheet = false },
        )
    }

    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("Move to trash?") },
            text = { Text("\"${currentItem.displayName}\" will be permanently deleted after 30 days.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.trashItem(currentItem)
                    showTrashDialog = false
                    onNavigateBack()
                }) { Text("Move to Trash") }
            },
            dismissButton = {
                TextButton(onClick = { showTrashDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete permanently?") },
            text = { Text("\"${currentItem.displayName}\" will be removed from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val item = currentItem
                    scope.launch {
                        val ok = viewModel.deleteItemPermanentlyAndWait(item)
                        showDeleteDialog = false
                        if (ok) {
                            Toast.makeText(context, "Deleted from device", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(context, "Could not delete this file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ZoomableImage(
    item: MediaItem,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
    onZoomStateChange: ((Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val panXAnim = remember { Animatable(0f) }
    val panYAnim = remember { Animatable(0f) }
    val dismissOffset = remember { Animatable(0f) }

    // Notify caller whenever zoom crosses the 1.05 threshold so overlays can auto-hide.
    LaunchedEffect(scaleAnim.value > 1.05f) {
        onZoomStateChange?.invoke(scaleAnim.value > 1.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Zoom + pan: only consumes multi-touch or single-touch when zoomed in.
            // Single-touch at scale==1 is NOT consumed → pager receives horizontal swipes.
            .pointerInput(Unit) {
                awaitEachGesture {
                    var isMultiTouch = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        when {
                            pressed.size >= 2 -> {
                                isMultiTouch = true
                                val newScale = (scaleAnim.value * event.calculateZoom()).coerceIn(1f, 5f)
                                val panChange = event.calculatePan()
                                // snapTo is suspend; launch into external scope to exit restricted scope
                                scope.launch {
                                    if (newScale <= 1f) {
                                        scaleAnim.snapTo(1f); panXAnim.snapTo(0f); panYAnim.snapTo(0f)
                                    } else {
                                        scaleAnim.snapTo(newScale)
                                        panXAnim.snapTo(panXAnim.value + panChange.x)
                                        panYAnim.snapTo(panYAnim.value + panChange.y)
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }
                            isMultiTouch -> {
                                // Consume stray events after multi-touch lifts
                                event.changes.forEach { it.consume() }
                            }
                            pressed.size == 1 && scaleAnim.value > 1.05f -> {
                                // Single-touch pan when zoomed — consume to block pager
                                val c = pressed[0]
                                val dx = c.position.x - c.previousPosition.x
                                val dy = c.position.y - c.previousPosition.y
                                scope.launch {
                                    panXAnim.snapTo(panXAnim.value + dx)
                                    panYAnim.snapTo(panYAnim.value + dy)
                                }
                                c.consume()
                            }
                            // Single-touch at scale==1: don't consume → pager + vertical drag handle it
                        }
                    }
                }
            }
            // Tap toggles overlays; double-tap zooms in (2.5×) or resets to 1× if already zoomed.
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scaleAnim.value > 1.5f) {
                            // Zoom out: launch three parallel tweens — each in its own coroutine
                            scope.launch { scaleAnim.animateTo(1f, tween(durationMillis = 280)) }
                            scope.launch { panXAnim.animateTo(0f, tween(durationMillis = 280)) }
                            scope.launch { panYAnim.animateTo(0f, tween(durationMillis = 280)) }
                        } else {
                            // Zoom in with a snappy spring (no bounce)
                            scope.launch {
                                scaleAnim.animateTo(
                                    targetValue = 2.5f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (dismissOffset.value > 200f) {
                                // Navigate back immediately so grid is responsive,
                                // let the fly-off animate briefly behind the transition.
                                onDismiss()
                                dismissOffset.animateTo(
                                    targetValue = 1400f,
                                    animationSpec = tween(durationMillis = 120),
                                )
                            } else {
                                // Spring back into place
                                dismissOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dismissOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    },
                ) { _, dragAmount ->
                    if (scaleAnim.value <= 1.05f && dragAmount > 0f) {
                        scope.launch {
                            dismissOffset.snapTo((dismissOffset.value + dragAmount).coerceAtLeast(0f))
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(300)
                .memoryCacheKey(item.uri.toString())
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    translationX = if (scaleAnim.value > 1f) panXAnim.value else 0f
                    translationY = if (scaleAnim.value > 1f) panYAnim.value else dismissOffset.value
                    alpha = if (scaleAnim.value <= 1f) (1f - dismissOffset.value / 800f).coerceIn(0.2f, 1f) else 1f
                },
        )
    }
}
