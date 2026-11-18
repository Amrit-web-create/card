@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.amrit.mycard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.amrit.mycard.data.PhotoItem

@Composable
fun PhotosScreen(
    photos: List<PhotoItem>,
    onAddPhotos: () -> Unit,
    onCrop: (PhotoItem) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onFullscreen: (Boolean) -> Unit
) {
    var showArchive by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var toDelete by remember { mutableStateOf<PhotoItem?>(null) }

    val active = photos.filter { !it.archived }
    val archived = photos.filter { it.archived }
    val inArchive = showArchive && archived.isNotEmpty()
    val shown = if (inArchive) archived else active

    LaunchedEffect(archived.isEmpty()) {
        if (archived.isEmpty()) showArchive = false
    }
    LaunchedEffect(viewerIndex != null) { onFullscreen(viewerIndex != null) }
    DisposableEffect(Unit) { onDispose { onFullscreen(false) } }
    BackHandler(enabled = viewerIndex != null) { viewerIndex = null }
    BackHandler(enabled = viewerIndex == null && inArchive) { showArchive = false }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (inArchive) {
                SubHeader("Archived photos", "Hidden from your main grid", onBack = { showArchive = false })
            } else {
                ScreenHeader(
                    title = "Photos",
                    subtitle = if (active.isEmpty()) "Nothing saved yet" else "${active.size} saved, encrypted on this device",
                    action = { AddRoundButton(onAddPhotos) }
                )
            }

            if (!inArchive && active.isEmpty() && archived.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(60.dp))
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { PhotoGlyph(MaterialTheme.colorScheme.onSurfaceVariant, 40.dp) }
                    Spacer(Modifier.height(18.dp))
                    Text("No photos yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pick photos from your gallery, crop them and keep them here. Every photo is encrypted and never leaves this phone.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onAddPhotos, shape = RoundedCornerShape(18.dp)) {
                        Text("Add photos", modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 130.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!inArchive && archived.isNotEmpty()) {
                        item(key = "archive_row", span = { GridItemSpan(maxLineSpan) }) {
                            ArchiveRow("Archived", archived.size) { showArchive = true }
                        }
                    }
                    if (shown.isEmpty()) {
                        item(key = "empty_hint", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "No active photos. Tap the plus button to add some.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp)
                            )
                        }
                    }
                    itemsIndexed(shown, key = { _, p -> p.id }) { index, photo ->
                        val img by rememberVaultImage(photo.id, photo.rev, 420)
                        val image = img
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewerIndex = index }
                        ) {
                            if (image != null) {
                                Image(
                                    bitmap = image,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        val vi = viewerIndex
        if (vi != null && shown.isNotEmpty()) {
            PhotoViewer(
                photos = shown,
                startIndex = vi,
                archivedMode = inArchive,
                onClose = { viewerIndex = null },
                onCrop = onCrop,
                onArchive = { id, value ->
                    onArchive(id, value)
                    if (shown.size <= 1) viewerIndex = null
                },
                onAskDelete = { toDelete = it }
            )
        }
    }

    val target = toDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this photo?") },
            text = { Text("The photo will be removed from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    toDelete = null
                    if (shown.size <= 1) viewerIndex = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PhotoViewer(
    photos: List<PhotoItem>,
    startIndex: Int,
    archivedMode: Boolean,
    onClose: () -> Unit,
    onCrop: (PhotoItem) -> Unit,
    onArchive: (String, Boolean) -> Unit,
    onAskDelete: (PhotoItem) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, photos.lastIndex)) { photos.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    val current = photos.getOrNull(pagerState.currentPage)

    LaunchedEffect(pagerState.currentPage) { zoomed = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            key = { photos[it].id },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomableImage(
                photo = photos[page],
                onTap = { chrome = !chrome },
                onZoomChange = { zoomed = it }
            )
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackCircle(onClose)
                Text(
                    "${pagerState.currentPage + 1} of ${photos.size}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = chrome && current != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { current?.let { onCrop(it) } }) { Text("Crop", color = Color.White, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = { current?.let { onArchive(it.id, !archivedMode) } }) {
                    Text(if (archivedMode) "Restore" else "Archive", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { current?.let { onAskDelete(it) } }) {
                    Text("Delete", color = Color(0xFFFF7A8A), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(photo: PhotoItem, onTap: () -> Unit, onZoomChange: (Boolean) -> Unit) {
    val img by rememberVaultImage(photo.id, photo.rev, 2048)
    val image = img
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    val tState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        val maxX = size.width * (scale - 1f) / 2f
        val maxY = size.height * (scale - 1f) / 2f
        offset = if (scale <= 1.01f) {
            Offset.Zero
        } else {
            Offset(
                (offset.x + panChange.x).coerceIn(-maxX, maxX),
                (offset.y + panChange.y).coerceIn(-maxY, maxY)
            )
        }
        onZoomChange(scale > 1.01f)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                            onZoomChange(false)
                        } else {
                            scale = 2.5f
                            onZoomChange(true)
                        }
                    }
                )
            }
            .transformable(state = tState, canPan = { scale > 1.01f }),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }
    }
}
