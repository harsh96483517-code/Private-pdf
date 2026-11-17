package com.lumen.pdf.ui.reader

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.GlassIconButton
import com.lumen.pdf.ui.components.GlassPanel
import com.lumen.pdf.ui.icons.LIcon
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Holds one open PdfRenderer for the reading session. All render calls go through a
 * single thread because PdfRenderer is not thread safe.
 */
class PdfSession(context: Context, private val uri: Uri) {

    private val appContext = context.applicationContext
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null
    private val cache = LruCache<String, Bitmap>(10)

    var pageCount: Int = 0
        private set

    suspend fun open(): Boolean = withContext(dispatcher) {
        try {
            val pfd = appContext.contentResolver.openFileDescriptor(uri, "r")
            if (pfd == null) {
                false
            } else {
                descriptor = pfd
                val pdfRenderer = PdfRenderer(pfd)
                renderer = pdfRenderer
                pageCount = pdfRenderer.pageCount
                true
            }
        } catch (t: Throwable) {
            false
        }
    }

    suspend fun render(page: Int, width: Int): Bitmap? = withContext(dispatcher) {
        val key = "$page-$width"
        cache.get(key)?.let { return@withContext it }
        val pdfRenderer = renderer ?: return@withContext null
        if (page < 0 || page >= pdfRenderer.pageCount) return@withContext null
        var pdfPage: PdfRenderer.Page? = null
        try {
            pdfPage = pdfRenderer.openPage(page)
            val targetWidth = width.coerceIn(180, 2400)
            val targetHeight = (targetWidth.toFloat() * pdfPage.height.toFloat() / pdfPage.width.toFloat())
                .toInt().coerceIn(1, 5000)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            cache.put(key, bitmap)
            bitmap
        } catch (t: Throwable) {
            null
        } finally {
            try { pdfPage?.close() } catch (_: Throwable) { }
        }
    }

    fun close() {
        try { renderer?.close() } catch (_: Throwable) { }
        try { descriptor?.close() } catch (_: Throwable) { }
        renderer = null
        descriptor = null
        cache.evictAll()
        try { dispatcher.close() } catch (_: Throwable) { }
    }
}

