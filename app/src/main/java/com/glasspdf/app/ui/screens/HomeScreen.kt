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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasspdf.app.data.FilterMode
import com.glasspdf.app.data.FolderEntity
import com.glasspdf.app.data.LayoutMode
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.domain.FolderTree
import com.glasspdf.app.domain.filterPdfs
import com.glasspdf.app.domain.recentPdfs
import com.glasspdf.app.domain.sortPdfs
import com.glasspdf.app.ui.components.AddPdfMenu
import com.glasspdf.app.ui.components.DateFilterMenu
import com.glasspdf.app.ui.components.FilterChips
import com.glasspdf.app.ui.components.GRID_COLUMNS
import com.glasspdf.app.ui.components.LayoutSelector
import com.glasspdf.app.ui.components.PdfDialog
import com.glasspdf.app.ui.components.PdfDialogHost
import com.glasspdf.app.ui.components.PdfStrip
import com.glasspdf.app.ui.components.PdfUi
import com.glasspdf.app.ui.components.SortMenu
import com.glasspdf.app.ui.components.pdfGalleryItems
import com.glasspdf.app.ui.components.rememberFolderPicker
import com.glasspdf.app.ui.components.rememberPdfFilesPicker
import com.glasspdf.app.ui.components.rememberPdfMenu
import com.glasspdf.app.ui.components.stripWidthFor
import com.glasspdf.app.ui.theme.AccentPill
import com.glasspdf.app.ui.theme.EmptyState
import com.glasspdf.app.ui.theme.GlassIconButton
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.theme.SectionHeader
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.theme.PillShape
import com.glasspdf.app.ui.vm.MainViewModel
import java.util.Calendar

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> "Good night"
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        h < 21 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenPdf: (Long) -> Unit,
    onSearch: () -> Unit,
    onArrange: () -> Unit
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val all by vm.pdfs.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val filter by vm.filterMode.collectAsStateWithLifecycle()
    val date by vm.dateFilter.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()

    var selected by remember { mutableStateOf(setOf<Long>()) }
    val selectionMode = selected.isNotEmpty()
    BackHandler(selectionMode) { selected = emptySet() }

    val menu = rememberPdfMenu(vm) { onOpenPdf(it.id) }
    val pickFiles = rememberPdfFilesPicker(vm)
    val pickFolder = rememberFolderPicker(vm)

    val pdfs = all
    val visible: List<PdfEntity> = remember(pdfs, settings, filter, date) {
        val base = filterPdfs(pdfs.orEmpty(), filter, date)
        if (filter == FilterMode.RECENT) base.sortedByDescending { it.lastOpenedAt } else sortPdfs(base, settings)
    }
    val recent = remember(pdfs) { recentPdfs(pdfs.orEmpty(), 12) }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 10.dp, bottom = bottomInset + 170.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ---- header
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                if (selectionMode) {
                    SelectionBar(
                        count = selected.size,
                        onClose = { selected = emptySet() },
                        onSelectAll = { selected = visible.map { it.id }.toSet() },
                        onFavorite = { vm.setFavorite(selected, true); selected = emptySet() },
                        onMove = { menu.dialog = PdfDialog.Move(selected.toList()) },
                        onDelete = { menu.dialog = PdfDialog.Delete(selected.toList()) }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(42.dp).glass(androidx.compose.foundation.shape.CircleShape, strong = true),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                        Text("GlassPDF", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        GlassIconButton(Icons.Rounded.Search, "Search", onClick = onSearch)
                        AddPdfMenu(vm)
                    }
                }
            }

            // ---- greeting
            if (!selectionMode) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Your Library", style = MaterialTheme.typography.headlineLarge)
                        val n = pdfs?.size ?: 0
                        Text(
                            "$n PDF${if (n == 1) "" else "s"} · ${folders.size} folder${if (folders.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- scanning progress
            val p = progress
            if (p != null || importing) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    Column(
                        Modifier.fillMaxWidth().glass(androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (p != null) "Reading PDFs ${p.done + 1}/${p.total}" else "Looking for PDFs…",
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (p != null) {
                            LinearProgressIndicator(progress = { (p.done.toFloat() / p.total.coerceAtLeast(1)) }, modifier = Modifier.fillMaxWidth())
                            Text(p.current, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (pdfs == null) return@LazyVerticalGrid

            if (pdfs.isEmpty()) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    EmptyState(
                        Icons.Rounded.PictureAsPdf,
                        "Your library is empty",
                        "Add some PDFs or pick a folder. Files stay where they are — nothing is copied.",
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlassPill("Add PDFs", onClick = pickFiles, selected = true)
                                GlassPill("Add folder", onClick = pickFolder)
                            }
                        }
                    )
                }
                return@LazyVerticalGrid
            }

            // ---- recently opened (not repeated when the strip layouts already show it)
            val stripLayout = settings.layout == LayoutMode.STRIP || settings.layout == LayoutMode.STRIP_COMPACT
            if (filter == FilterMode.ALL && recent.isNotEmpty() && !selectionMode) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    PdfStrip("Recently Opened", recent, ui, if (stripLayout) stripWidthFor(settings.layout) else 118.dp)
                }
            }

            // ---- controls
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChips(filter) { vm.filterMode.value = it }
                        DateFilterMenu(date) { vm.dateFilter.value = it }
                        SortMenu(settings, onSort = { f, d -> vm.setSort(f, d) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader(
                            title = when (filter) {
                                FilterMode.ALL -> "All PDFs"
                                FilterMode.RECENT -> "Recently opened"
                                FilterMode.FAVORITES -> "Favorites"
                            },
                            subtitle = "${visible.size} shown",
                            modifier = Modifier.weight(1f)
                        )
                        LayoutSelector(settings.layout, onSelect = { vm.setLayout(it) })
                    }
                }
            }

            // ---- gallery
            if (visible.isEmpty()) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    EmptyState(Icons.Rounded.Star, "Nothing here yet", "No PDFs match these filters.")
                }
            } else if (stripLayout) {
                val sections = buildSections(visible, folders)
                sections.forEach { (title, list) ->
                    item(span = { GridItemSpan(GRID_COLUMNS) }) {
                        PdfStrip(title, list, ui, stripWidthFor(settings.layout))
                    }
                }
            } else {
                pdfGalleryItems(visible, settings.layout, ui)
            }
        }

        if (!selectionMode && (pdfs?.isNotEmpty() == true)) {
            AccentPill(
                "Auto Arrange", Icons.Rounded.AutoAwesome, onClick = onArrange,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset + 96.dp)
            )
        }
    }

    PdfDialogHost(menu, vm, folders, onDone = { selected = emptySet() })
}

