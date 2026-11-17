package com.lumen.pdf.ui.folders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.pdf.data.Folder
import com.lumen.pdf.data.PdfFile
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.ConfirmDialog
import com.lumen.pdf.ui.components.CoverImage
import com.lumen.pdf.ui.components.EmptyState
import com.lumen.pdf.ui.components.GlassIconButton
import com.lumen.pdf.ui.components.MoveToFolderDialog
import com.lumen.pdf.ui.components.PdfListRow
import com.lumen.pdf.ui.components.PillButton
import com.lumen.pdf.ui.components.ScreenHeader
import com.lumen.pdf.ui.components.SectionHeader
import com.lumen.pdf.ui.components.TextInputDialog
import com.lumen.pdf.ui.components.glassBorder
import com.lumen.pdf.ui.components.glassFill
import com.lumen.pdf.ui.icons.LIcon
import com.lumen.pdf.ui.library.rememberLibraryActions

private fun descendantsOf(folders: List<Folder>, rootId: Long): Set<Long> {
    val result = HashSet<Long>()
    val queue = ArrayDeque<Long>()
    queue.add(rootId)
    var guard = 0
    while (queue.isNotEmpty() && guard < 500) {
        guard++
        val current = queue.removeFirst()
        folders.filter { it.parentId == current }.forEach {
            if (result.add(it.id)) queue.addLast(it.id)
        }
    }
    return result
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    vm: LibraryViewModel,
    folderId: Long?,
    onOpenFolder: (Long) -> Unit,
    onOpenPdf: (Long) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val folders by vm.folders.collectAsStateWithLifecycle()
    val pdfs by vm.pdfs.collectAsStateWithLifecycle()

    var createOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Folder?>(null) }
    var deleting by remember { mutableStateOf<Folder?>(null) }
    var movingFolder by remember { mutableStateOf<Folder?>(null) }
    var coverTarget by remember { mutableStateOf<Long?>(null) }

    val actions = rememberLibraryActions(vm, onOpen = { onOpenPdf(it.id) })

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val id = coverTarget
        if (uri != null && id != null) vm.setFolderCover(id, uri)
        coverTarget = null
    }

    val currentFolder = folders.firstOrNull { it.id == folderId }
    val children = folders.filter { it.parentId == folderId }
    val inside = pdfs.filter { it.folderId == folderId }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = currentFolder?.name ?: "Folders",
            subtitle = if (folderId == null) {
                "${folders.size} folders · ${pdfs.count { it.folderId == null }} unfiled"
            } else {
                "${children.size} subfolders · ${inside.size} PDFs"
            },
            onBack = onBack
        ) {
            GlassIconButton(
                icon = LIcon.CreateNewFolder,
                contentDescription = "New folder",
                onClick = { createOpen = true }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (children.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Folders", trailing = "${children.size}")
                }
                items(children, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        count = pdfs.count { it.folderId == folder.id },
                        previews = pdfs.filter { it.folderId == folder.id }.take(3),
                        onClick = { onOpenFolder(folder.id) },
                        onRename = { renaming = folder },
                        onCover = {
                            coverTarget = folder.id
                            pickCover.launch("image/*")
                        },
                        onMove = { movingFolder = folder },
                        onDelete = { deleting = folder }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("PDFs in this folder", trailing = "${inside.size}")
            }

            if (inside.isEmpty() && children.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Nothing here yet",
                        message = "Create a folder or use Auto arrange on the Home screen to group related PDFs automatically.",
                        icon = LIcon.Folder
                    ) {
                        PillButton(
                            text = "New folder",
                            icon = LIcon.CreateNewFolder,
                            filled = true,
                            onClick = { createOpen = true }
                        )
                    }
                }
            }

            if (inside.isEmpty() && children.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No PDFs directly in this folder.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            items(inside, key = { it.id }) { pdf ->
                Box(Modifier.fillMaxWidth()) {
                    PdfListRow(pdf = pdf, actions = actions)
                }
            }
        }
    }

    if (createOpen) {
        TextInputDialog(
            title = "New folder",
            initial = "",
            label = "Folder name",
            confirmLabel = "Create",
            onDismiss = { createOpen = false },
            onConfirm = { name ->
                vm.createFolder(name, folderId)
                createOpen = false
            }
        )
    }

    renaming?.let { folder ->
        TextInputDialog(
            title = "Rename folder",
            initial = folder.name,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                vm.renameFolder(folder.id, name)
                renaming = null
            }
        )
    }

    deleting?.let { folder ->
        ConfirmDialog(
            title = "Delete folder?",
            message = "“${folder.name}” will be deleted. PDFs and subfolders inside are kept and moved one level up.",
            onDismiss = { deleting = null },
            onConfirm = { vm.deleteFolder(folder.id) }
        )
    }

    movingFolder?.let { folder ->
        val blocked = descendantsOf(folders, folder.id) + folder.id
        MoveToFolderDialog(
            count = 1,
            folders = folders.filter { !blocked.contains(it.id) },
            onDismiss = { movingFolder = null },
            onPick = { target ->
                vm.moveFolderInto(folder.id, target)
                movingFolder = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    folder: Folder,
    count: Int,
    previews: List<PdfFile>,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onCover: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Column {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(shape)
                    .background(glassFill(), shape)
                    .border(1.dp, glassBorder(), shape)
                    .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                    .padding(6.dp)
            ) {
                val cover = folder.coverPath ?: previews.firstOrNull()?.coverPath
                if (cover != null) {
                    CoverImage(cover, Modifier.fillMaxSize(), corner = 17)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color.Black.copy(alpha = 0.28f))
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            LIcon.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$count PDFs",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        LIcon.MoreVert,
                        contentDescription = "Folder menu",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Open") },
                    leadingIcon = { Icon(LIcon.FolderOpen, null) },
                    onClick = { menuOpen = false; onClick() }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(LIcon.Rename, null) },
                    onClick = { menuOpen = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("Set cover from gallery") },
                    leadingIcon = { Icon(LIcon.Image, null) },
                    onClick = { menuOpen = false; onCover() }
                )
                DropdownMenuItem(
                    text = { Text("Move inside another folder") },
                    leadingIcon = { Icon(LIcon.FolderOpen, null) },
                    onClick = { menuOpen = false; onMove() }
                )
                DropdownMenuItem(
                    text = { Text("Delete folder") },
                    leadingIcon = { Icon(LIcon.Delete, null) },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
        if (previews.size > 1) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                previews.take(3).forEach { pdf ->
                    CoverImage(
                        pdf.coverPath,
                        Modifier
                            .width(22.dp)
                            .height(28.dp),
                        corner = 6
                    )
                }
            }
        }
    }
}
