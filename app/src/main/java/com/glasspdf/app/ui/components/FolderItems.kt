package com.glasspdf.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.glasspdf.app.data.FolderEntity
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.domain.FolderTree
import com.glasspdf.app.ui.theme.appear
import com.glasspdf.app.ui.theme.bouncyClick
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.MainViewModel

sealed interface FolderDialog {
    data class Create(val parentId: Long?) : FolderDialog
    data class Rename(val folder: FolderEntity) : FolderDialog
    data class Move(val folder: FolderEntity) : FolderDialog
    data class Delete(val folder: FolderEntity) : FolderDialog
    data class Cover(val folder: FolderEntity) : FolderDialog
}

enum class FolderAction { OPEN, RENAME, COVER, RESTORE_COVER, MOVE, DELETE }

/** Large folder tile: custom cover or an automatic fan of the PDFs inside, with name and counts. */
@Composable
fun FolderCard(
    folder: FolderEntity,
    pdfCount: Int,
    subfolderCount: Int,
    previews: List<PdfEntity>,
    highlighted: Boolean,
    onClick: () -> Unit,
    onAction: (FolderAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier.appear()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.08f)
                .bouncyClick(onClick = onClick)
        ) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.40f)
                    .height(18.dp)
                    .glass(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp), strong = true)
            )
            val bodyShape = RoundedCornerShape(topStart = 4.dp, topEnd = 26.dp, bottomStart = 26.dp, bottomEnd = 26.dp)
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxSize()
                    .glass(bodyShape, strong = true)
                    .then(if (highlighted) Modifier.border(3.dp, primary, bodyShape) else Modifier)
            ) {
                val custom = folder.customCoverPath
                if (custom != null) {
                    PathImage(custom, Modifier.fillMaxSize().clip(bodyShape)) { FolderFan(previews) }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f))))
                    )
                } else {
                    FolderFan(previews)
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 6.dp)) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.MoreVert, "Folder menu", tint = Color.White, modifier = Modifier.size(18.dp)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = RoundedCornerShape(20.dp)) {
                    MenuEntry("Open", Icons.Rounded.FolderOpen) { menuOpen = false; onAction(FolderAction.OPEN) }
                    MenuEntry("Rename", Icons.Rounded.Edit) { menuOpen = false; onAction(FolderAction.RENAME) }
                    MenuEntry("Change cover", Icons.Rounded.Image) { menuOpen = false; onAction(FolderAction.COVER) }
                    if (folder.customCoverPath != null) {
                        MenuEntry("Use automatic cover", Icons.Rounded.Restore) { menuOpen = false; onAction(FolderAction.RESTORE_COVER) }
                    }
                    MenuEntry("Move to…", Icons.Rounded.DriveFileMove) { menuOpen = false; onAction(FolderAction.MOVE) }
                    MenuEntry("Delete folder", Icons.Rounded.Delete, danger = true) { menuOpen = false; onAction(FolderAction.DELETE) }
                }
            }
        }
        Text(
            folder.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        val parts = ArrayList<String>()
        parts.add("$pdfCount PDF${if (pdfCount == 1) "" else "s"}")
        if (subfolderCount > 0) parts.add("$subfolderCount folder${if (subfolderCount == 1) "" else "s"}")
        Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Up to three PDF covers fanned out inside the folder (automatic folder cover). */
@Composable
private fun FolderFan(previews: List<PdfEntity>) {
    if (previews.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(54.dp))
        }
        return
    }
    val shown = previews.take(3)
    val angles = when (shown.size) {
        1 -> listOf(0f)
        2 -> listOf(-7f, 6f)
        else -> listOf(-10f, 0f, 10f)
    }
    val shifts = when (shown.size) {
        1 -> listOf(0)
        2 -> listOf(-22, 22)
        else -> listOf(-34, 0, 34)
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
        shown.forEachIndexed { i, p ->
            PdfCoverImage(
                p,
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(COVER_RATIO)
                    .offset(x = shifts[i].dp)
                    .graphicsLayer { rotationZ = angles[i] }
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun FolderDialogHost(dialog: FolderDialog?, onDismiss: () -> Unit, vm: MainViewModel, folders: List<FolderEntity>, onCreated: (Long?) -> Unit = {}) {
    var coverTarget by remember { mutableStateOf<Long?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val id = coverTarget
        if (uri != null && id != null) vm.setFolderCover(id, uri)
        coverTarget = null
    }
    when (dialog) {
        null -> Unit
        is FolderDialog.Create -> {
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(28.dp),
                title = { Text("New folder") },
                text = {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text("Folder name") }, shape = RoundedCornerShape(16.dp)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { vm.createFolder(name, dialog.parentId); onDismiss() }, enabled = name.isNotBlank()) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        is FolderDialog.Rename -> {
            var name by remember(dialog.folder.id) { mutableStateOf(dialog.folder.name) }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(28.dp),
                title = { Text("Rename folder") },
                text = {
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, shape = RoundedCornerShape(16.dp))
                },
                confirmButton = {
                    TextButton(onClick = { vm.renameFolder(dialog.folder.id, name); onDismiss() }, enabled = name.isNotBlank()) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        is FolderDialog.Move -> {
            val exclude = remember(folders, dialog.folder.id) { FolderTree.descendants(folders, dialog.folder.id) + dialog.folder.id }
            MoveDialog(
                title = "Move “${dialog.folder.name}” to…",
                folders = folders,
                exclude = exclude,
                onPick = { vm.moveFolder(dialog.folder.id, it); onDismiss() },
                onNewFolder = null,
                onDismiss = onDismiss,
                rootLabel = "Library (top level)"
            )
        }
        is FolderDialog.Delete -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete “${dialog.folder.name}”?") },
            text = { Text("The PDFs and folders inside move up one level. No PDF file is deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteFolder(dialog.folder.id); onDismiss() }) {
                    Text("Delete folder", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
        is FolderDialog.Cover -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Folder cover") },
            text = { Text("Pick a photo from your gallery, or let the folder show the PDFs inside it.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { coverTarget = dialog.folder.id; picker.launch("image/*"); onDismiss() }) { Text("Choose from gallery") }
                    if (dialog.folder.customCoverPath != null) {
                        TextButton(onClick = { vm.restoreFolderCover(dialog.folder.id); onDismiss() }) { Text("Use automatic cover") }
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        )
    }
}

/** Drag the handles to put PDFs into a custom order. */
@Composable
fun ReorderDialog(
    title: String,
    entries: List<Pair<Long, String>>,
    onSave: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val order = remember { mutableStateListOf<Pair<Long, String>>().also { it.addAll(entries) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ReorderableColumn(
                    items = order.toList(),
                    key = { it.first },
                    rowHeight = 56.dp,
                    onMove = { from, to ->
                        val item = order.removeAt(from)
                        order.add(to, item)
                    }
                ) { entry, index, handle ->
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(22.dp))
                        Text(entry.second, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.DragHandle, "Drag to reorder", modifier = handle.size(32.dp).padding(4.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(order.map { it.first }); onDismiss() }) { Text("Save order") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
