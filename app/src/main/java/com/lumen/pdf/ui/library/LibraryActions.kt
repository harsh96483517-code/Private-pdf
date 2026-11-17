package com.lumen.pdf.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.pdf.data.PdfFile
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.ConfirmDialog
import com.lumen.pdf.ui.components.InfoDialog
import com.lumen.pdf.ui.components.MoveToFolderDialog
import com.lumen.pdf.ui.components.PdfActions
import com.lumen.pdf.ui.components.TextInputDialog

/**
 * Single place that owns the per PDF dialogs (rename, move, info, delete, cover picker)
 * so every screen gets identical behaviour.
 */
@Composable
fun rememberLibraryActions(
    vm: LibraryViewModel,
    onOpen: (PdfFile) -> Unit,
    onToggleSelect: (PdfFile) -> Unit = {}
): PdfActions {
    var renaming by remember { mutableStateOf<PdfFile?>(null) }
    var moving by remember { mutableStateOf<PdfFile?>(null) }
    var info by remember { mutableStateOf<PdfFile?>(null) }
    var deleting by remember { mutableStateOf<PdfFile?>(null) }
    var coverTarget by remember { mutableStateOf<Long?>(null) }

    val folders by vm.folders.collectAsStateWithLifecycle()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val id = coverTarget
        if (uri != null && id != null) vm.setCustomCover(id, uri)
        coverTarget = null
    }

    renaming?.let { pdf ->
        TextInputDialog(
            title = "Rename PDF",
            initial = pdf.title,
            label = "Displayed title",
            onDismiss = { renaming = null },
            onConfirm = { title ->
                vm.rename(pdf.id, title)
                renaming = null
            }
        )
    }

    moving?.let { pdf ->
        MoveToFolderDialog(
            count = 1,
            folders = folders,
            onDismiss = { moving = null },
            onPick = { folderId -> vm.moveToFolder(listOf(pdf.id), folderId) }
        )
    }

    info?.let { pdf ->
        InfoDialog(
            pdf = pdf,
            folderName = folders.firstOrNull { it.id == pdf.folderId }?.name,
            onDismiss = { info = null }
        )
    }

    deleting?.let { pdf ->
        ConfirmDialog(
            title = "Remove from library?",
            message = "“${pdf.title}” will be removed from Lumen. The original file on your device is never touched.",
            confirmLabel = "Remove",
            onDismiss = { deleting = null },
            onConfirm = { vm.deletePdfs(listOf(pdf.id)) }
        )
    }

    return PdfActions(
        onOpen = onOpen,
        onRename = { renaming = it },
        onMove = { moving = it },
        onDelete = { deleting = it },
        onInfo = { info = it },
        onCover = { pdf ->
            coverTarget = pdf.id
            pickImage.launch("image/*")
        },
        onRestoreCover = { vm.restoreCover(it.id) },
        onFavorite = { vm.toggleFavorite(it) },
        onToggleSelect = onToggleSelect
    )
}
