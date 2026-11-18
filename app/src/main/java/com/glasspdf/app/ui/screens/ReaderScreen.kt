package com.glasspdf.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass as PointerPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasspdf.app.data.title
import com.glasspdf.app.ui.components.MenuEntry
import com.glasspdf.app.ui.theme.EmptyState
import com.glasspdf.app.ui.theme.GlassIconButton
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.theme.PillShape
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.ReaderLoad
import com.glasspdf.app.ui.vm.ReaderViewModel
import com.glasspdf.app.util.PdfDocSource
import com.glasspdf.app.util.ShareUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ReaderSheet { THUMBS, SEARCH, BOOKMARKS }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ReaderScreen(onBack: () -> Unit, vm: ReaderViewModel = viewModel()) {
    val load by vm.load.collectAsStateWithLifecycle()
    when (val l = load) {
        ReaderLoad.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is ReaderLoad.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                Icons.Rounded.Warning, "Can’t open this PDF", l.message,
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassPill("Back", onClick = onBack, selected = true)
                        GlassPill("Remove from library", onClick = { vm.removeFromLibrary(); onBack() })
                    }
                }
            )
        }
        is ReaderLoad.Ready -> ReaderContent(vm, l.source, l.startPage, onBack)
    }
}

private fun nearestPage(state: LazyListState): Int? {
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return null
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return visible.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
}

