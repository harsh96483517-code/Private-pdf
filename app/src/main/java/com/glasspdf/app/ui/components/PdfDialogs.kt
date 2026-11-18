package com.glasspdf.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glasspdf.app.data.FolderEntity
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.data.title
import com.glasspdf.app.domain.FolderTree
import com.glasspdf.app.ui.theme.SwitchRow
import com.glasspdf.app.ui.vm.MainViewModel
import com.glasspdf.app.util.formatDate
import com.glasspdf.app.util.formatSize
import android.net.Uri

sealed interface PdfDialog {
    data class Rename(val pdf: PdfEntity) : PdfDialog
    data class Move(val ids: List<Long>) : PdfDialog
    data class Delete(val ids: List<Long>) : PdfDialog
    data class Info(val pdf: PdfEntity) : PdfDialog
    data class Cover(val pdf: PdfEntity) : PdfDialog
}

@Stable
class PdfMenuController(
    val onOpen: (PdfEntity) -> Unit,
    val onToggleFavorite: (PdfEntity) -> Unit
) {
    var dialog by mutableStateOf<PdfDialog?>(null)
}

@Composable
fun rememberPdfMenu(vm: MainViewModel, onOpen: (PdfEntity) -> Unit): PdfMenuController {
    val open by rememberUpdatedState(onOpen)
    return remember(vm) {
        PdfMenuController(
            onOpen = { open(it) },
            onToggleFavorite = { vm.setFavorite(listOf(it.id), !it.isFavorite) }
        )
    }
}

@Composable
fun PdfDialogHost(menu: PdfMenuController, vm: MainViewModel, folders: List<FolderEntity>, onDone: () -> Unit = {}) {
    // The launcher must stay registered even after the dialog closes (the picker result arrives later).
    var coverTarget by remember { mutableStateOf<Long?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val id = coverTarget
        if (uri != null && id != null) vm.setCover(id, uri)
        coverTarget = null
    }
    val dialog = menu.dialog ?: return
    val close = { menu.dialog = null }

    when (dialog) {
        is PdfDialog.Rename -> {
            var text by remember(dialog.pdf.id) { mutableStateOf(dialog.pdf.title) }
            var alsoFile by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = close,
                shape = RoundedCornerShape(28.dp),
                title = { Text("Rename") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = text, onValueChange = { text = it }, singleLine = true,
                            label = { Text("Title") }, shape = RoundedCornerShape(16.dp)
                        )
                        SwitchRow(
                            "Also rename the file", "Off = only the title shown here changes. The file on your device keeps its name.",
                            alsoFile
                        ) { alsoFile = it }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.rename(dialog.pdf, text, alsoFile); close() }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = close) { Text("Cancel") } }
            )
        }

        is PdfDialog.Move -> MoveDialog(
            title = "Move ${dialog.ids.size} PDF${if (dialog.ids.size == 1) "" else "s"} to…",
            folders = folders,
            exclude = emptySet(),
            onPick = { vm.move(dialog.ids, it); close(); onDone() },
            onNewFolder = { name -> vm.createFolder(name, null, dialog.ids); close(); onDone() },
            onDismiss = close
        )

        is PdfDialog.Delete -> {
            val n = dialog.ids.size
            AlertDialog(
                onDismissRequest = close,
                shape = RoundedCornerShape(28.dp),
                title = { Text("Delete $n PDF${if (n == 1) "" else "s"}?") },
                text = {
                    Text(
                        "“Remove from library” keeps the file on your device. " +
                            "“Delete file” permanently removes it from storage."
                    )
                },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { vm.removeFromLibrary(dialog.ids); close(); onDone() }) { Text("Remove from library") }
                        TextButton(onClick = { vm.deleteFiles(dialog.ids); close(); onDone() }) {
                            Text("Delete file from device", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = close) { Text("Cancel") }
                    }
                }
            )
        }

        is PdfDialog.Info -> {
            val p = dialog.pdf
            val folder = folders.firstOrNull { it.id == p.folderId }?.name ?: "Library (no folder)"
            AlertDialog(
                onDismissRequest = close,
                shape = RoundedCornerShape(28.dp),
                title = { Text("File information") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow("Title", p.title)
                        InfoRow("File name", p.fileName)
                        InfoRow("Size", formatSize(p.sizeBytes))
                        InfoRow("Pages", if (p.pageCount > 0) p.pageCount.toString() else "—")
                        InfoRow("Modified", formatDate(p.modifiedAt))
                        InfoRow("Added", formatDate(p.addedAt))
                        InfoRow("Last opened", formatDate(p.lastOpenedAt))
                        InfoRow("Folder", folder)
                        InfoRow("Embedded title", p.metaTitle ?: "—")
                    }
                },
                confirmButton = { TextButton(onClick = close) { Text("Close") } }
            )
        }

        is PdfDialog.Cover -> {
            val p = dialog.pdf
            AlertDialog(
                onDismissRequest = close,
                shape = RoundedCornerShape(28.dp),
                title = { Text("Cover") },
                text = { Text("Use the first page automatically, or pick your own image from the gallery.") },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { coverTarget = p.id; picker.launch("image/*"); close() }) { Text("Choose from gallery") }
                        if (p.customCoverPath != null) {
                            TextButton(onClick = { vm.restoreCover(p.id); close() }) { Text("Restore original cover") }
                        }
                        TextButton(onClick = close) { Text("Cancel") }
                    }
                }
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Folder picker with indentation for nested folders, plus a "New folder" shortcut. */
@Composable
fun MoveDialog(
    title: String,
    folders: List<FolderEntity>,
    exclude: Set<Long>,
    onPick: (Long?) -> Unit,
    onNewFolder: ((String) -> Unit)?,
    onDismiss: () -> Unit,
    rootLabel: String = "Library (no folder)"
) {
    val flat = remember(folders, exclude) { FolderTree.flatten(folders, exclude) }
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title) },
        text = {
            Column {
                if (onNewFolder != null) {
                    if (creating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newName, onValueChange = { newName = it }, singleLine = true,
                                label = { Text("Folder name") }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            TextButton(onClick = { if (newName.isNotBlank()) onNewFolder(newName) }) { Text("Create") }
                        }
                    } else {
                        MoveRow(Icons.Rounded.CreateNewFolder, "New folder…", 0) { creating = true }
                    }
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    item { MoveRow(Icons.Rounded.Home, rootLabel, 0) { onPick(null) } }
                    items(flat, key = { it.first.id }) { (f, depth) ->
                        MoveRow(Icons.Rounded.Folder, f.name, depth) { onPick(f.id) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MoveRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, depth: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = (12 + depth * 18).dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
