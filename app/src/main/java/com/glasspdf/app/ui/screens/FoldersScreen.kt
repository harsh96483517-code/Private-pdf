package com.glasspdf.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasspdf.app.data.LayoutMode
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.data.SortField
import com.glasspdf.app.data.title
import com.glasspdf.app.domain.FolderTree
import com.glasspdf.app.domain.sortPdfs
import com.glasspdf.app.ui.components.AddPdfMenu
import com.glasspdf.app.ui.components.DROP_ROOT
import com.glasspdf.app.ui.components.DragDropState
import com.glasspdf.app.ui.components.DragPayload
import com.glasspdf.app.ui.components.FolderAction
import com.glasspdf.app.ui.components.FolderCard
import com.glasspdf.app.ui.components.FolderDialog
import com.glasspdf.app.ui.components.FolderDialogHost
import com.glasspdf.app.ui.components.GRID_COLUMNS
import com.glasspdf.app.ui.components.LayoutSelector
import com.glasspdf.app.ui.components.PdfDialog
import com.glasspdf.app.ui.components.PdfDialogHost
import com.glasspdf.app.ui.components.PdfUi
import com.glasspdf.app.ui.components.ReorderDialog
import com.glasspdf.app.ui.components.ScreenHeader
import com.glasspdf.app.ui.components.SortMenu
import com.glasspdf.app.ui.components.dragSource
import com.glasspdf.app.ui.components.dropTarget
import com.glasspdf.app.ui.components.pdfGalleryItems
import com.glasspdf.app.ui.components.rememberPdfMenu
import com.glasspdf.app.ui.theme.EmptyState
import com.glasspdf.app.ui.theme.GlassIconButton
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.theme.PillShape
import com.glasspdf.app.ui.theme.SectionHeader
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.MainViewModel
import kotlin.math.roundToInt