/** Two-finger pinch is consumed here before the list can scroll; one finger is left alone. */
private fun Modifier.pinchZoom(onZoom: (Float) -> Unit, onEnd: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var pinched = false
            do {
                val event = awaitPointerEvent(PointerPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    val z = event.calculateZoom()
                    if (z != 1f) {
                        pinched = true
                        onZoom(z)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
            } while (event.changes.any { it.pressed })
            if (pinched) onEnd()
        }
    }

@Composable
private fun ReaderContent(vm: ReaderViewModel, source: PdfDocSource, startPage: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pdf by vm.pdf.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val count = source.pageCount

    var vertical by rememberSaveable { mutableStateOf(true) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var renderZoom by remember { mutableFloatStateOf(1f) }
    var controls by remember { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(startPage) }
    var sheet by remember { mutableStateOf<ReaderSheet?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var sliderDrag by remember { mutableStateOf<Float?>(null) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startPage)
    val pagerState = rememberPagerState(initialPage = startPage) { count }

    // keep the page indicator in sync with whichever mode is active
    LaunchedEffect(listState) {
        snapshotFlow { nearestPage(listState) }.collect { p -> if (vertical && p != null) currentPage = p }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { p -> if (!vertical) currentPage = p }
    }
    LaunchedEffect(vertical) {
        if (vertical) listState.scrollToItem(currentPage) else pagerState.scrollToPage(currentPage)
    }

    // remember reading position
    val latestPage by rememberUpdatedState(currentPage)
    LaunchedEffect(currentPage) {
        delay(700)
        vm.saveLastPage(currentPage)
    }
    DisposableEffect(Unit) { onDispose { vm.saveLastPage(latestPage) } }

    // full-screen reading mode
    DisposableEffect(fullscreen) {
        val window = view.context.findActivity()?.window
        val controller: WindowInsetsControllerCompat? = window?.let { WindowCompat.getInsetsController(it, view) }
        if (fullscreen) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    BackHandler(fullscreen) { fullscreen = false }

    fun goTo(page: Int) {
        val p = page.coerceIn(0, count - 1)
        currentPage = p
        scope.launch {
            if (vertical) listState.scrollToItem(p) else pagerState.scrollToPage(p)
        }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bookmarked = bookmarks.any { it.page == currentPage }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenWpx = with(LocalDensity.current) { maxWidth.toPx() }
        val renderW = (screenWpx * renderZoom).toInt().coerceIn(300, 2400)
        val topPad = topInset + 76.dp
        val bottomPad = bottomInset + 120.dp

        Box(
            Modifier
                .fillMaxSize()
                .pinchZoom(
                    onZoom = { f -> zoom = (zoom * f).coerceIn(1f, 3.5f) },
                    onEnd = { renderZoom = zoom }
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { controls = !controls },
                        onDoubleTap = {
                            val z = if (zoom > 1.05f) 1f else 2f
                            zoom = z
                            renderZoom = z
                        }
                    )
                }
        ) {
            if (vertical) {
                Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.width(screenW * zoom).fillMaxHeight(),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = topPad, bottom = bottomPad),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(count, key = { it }) { i -> PdfPageView(source, i, renderW, fit = false) }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = zoom <= 1.02f,
                    pageSpacing = 12.dp,
                    key = { it },
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (zoom <= 1.02f) {
                        PdfPageView(
                            source, page, renderW, fit = true,
                            modifier = Modifier.fillMaxSize().padding(top = topInset + 64.dp, bottom = bottomInset + 112.dp, start = 8.dp, end = 8.dp)
                        )
                    } else {
                        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                            PdfPageView(source, page, renderW, fit = false, modifier = Modifier.width(screenW * zoom).padding(vertical = topPad))
                        }
                    }
                }
            }
        }

        // ---- top controls
        AnimatedVisibility(
            visible = controls,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = topInset + 8.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
                Box(
                    Modifier.weight(1f).height(42.dp).glass(PillShape, strong = true).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(pdf?.title ?: "", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                GlassIconButton(Icons.Rounded.Search, "Search in PDF", onClick = { sheet = ReaderSheet.SEARCH })
                GlassIconButton(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, "Bookmark this page",
                    onClick = { vm.toggleBookmark(currentPage) }, selected = bookmarked
                )
                Box {
                    GlassIconButton(Icons.Rounded.MoreVert, "More", onClick = { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(20.dp)) {
                        MenuEntry("Share", Icons.Rounded.Share) {
                            menuOpen = false
                            val p = pdf
                            if (p != null) scope.launch {
                                if (!ShareUtil.sharePdf(context, Uri.parse(p.uri), p.fileName)) {
                                    Toast.makeText(context, "Couldn’t share this file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        MenuEntry("Open with another app", Icons.AutoMirrored.Rounded.OpenInNew) {
                            menuOpen = false
                            val p = pdf
                            if (p != null && !ShareUtil.openWithExternal(context, Uri.parse(p.uri))) {
                                Toast.makeText(context, "No app found to open PDFs", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MenuEntry("Bookmarks", Icons.Rounded.Bookmark) { menuOpen = false; sheet = ReaderSheet.BOOKMARKS }
                        MenuEntry("Page thumbnails", Icons.Rounded.GridView) { menuOpen = false; sheet = ReaderSheet.THUMBS }
                        MenuEntry(
                            if (fullscreen) "Exit full screen" else "Full screen",
                            if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen
                        ) { menuOpen = false; fullscreen = !fullscreen; if (fullscreen) controls = false }
                    }
                }
            }
        }

        // ---- bottom controls
        AnimatedVisibility(
            visible = controls,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + 10.dp)
                    .glass(RoundedCornerShape(28.dp), strong = true)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val shown = (sliderDrag?.roundToInt() ?: currentPage) + 1
                    Text("$shown / $count", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    GlassIconButton(Icons.Rounded.ZoomOut, "Zoom out", onClick = {
                        zoom = (zoom - 0.5f).coerceAtLeast(1f); renderZoom = zoom
                    }, size = 38.dp)
                    GlassIconButton(Icons.Rounded.ZoomIn, "Zoom in", onClick = {
                        zoom = (zoom + 0.5f).coerceAtMost(3.5f); renderZoom = zoom
                    }, size = 38.dp)
                    GlassIconButton(
                        if (vertical) Icons.Rounded.SwapHoriz else Icons.Rounded.SwapVert,
                        if (vertical) "Switch to horizontal pages" else "Switch to vertical scrolling",
                        onClick = { vertical = !vertical; zoom = 1f; renderZoom = 1f }, size = 38.dp
                    )
                    GlassIconButton(Icons.Rounded.GridView, "Thumbnails", onClick = { sheet = ReaderSheet.THUMBS }, size = 38.dp)
                    GlassIconButton(
                        if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, "Full screen",
                        onClick = { fullscreen = !fullscreen; if (fullscreen) controls = false }, size = 38.dp
                    )
                }
                if (count > 1) {
                    Slider(
                        value = sliderDrag ?: currentPage.toFloat(),
                        onValueChange = { sliderDrag = it },
                        onValueChangeFinished = {
                            sliderDrag?.let { goTo(it.roundToInt()) }
                            sliderDrag = null
                        },
                        valueRange = 0f..(count - 1).toFloat()
                    )
                }
            }
        }

        if (!controls) {
            Text(
                "${currentPage + 1} / $count",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset + 14.dp)
                    .glass(PillShape, strong = true)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }

    val activeSheet = sheet
    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            when (activeSheet) {
                ReaderSheet.THUMBS -> ThumbnailsSheet(source, currentPage) { goTo(it); sheet = null }
                ReaderSheet.SEARCH -> SearchSheet(vm) { goTo(it); sheet = null }
                ReaderSheet.BOOKMARKS -> Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                    Text("Bookmarks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                    if (bookmarks.isEmpty()) {
                        Text("Tap the bookmark icon at the top to save a page.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
                        items(bookmarks, key = { it.id }) { b ->
                            Row(
                                Modifier.fillMaxWidth().clickable { goTo(b.page); sheet = null }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Page ${b.page + 1}", modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleSmall)
                                Icon(
                                    Icons.Rounded.Delete, "Delete bookmark", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { vm.deleteBookmark(b.id) }.padding(6.dp)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageView(source: PdfDocSource, index: Int, renderWidthPx: Int, fit: Boolean, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(source.cached(index, renderWidthPx)?.asImageBitmap(), source, index, renderWidthPx) {
        val b = source.cached(index, renderWidthPx) ?: source.render(index, renderWidthPx)
        if (b != null) value = b.asImageBitmap()
    }
    val bmp = bitmap
    if (fit) {
        Box(modifier, contentAlignment = Alignment.Center) {
            if (bmp != null) {
                Image(
                    bitmap = bmp, contentDescription = "Page ${index + 1}", contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator()
            }
        }
    } else {
        val ratio = if (bmp != null) bmp.height.toFloat() / bmp.width.toFloat() else (source.aspect(index) ?: source.aspect(0) ?: 1.414f)
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier
                .fillMaxWidth()
                .aspectRatio(1f / ratio)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp, contentDescription = "Page ${index + 1}", contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("${index + 1}", color = Color(0xFFB0B0B8), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun ThumbnailsSheet(source: PdfDocSource, current: Int, onPick: (Int) -> Unit) {
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (current / 3) * 3)
    val primary = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("Pages", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = state,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(source.pageCount, key = { it }) { i ->
                val thumb by produceState<ImageBitmap?>(null, i) { value = source.render(i, 260)?.asImageBitmap() }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onPick(i) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .then(if (i == current) Modifier.border(3.dp, primary, RoundedCornerShape(8.dp)) else Modifier)
                    ) {
                        val t = thumb
                        if (t != null) {
                            Image(bitmap = t, contentDescription = null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchSheet(vm: ReaderViewModel, onPick: (Int) -> Unit) {
    val ui by vm.search.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf(ui.query) }
    LaunchedEffect(query) {
        delay(400)
        if (query.trim() != vm.search.value.query) vm.search(query)
    }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.75f).padding(horizontal = 16.dp)) {
        Text("Search in PDF", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("Type at least 2 letters") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            shape = PillShape, modifier = Modifier.fillMaxWidth()
        )
        val status = when {
            ui.error != null -> ui.error
            ui.running -> "Searching… ${ui.hits.size} found so far"
            ui.finished && ui.hits.isEmpty() -> "No matches. If this PDF is a scan, it has no searchable text."
            ui.finished -> "${ui.hits.size} match${if (ui.hits.size == 1) "" else "es"}"
            else -> null
        }
        if (status != null) {
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(ui.hits) { h ->
                Row(Modifier.fillMaxWidth().clickable { onPick(h.page) }.padding(vertical = 10.dp)) {
                    Text(
                        "p. ${h.page + 1}", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.width(56.dp)
                    )
                    Text(h.snippet, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
