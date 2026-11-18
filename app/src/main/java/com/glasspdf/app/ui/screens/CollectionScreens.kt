package com.glasspdf.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.domain.recentPdfs
import com.glasspdf.app.domain.sortPdfs
import com.glasspdf.app.ui.components.GRID_COLUMNS
import com.glasspdf.app.ui.components.LayoutSelector
import com.glasspdf.app.ui.components.PdfDialog
import com.glasspdf.app.ui.components.PdfDialogHost
import com.glasspdf.app.ui.components.PdfUi
import com.glasspdf.app.ui.components.ScreenHeader
import com.glasspdf.app.ui.components.SortMenu
import com.glasspdf.app.ui.components.pdfGalleryItems
import com.glasspdf.app.ui.components.rememberPdfMenu
import com.glasspdf.app.ui.theme.EmptyState
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.vm.MainViewModel

@Composable
fun RecentScreen(vm: MainViewModel, onOpenPdf: (Long) -> Unit) {
    val all by vm.pdfs.collectAsStateWithLifecycle()
    val items = remember(all) { recentPdfs(all.orEmpty()) }
    var confirmClear by remember { mutableStateOf(false) }
    PdfCollectionScreen(
        vm = vm,
        title = "Recent",
        subtitle = "${items.size} recently opened",
        items = items,
        keepOrder = true,
        emptyIcon = Icons.Rounded.History,
        emptyTitle = "Nothing opened yet",
        emptyMessage = "PDFs you read show up here so you can jump back in.",
        onOpenPdf = onOpenPdf,
        headerAction = {
            if (items.isNotEmpty()) GlassPill("Clear", onClick = { confirmClear = true })
        }
    )
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Clear recent list?") },
            text = { Text("This only clears the history. Your PDFs and reading positions are kept.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun FavoritesScreen(vm: MainViewModel, onOpenPdf: (Long) -> Unit) {
    val all by vm.pdfs.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val items = remember(all, settings) { sortPdfs(all.orEmpty().filter { it.isFavorite }, settings) }
    PdfCollectionScreen(
        vm = vm,
        title = "Favorites",
        subtitle = "${items.size} favorite${if (items.size == 1) "" else "s"}",
        items = items,
        keepOrder = false,
        emptyIcon = Icons.Rounded.Star,
        emptyTitle = "No favorites yet",
        emptyMessage = "Use the ⋮ menu on any PDF and choose “Add to favorites”.",
        onOpenPdf = onOpenPdf,
        headerAction = {}
    )
}

/** Shared layout-aware list used by Recent and Favorites. */
@Composable
private fun PdfCollectionScreen(
    vm: MainViewModel,
    title: String,
    subtitle: String,
    items: List<PdfEntity>,
    keepOrder: Boolean,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    onOpenPdf: (Long) -> Unit,
    headerAction: @Composable () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val selectionMode = selected.isNotEmpty()
    BackHandler(selectionMode) { selected = emptySet() }
    val menu = rememberPdfMenu(vm) { onOpenPdf(it.id) }

    val ui = PdfUi(
        settings = settings,
        selected = selected,
        selectionMode = selectionMode,
        onClick = { p ->
            if (selectionMode) selected = if (p.id in selected) selected - p.id else selected + p.id
            else onOpenPdf(p.id)
        },
        onLongClick = { p -> selected = selected + p.id },
        menu = menu
    )
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 10.dp, bottom = bottomInset + 130.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                if (selectionMode) {
                    SelectionBar(
                        count = selected.size,
                        onClose = { selected = emptySet() },
                        onSelectAll = { selected = items.map { it.id }.toSet() },
                        onFavorite = { vm.setFavorite(selected, true); selected = emptySet() },
                        onMove = { menu.dialog = PdfDialog.Move(selected.toList()) },
                        onDelete = { menu.dialog = PdfDialog.Delete(selected.toList()) }
                    )
                } else {
                    ScreenHeader(title, subtitle = subtitle) { headerAction() }
                }
            }
            if (items.isNotEmpty()) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            if (!keepOrder) SortMenu(settings, onSort = { f, d -> vm.setSort(f, d) })
                        }
                        LayoutSelector(settings.layout, onSelect = { vm.setLayout(it) })
                    }
                }
                pdfGalleryItems(items, settings.layout, ui, stripTitle = title)
            } else {
                item(span = { GridItemSpan(GRID_COLUMNS) }) { EmptyState(emptyIcon, emptyTitle, emptyMessage) }
            }
        }
    }
    PdfDialogHost(menu, vm, folders, onDone = { selected = emptySet() })
}
