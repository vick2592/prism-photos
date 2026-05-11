package dev.prism.gallery.ui.viewer

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.prism.gallery.data.model.MediaItem
import kotlinx.coroutines.launch

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
    var currentExif by remember { mutableStateOf(MediaExif()) }
    val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val currentItem = state.items.getOrNull(pagerState.currentPage) ?: return
    val isFavorite = currentItem.id in state.favoriteIds

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 200f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = state.items[page]
            if (item.isVideo) {
                VideoPlayerScreen(
                    uri = item.uri,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ZoomableImage(
                    item = item,
                    dragOffsetY = if (page == pagerState.currentPage) dragOffsetY else 0f,
                    onTap = { overlaysVisible = !overlaysVisible },
                    onVerticalDrag = { delta ->
                        dragOffsetY += delta
                        if (dragOffsetY < 0f) dragOffsetY = 0f
                    },
                    onVerticalDragEnd = {
                        if (dragOffsetY > dismissThreshold) onNavigateBack()
                        else dragOffsetY = 0f
                    },
                )
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
                IconButton(onClick = { /* Phase 4: trash */ }) {
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
}

@Composable
private fun ZoomableImage(
    item: MediaItem,
    dragOffsetY: Float,
    onTap: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragEnd: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = onVerticalDragEnd,
                    onDragCancel = onVerticalDragEnd,
                ) { _, dragAmount ->
                    if (scale <= 1.05f) onVerticalDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = dragOffsetY
                    alpha = (1f - dragOffsetY / 600f).coerceIn(0.3f, 1f)
                },
        )
    }
}
