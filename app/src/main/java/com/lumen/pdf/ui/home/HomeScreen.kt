package com.lumen.pdf.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.pdf.data.LibraryLayout
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.ArrangeDialog
import com.lumen.pdf.ui.components.ConfirmDialog
import com.lumen.pdf.ui.components.EmptyState
import com.lumen.pdf.ui.components.GlassIconButton
import com.lumen.pdf.ui.components.GlassPanel
import com.lumen.pdf.ui.components.GlassToggle
import com.lumen.pdf.ui.components.MoveToFolderDialog
import com.lumen.pdf.ui.components.PdfGridCard
import com.lumen.pdf.ui.components.PdfListRow
import com.lumen.pdf.ui.components.PdfStripCard
import com.lumen.pdf.ui.components.PillButton
import com.lumen.pdf.ui.components.SectionHeader
import com.lumen.pdf.ui.icons.LIcon
import com.lumen.pdf.ui.library.rememberLibraryActions
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: LibraryViewModel,
    onOpenPdf: (Long) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onSearch: () -> Unit
) {
    val context = LocalContext.current
    val pdfs by vm.pdfs.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val layout by vm.layout.collectAsStateWithLifecycle()
    val showRecent by vm.showRecentStrip.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val arrangeResult by vm.arrangeResult.collectAsStateWithLifecycle()
    val arrangeBusy by vm.arrangeBusy.collectAsStateWithLifecycle()

    var selection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var moveSelectionOpen by remember { mutableStateOf(false) }
    var deleteSelectionOpen by remember { mutableStateOf(false) }
    var addMenuOpen by remember { mutableStateOf(false) }

    val actions = rememberLibraryActions(
        vm = vm,
        onOpen = { onOpenPdf(it.id) },
        onToggleSelect = { pdf ->
            selection = if (selection.contains(pdf.id)) selection - pdf.id else selection + pdf.id
        }
    )

    val importFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.importTree(uri)
        }
    }
    val importFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        vm.importUris(uris)
    }

    val gridState = rememberLazyGridState()
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    val latestPdfs by rememberUpdatedState(pdfs)

    fun finishDrag() {
        val dragged = draggingId
        if (dragged != null && dragDelta != Offset.Zero) {
            val info = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == dragged }
            if (info != null) {
                val center = Offset(
                    info.offset.x + info.size.width / 2f + dragDelta.x,
                    info.offset.y + info.size.height / 2f + dragDelta.y
                )
                val target = gridState.layoutInfo.visibleItemsInfo
                    .filter { it.key != dragged && it.key is Long }
                    .minByOrNull {
                        abs(it.offset.x + it.size.width / 2f - center.x) +
                            abs(it.offset.y + it.size.height / 2f - center.y)
                    }
                val targetId = target?.key as? Long
                if (targetId != null) {
                    val ids = latestPdfs.map { it.id }.toMutableList()
                    val from = ids.indexOf(dragged)
                    val to = ids.indexOf(targetId)
                    if (from >= 0 && to >= 0) {
                        ids.removeAt(from)
                        ids.add(to, dragged)
                        vm.persistOrder(ids)
                    }
                }
            }
        }
        draggingId = null
        dragDelta = Offset.Zero
    }

    val columns = when (layout) {
        LibraryLayout.GRID3 -> 3
        LibraryLayout.GRID2 -> 2
        else -> 1
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (selection.isEmpty()) {
                HomeTopBar(
                    onSearch = onSearch,
                    onAdd = { addMenuOpen = true },
                    onImportFolder = { importFolder.launch(null) },
                    onImportFiles = { importFiles.launch(arrayOf("application/pdf")) },
                    addMenuOpen = addMenuOpen,
                    closeAddMenu = { addMenuOpen = false }
                )
            } else {
                SelectionTopBar(
                    count = selection.size,
                    onClose = { selection = emptySet() },
                    onMove = { moveSelectionOpen = true },
                    onFavorite = {
                        pdfs.filter { selection.contains(it.id) }.forEach { vm.toggleFavorite(it) }
                    },
                    onDelete = { deleteSelectionOpen = true }
                )
            }

            if (busy) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Scanning PDFs…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GreetingBlock(pdfCount = pdfs.size, folderCount = folders.size)
                }

                if (showRecent && recent.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            SectionHeader("Recently opened")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                lazyRowItems(recent.take(12), key = { it.id }) { pdf ->
                                    PdfStripCard(pdf = pdf, actions = actions)
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "All PDFs",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "${pdfs.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LayoutSwitcher(current = layout, onPick = vm::setLayout)
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (pdfs.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = "Your library is empty",
                            message = "Import a folder or pick PDFs from your device. Everything stays offline on your phone.",
                            icon = LIcon.PictureAsPdf
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PillButton(
                                    text = "Import folder",
                                    icon = LIcon.FolderOpen,
                                    filled = true,
                                    onClick = { importFolder.launch(null) }
                                )
                                PillButton(
                                    text = "Pick files",
                                    icon = LIcon.Add,
                                    onClick = { importFiles.launch(arrayOf("application/pdf")) }
                                )
                            }
                        }
                    }
                } else if (layout == LibraryLayout.STRIP) {
                    val groups = ArrayList<Pair<String, List<com.lumen.pdf.data.PdfFile>>>()
                    if (recent.isNotEmpty()) groups.add("Recently opened" to recent.take(12))
                    val favs = pdfs.filter { it.isFavorite }
                    if (favs.isNotEmpty()) groups.add("Favorites" to favs)
                    folders.forEach { folder ->
                        val inside = pdfs.filter { it.folderId == folder.id }
                        if (inside.isNotEmpty()) groups.add(folder.name to inside)
                    }
                    val root = pdfs.filter { it.folderId == null }
                    if (root.isNotEmpty()) groups.add("Unfiled" to root)

                    groups.forEach { group ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                SectionHeader(group.first, trailing = "${group.second.size}")
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    lazyRowItems(group.second, key = { it.id }) { pdf ->
                                        PdfStripCard(pdf = pdf, cardWidth = 130, actions = actions)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(pdfs, key = { it.id }) { pdf ->
                        val cardModifier = Modifier
                            .graphicsLayer {
                                if (draggingId == pdf.id) {
                                    translationX = dragDelta.x
                                    translationY = dragDelta.y
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                    shadowElevation = 14f
                                }
                            }
                            .pointerInput(pdf.id, layout) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = pdf.id
                                        dragDelta = Offset.Zero
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount
                                    },
                                    onDragEnd = { finishDrag() },
                                    onDragCancel = {
                                        draggingId = null
                                        dragDelta = Offset.Zero
                                    }
                                )
                            }

                        if (layout == LibraryLayout.LIST) {
                            PdfListRow(
                                pdf = pdf,
                                modifier = cardModifier,
                                selected = selection.contains(pdf.id),
                                selectionMode = selection.isNotEmpty(),
                                actions = actions
                            )
                        } else {
                            PdfGridCard(
                                pdf = pdf,
                                modifier = cardModifier,
                                selected = selection.contains(pdf.id),
                                selectionMode = selection.isNotEmpty(),
                                actions = actions
                            )
                        }
                    }
                }
            }
        }

        if (selection.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                PillButton(
                    text = if (arrangeBusy) "Analysing…" else "Auto arrange",
                    icon = LIcon.AutoAwesome,
                    filled = true,
                    onClick = { if (!arrangeBusy) vm.previewArrange() }
                )
            }
        }
    }

    if (moveSelectionOpen) {
        MoveToFolderDialog(
            count = selection.size,
            folders = folders,
            onDismiss = { moveSelectionOpen = false },
            onPick = { folderId ->
                vm.moveToFolder(selection.toList(), folderId)
                selection = emptySet()
            }
        )
    }

    if (deleteSelectionOpen) {
        ConfirmDialog(
            title = "Remove ${selection.size} item(s)?",
            message = "They will be removed from Lumen only. Original files stay on your device.",
            confirmLabel = "Remove",
            onDismiss = { deleteSelectionOpen = false },
            onConfirm = {
                vm.deletePdfs(selection.toList())
                selection = emptySet()
            }
        )
    }

    arrangeResult?.let { result ->
        ArrangeDialog(
            result = result,
            titles = pdfs.associate { it.id to it.title },
            onDismiss = { vm.dismissArrange() },
            onApply = { vm.applyArrange(result) }
        )
    }
}