/** Sections for the strip layouts: Favorites, one per top-level folder (including nested), and unfiled PDFs. */
private fun buildSections(visible: List<PdfEntity>, folders: List<FolderEntity>): List<Pair<String, List<PdfEntity>>> {
    val out = ArrayList<Pair<String, List<PdfEntity>>>()
    val favs = visible.filter { it.isFavorite }
    if (favs.isNotEmpty()) out.add("Favorites" to favs)
    for (f in FolderTree.children(folders, null)) {
        val ids = FolderTree.descendants(folders, f.id) + f.id
        val list = visible.filter { it.folderId != null && it.folderId in ids }
        if (list.isNotEmpty()) out.add(f.name to list)
    }
    val unfiled = visible.filter { it.folderId == null }
    if (unfiled.isNotEmpty()) out.add("Not in a folder" to unfiled)
    return out
}

@Composable
fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onFavorite: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().glass(PillShape, strong = true).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        GlassIconButton(Icons.Rounded.Close, "Cancel selection", onClick = onClose, size = 38.dp)
        Text("$count selected", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
        GlassIconButton(Icons.Rounded.SelectAll, "Select all", onClick = onSelectAll, size = 38.dp)
        GlassIconButton(Icons.Rounded.Star, "Favorite", onClick = onFavorite, size = 38.dp)
        GlassIconButton(Icons.Rounded.DriveFileMove, "Move", onClick = onMove, size = 38.dp)
        GlassIconButton(Icons.Rounded.Delete, "Delete", onClick = onDelete, size = 38.dp)
    }
}
