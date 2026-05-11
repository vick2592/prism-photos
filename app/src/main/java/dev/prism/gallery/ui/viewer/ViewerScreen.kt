package dev.prism.gallery.ui.viewer

import android.app.Activity
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

    var overlaysVisible by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var currentExif by remember { mutableStateOf(MediaExif()) }
    val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Holds the display name of the item being edited so the result callback doesn't
    // need to reference currentItem (which isn't in scope at launcher definition time).
    var editingDisplayName by remember { mutableStateOf("") }

    // UCrop result launcher
    val editLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val outputUri = UCrop.getOutput(result.data!!)
            if (outputUri != null) {
                val displayName = editingDisplayName
                scope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        EditHelper.saveToMediaStore(context, outputUri, displayName)
                    }
                    EditHelper.cleanupCacheFile(outputUri)
                    Toast.makeText(
                        context,
                        if (saved != null) "Saved to gallery" else "Save failed",
                        Toast.LENGTH_SHORT,
                    ).show()
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
                        onTap = { overlaysVisible = !overlaysVisible },
                        onDismiss = onNavigateBack,
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
}

@Composable
private fun ZoomableImage(
    item: MediaItem,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    val dismissOffset = remember { Animatable(0f) }

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
                                val newScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                                val panChange = event.calculatePan()
                                if (newScale <= 1f) {
                                    scale = 1f; panX = 0f; panY = 0f
                                } else {
                                    scale = newScale
                                    panX += panChange.x
                                    panY += panChange.y
                                }
                                event.changes.forEach { it.consume() }
                            }
                            isMultiTouch -> {
                                // Consume stray events after multi-touch lifts
                                event.changes.forEach { it.consume() }
                            }
                            pressed.size == 1 && scale > 1.05f -> {
                                // Single-touch pan when zoomed — consume to block pager
                                val c = pressed[0]
                                panX += c.position.x - c.previousPosition.x
                                panY += c.position.y - c.previousPosition.y
                                c.consume()
                            }
                            // Single-touch at scale==1: don't consume → pager + vertical drag handle it
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (dismissOffset.value > 200f) {
                                // Fly off screen then navigate back
                                dismissOffset.animateTo(
                                    targetValue = 1400f,
                                    animationSpec = tween(durationMillis = 220),
                                )
                                onDismiss()
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
                    if (scale <= 1.05f && dragAmount > 0f) {
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
                    scaleX = scale
                    scaleY = scale
                    translationX = if (scale > 1f) panX else 0f
                    translationY = if (scale > 1f) panY else dismissOffset.value
                    alpha = if (scale <= 1f) (1f - dismissOffset.value / 800f).coerceIn(0.2f, 1f) else 1f
                },
        )
    }
}