@Composable
private fun HomeTopBar(
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onImportFolder: () -> Unit,
    onImportFiles: () -> Unit,
    addMenuOpen: Boolean,
    closeAddMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                LIcon.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Lumen PDF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Offline library",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            GlassIconButton(
                icon = LIcon.Add,
                contentDescription = "Import",
                onClick = onAdd
            )
            DropdownMenu(expanded = addMenuOpen, onDismissRequest = closeAddMenu) {
                DropdownMenuItem(
                    text = { Text("Import a folder") },
                    leadingIcon = { Icon(LIcon.FolderOpen, null) },
                    onClick = { closeAddMenu(); onImportFolder() }
                )
                DropdownMenuItem(
                    text = { Text("Pick PDF files") },
                    leadingIcon = { Icon(LIcon.Add, null) },
                    onClick = { closeAddMenu(); onImportFiles() }
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        GlassIconButton(
            icon = LIcon.Search,
            contentDescription = "Search",
            onClick = onSearch
        )
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onMove: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = LIcon.Close,
            contentDescription = "Clear selection",
            onClick = onClose
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        GlassIconButton(
            icon = LIcon.FolderOpen,
            contentDescription = "Move",
            onClick = onMove
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = LIcon.Star,
            contentDescription = "Favorite",
            onClick = onFavorite
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = LIcon.Delete,
            contentDescription = "Delete",
            onClick = onDelete
        )
    }
}

@Composable
private fun GreetingBlock(pdfCount: Int, folderCount: Int) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Text(
            greeting,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Your reading space",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("$pdfCount PDFs")
            StatPill("$folderCount folders")
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LayoutSwitcher(current: LibraryLayout, onPick: (LibraryLayout) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        GlassToggle("2 col", current == LibraryLayout.GRID2, { onPick(LibraryLayout.GRID2) })
        GlassToggle("3 col", current == LibraryLayout.GRID3, { onPick(LibraryLayout.GRID3) })
        GlassToggle("List", current == LibraryLayout.LIST, { onPick(LibraryLayout.LIST) })
        GlassToggle("Strip", current == LibraryLayout.STRIP, { onPick(LibraryLayout.STRIP) })
    }
}