@Composable
fun ReaderScreen(vm: LibraryViewModel, pdfId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val pdfs by vm.pdfs.collectAsStateWithLifecycle()
    val pdf = pdfs.firstOrNull { it.id == pdfId }

    if (pdf == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val uri = remember(pdf.uri) { Uri.parse(pdf.uri) }
    val session = remember(pdf.id, pdf.uri) { PdfSession(context, uri) }
    var opened by remember(pdf.id) { mutableStateOf(false) }
    var pageCount by remember(pdf.id) { mutableStateOf(0) }

    LaunchedEffect(session) {
        opened = session.open()
        pageCount = session.pageCount
    }
    DisposableEffect(session) {
        onDispose { session.close() }
    }

    val bookmarksFlow = remember(pdfId) { vm.bookmarks(pdfId) }
    val bookmarks by bookmarksFlow.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(0) }
    var scale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var fullscreen by remember { mutableStateOf(false) }
    var showThumbnails by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }

    LaunchedEffect(opened, pageCount) {
        if (opened && pageCount > 0 && pdf.lastPage > 0) {
            listState.scrollToItem(pdf.lastPage.coerceIn(0, pageCount - 1))
        }
    }

    LaunchedEffect(opened, pageCount) {
        if (!opened) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }.collectLatest { index ->
            currentPage = index
            delay(1200)
            vm.updateProgress(pdfId, index)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        if (!opened) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Opening PDF…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val widthPx = with(LocalDensity.current) { maxWidth.toPx().toInt() }
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                    panOffset = if (scale > 1.02f) panOffset + panChange else Offset.Zero
                }
                Box(Modifier.fillMaxSize().transformable(transformState)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = scale <= 1.02f
                    ) {
                        items(pageCount) { index ->
                            PageItem(session = session, index = index, widthPx = widthPx)
                        }
                    }
                }
            }
        }

        if (!fullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = LIcon.Close,
                    contentDescription = "Close",
                    onClick = onBack
                )
                Spacer(Modifier.width(10.dp))
                GlassPanel(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        pdf.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (pageCount > 0) "Page ${currentPage + 1} of $pageCount" else "Loading…",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(10.dp))
                GlassIconButton(
                    icon = if (bookmarks.any { it.page == currentPage + 1 }) LIcon.Bookmark
                    else LIcon.BookmarkBorder,
                    contentDescription = "Bookmark page",
                    onClick = { vm.toggleBookmark(pdfId, currentPage + 1) }
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    GlassIconButton(
                        icon = LIcon.MoreVert,
                        contentDescription = "More",
                        onClick = { moreOpen = true }
                    )
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Search inside PDF") },
                            leadingIcon = { Icon(LIcon.Search, null) },
                            onClick = { moreOpen = false; showSearch = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Page thumbnails") },
                            leadingIcon = { Icon(LIcon.GridView, null) },
                            onClick = { moreOpen = false; showThumbnails = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            leadingIcon = { Icon(LIcon.Bookmark, null) },
                            onClick = { moreOpen = false; showBookmarks = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Full screen reading") },
                            leadingIcon = { Icon(LIcon.Fullscreen, null) },
                            onClick = { moreOpen = false; fullscreen = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Share PDF") },
                            leadingIcon = { Icon(LIcon.Share, null) },
                            onClick = {
                                moreOpen = false
                                runCatching {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open with another app") },
                            leadingIcon = { Icon(LIcon.OpenInNew, null) },
                            onClick = {
                                moreOpen = false
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassIconButton(
                icon = LIcon.ZoomOut,
                contentDescription = "Zoom out",
                onClick = {
                    scale = (scale - 0.5f).coerceIn(1f, 5f)
                    if (scale <= 1.02f) panOffset = Offset.Zero
                }
            )
            GlassIconButton(
                icon = LIcon.ZoomIn,
                contentDescription = "Zoom in",
                onClick = { scale = (scale + 0.5f).coerceIn(1f, 5f) }
            )
            GlassPanel(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (fullscreen) "Full screen" else "Page ${currentPage + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            GlassIconButton(
                icon = LIcon.GridView,
                contentDescription = "Thumbnails",
                onClick = { showThumbnails = true }
            )
            if (fullscreen) {
                GlassIconButton(
                    icon = LIcon.FullscreenExit,
                    contentDescription = "Exit full screen",
                    onClick = { fullscreen = false }
                )
            }
        }
    }

    if (showThumbnails) {
        Dialog(onDismissRequest = { showThumbnails = false }) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    "Pages",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pageCount) { index ->
                        Column(
                            Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                                showThumbnails = false
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        ) {
                            PageThumb(session = session, index = index)
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBookmarks) {
        Dialog(onDismissRequest = { showBookmarks = false }) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    "Bookmarks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                if (bookmarks.isEmpty()) {
                    Text(
                        "No bookmarks yet. Tap the bookmark icon while reading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showBookmarks = false
                                        scope.launch {
                                            listState.animateScrollToItem((bookmark.page - 1).coerceAtLeast(0))
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    LIcon.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Page ${bookmark.page}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearch) {
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Int>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }

        LaunchedEffect(query) {
            if (query.trim().length >= 2) {
                delay(450)
                searching = true
                results = runCatching { vm.repo.searchInPdf(pdfId, query.trim()) }.getOrDefault(emptyList())
                searching = false
            } else {
                results = emptyList()
            }
        }

        Dialog(onDismissRequest = { showSearch = false }) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Search inside PDF",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    GlassIconButton(
                        icon = LIcon.Close,
                        contentDescription = "Close",
                        onClick = { showSearch = false },
                        size = 34
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Word or phrase") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
                Spacer(Modifier.height(8.dp))
                if (searching) {
                    Text(
                        "Searching…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (results.isEmpty() && query.trim().length >= 2) {
                    Text(
                        "No matches in the first pages.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "${results.size} pages contain “$query”",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(results) { page ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showSearch = false
                                    scope.launch {
                                        listState.animateScrollToItem((page - 1).coerceAtLeast(0))
                                    }
                                }
                                .padding(vertical = 9.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                LIcon.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Page $page",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageItem(session: PdfSession, index: Int, widthPx: Int) {
    var bitmap by remember(index, widthPx) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(index, widthPx) {
        bitmap = session.render(index, widthPx)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(androidx.compose.ui.graphics.Color.White),
        contentAlignment = Alignment.Center
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun PageThumb(session: PdfSession, index: Int) {
    var bitmap by remember(index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(index) {
        bitmap = session.render(index, 200)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color.White),
        contentAlignment = Alignment.Center
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(strokeWidth = 1.5.dp, modifier = Modifier.size(14.dp))
        }
    }
}