@Composable
fun FoldersScreen(vm: MainViewModel, onOpenPdf: (Long) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val allPdfs by vm.pdfs.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val pdfs = allPdfs.orEmpty()

    var currentId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(folders, currentId) {
        if (currentId != null && folders.none { it.id == currentId }) currentId = null
    }
    val pending by vm.pendingFolder.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        val target = pending
        if (target != null) {
            currentId = target
            vm.pendingFolder.value = null
        }
    }
    val path = FolderTree.path(folders, currentId)
    BackHandler(currentId != null) { currentId = path.getOrNull(path.size - 2)?.id }

    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    BackHandler(selecting) { selecting = false; selected = emptySet() }

    var folderDialog by remember { mutableStateOf<FolderDialog?>(null) }
    var reordering by remember { mutableStateOf(false) }
    val menu = rememberPdfMenu(vm) { onOpenPdf(it.id) }
    val drag = remember { DragDropState() }

    val subfolders = FolderTree.children(folders, currentId)
    val here: List<PdfEntity> = sortPdfs(pdfs.filter { it.folderId == currentId }, settings)

    val ui = PdfUi(
        settings = settings,
        selected = selected,
        selectionMode = selecting,
        onClick = { p ->
            if (selecting) selected = if (p.id in selected) selected - p.id else selected + p.id
            else onOpenPdf(p.id)
        },
        onLongClick = {},
        menu = menu,
        extra = { p ->
            if (selecting) Modifier
            else Modifier.dragSource(drag, DragPayload(false, p.id, p.title)) { target ->
                vm.move(listOf(p.id), if (target == DROP_ROOT) null else target)
            }
        },
        longPressEnabled = false
    )

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(Modifier.fillMaxSize().onGloballyPositioned { boxOrigin = it.positionInRoot() }) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 10.dp, bottom = bottomInset + 130.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                if (selecting) {
                    Row(
                        Modifier.glass(PillShape, strong = true).padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GlassIconButton(Icons.Rounded.Close, "Done", onClick = { selecting = false; selected = emptySet() }, size = 38.dp)
                        Text("${selected.size} selected", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        GlassIconButton(Icons.Rounded.SelectAll, "Select all", onClick = { selected = here.map { it.id }.toSet() }, size = 38.dp)
                        GlassIconButton(Icons.Rounded.DriveFileMove, "Move", onClick = { if (selected.isNotEmpty()) menu.dialog = PdfDialog.Move(selected.toList()) }, size = 38.dp)
                        GlassIconButton(Icons.Rounded.Delete, "Delete", onClick = { if (selected.isNotEmpty()) menu.dialog = PdfDialog.Delete(selected.toList()) }, size = 38.dp)
                    }
                } else {
                    ScreenHeader("Folders", subtitle = "${folders.size} folder${if (folders.size == 1) "" else "s"} · long-press and drag to organize") {
                        GlassIconButton(Icons.Rounded.CreateNewFolder, "New folder", onClick = { folderDialog = FolderDialog.Create(currentId) })
                        AddPdfMenu(vm)
                    }
                }
            }

            // breadcrumbs (also drop targets: drop here to move up)
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Crumb("Library", Icons.Rounded.Home, DROP_ROOT, currentId == null, drag) { currentId = null }
                    path.forEach { f ->
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Crumb(f.name, null, f.id, f.id == currentId, drag) { currentId = f.id }
                    }
                }
            }

            // sub-folders
            if (subfolders.isNotEmpty()) {
                items(subfolders, key = { "f${it.id}" }, span = { GridItemSpan(3) }) { f ->
                    val inside = FolderTree.pdfsRecursive(folders, pdfs, f.id)
                    FolderCard(
                        folder = f,
                        pdfCount = inside.size,
                        subfolderCount = folders.count { it.parentId == f.id },
                        previews = sortPdfs(inside, settings).take(3),
                        highlighted = drag.hover == f.id,
                        onClick = { currentId = f.id },
                        onAction = { a ->
                            when (a) {
                                FolderAction.OPEN -> currentId = f.id
                                FolderAction.RENAME -> folderDialog = FolderDialog.Rename(f)
                                FolderAction.COVER -> folderDialog = FolderDialog.Cover(f)
                                FolderAction.RESTORE_COVER -> vm.restoreFolderCover(f.id)
                                FolderAction.MOVE -> folderDialog = FolderDialog.Move(f)
                                FolderAction.DELETE -> folderDialog = FolderDialog.Delete(f)
                            }
                        },
                        modifier = Modifier
                            .dropTarget(drag, f.id)
                            .dragSource(drag, DragPayload(true, f.id, f.name)) { target ->
                                vm.moveFolder(f.id, if (target == DROP_ROOT) null else target)
                            }
                    )
                }
            }

            // PDFs in this folder
            item(span = { GridItemSpan(GRID_COLUMNS) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SortMenu(settings, onSort = { f, d -> vm.setSort(f, d) })
                        GlassPill("Select", onClick = { selecting = true }, selected = selecting)
                        if (here.size > 1) GlassPill("Reorder", icon = Icons.Rounded.DragHandle, onClick = { reordering = true })
                    }
                    SectionHeader(
                        title = if (path.isEmpty()) "PDFs not in a folder" else "PDFs in ${path.last().name}",
                        subtitle = "${here.size} PDF${if (here.size == 1) "" else "s"}",
                        trailing = { LayoutSelector(settings.layout, onSelect = { vm.setLayout(it) }) }
                    )
                }
            }
            if (here.isEmpty()) {
                item(span = { GridItemSpan(GRID_COLUMNS) }) {
                    EmptyState(
                        Icons.Rounded.Folder,
                        if (subfolders.isEmpty()) "This folder is empty" else "No PDFs at this level",
                        "Drag a PDF onto a folder, use “Move to folder” from a PDF’s menu, or try Auto Arrange on the Home tab."
                    )
                }
            } else {
                pdfGalleryItems(here, settings.layout, ui, stripTitle = "PDFs")
            }
        }

        // drag ghost
        val dragging = drag.dragging
        if (dragging != null) {
            Box(
                Modifier
                    .offset { IntOffset((drag.position.x - boxOrigin.x).roundToInt() - 60, (drag.position.y - boxOrigin.y).roundToInt() - 130) }
                    .glass(PillShape, strong = true)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(dragging.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }

    PdfDialogHost(menu, vm, folders, onDone = { selected = emptySet(); selecting = false })
    FolderDialogHost(folderDialog, onDismiss = { folderDialog = null }, vm = vm, folders = folders)

    if (reordering) {
        ReorderDialog(
            title = "Reorder PDFs",
            entries = here.map { it.id to it.title },
            onSave = { ids ->
                vm.setManualOrder(ids)
                vm.setSort(SortField.MANUAL, false)
            },
            onDismiss = { reordering = false }
        )
    }
}

@Composable
private fun Crumb(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    dropId: Long,
    current: Boolean,
    drag: DragDropState,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .dropTarget(drag, dropId)
            .then(if (drag.hover == dropId) Modifier.border(2.dp, primary, PillShape) else Modifier)
    ) {
        GlassPill(label, onClick = onClick, icon = icon, selected = current)
    }
}
