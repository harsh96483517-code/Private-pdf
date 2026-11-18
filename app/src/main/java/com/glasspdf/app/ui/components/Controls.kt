package com.glasspdf.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.glasspdf.app.data.AppSettings
import com.glasspdf.app.data.DateFilter
import com.glasspdf.app.data.FilterMode
import com.glasspdf.app.data.LayoutMode
import com.glasspdf.app.data.SortField
import com.glasspdf.app.ui.theme.GlassIconButton
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.theme.PillShape
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.MainViewModel

fun layoutIcon(m: LayoutMode): ImageVector = when (m) {
    LayoutMode.GRID2 -> Icons.Rounded.GridView
    LayoutMode.GRID3 -> Icons.Rounded.Apps
    LayoutMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
    LayoutMode.STRIP -> Icons.Rounded.ViewCarousel
    LayoutMode.STRIP_COMPACT -> Icons.Rounded.ViewWeek
    LayoutMode.LARGE -> Icons.Rounded.ViewAgenda
}

/** Instant layout switcher: 2 / 3 columns, list, strip, compact strip, large cover. */
@Composable
fun LayoutSelector(current: LayoutMode, onSelect: (LayoutMode) -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .glass(PillShape)
            .padding(4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        LayoutMode.values().forEach { m ->
            val sel = m == current
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (sel) primary else Color.Transparent)
                    .clickable { onSelect(m) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    layoutIcon(m), m.label,
                    tint = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SortMenu(settings: AppSettings, onSort: (SortField, Boolean) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        GlassPill(
            text = settings.sortField.label,
            icon = Icons.AutoMirrored.Rounded.Sort,
            onClick = { open = true }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortField.values().forEach { f ->
                DropdownMenuItem(
                    text = { Text(if (f == SortField.NAME) "Alphabetical" else "By ${f.label.lowercase()}") },
                    trailingIcon = { if (f == settings.sortField) Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) },
                    onClick = { onSort(f, settings.sortDescending); open = false }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (settings.sortDescending) "Descending" else "Ascending") },
                leadingIcon = {
                    Icon(
                        if (settings.sortDescending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        null, modifier = Modifier.size(18.dp)
                    )
                },
                onClick = { onSort(settings.sortField, !settings.sortDescending); open = false }
            )
        }
    }
}

@Composable
fun DateFilterMenu(current: DateFilter, onSelect: (DateFilter) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        GlassPill(
            text = if (current == DateFilter.ANY) "Date" else current.label,
            icon = Icons.Rounded.FilterList,
            selected = current != DateFilter.ANY,
            onClick = { open = true }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DateFilter.values().forEach { d ->
                DropdownMenuItem(
                    text = { Text(d.label) },
                    trailingIcon = { if (d == current) Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)) },
                    onClick = { onSelect(d); open = false }
                )
            }
        }
    }
}

@Composable
fun FilterChips(current: FilterMode, onSelect: (FilterMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterMode.values().forEach { m ->
            GlassPill(text = m.label, onClick = { onSelect(m) }, selected = m == current)
        }
    }
}

/** "+" button that imports individual PDFs or a whole folder through the Storage Access Framework. */
@Composable
fun AddPdfMenu(vm: MainViewModel, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val pickFiles = rememberPdfFilesPicker(vm)
    val pickFolder = rememberFolderPicker(vm)
    Box(modifier) {
        GlassIconButton(Icons.Rounded.Add, "Add PDFs", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
            MenuEntry("Add PDF files", Icons.Rounded.PictureAsPdf) { open = false; pickFiles() }
            MenuEntry("Add a folder (with subfolders)", Icons.Rounded.Folder) { open = false; pickFolder() }
        }
    }
}

@Composable
fun rememberPdfFilesPicker(vm: MainViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importFiles(uris)
    }
    return { launcher.launch(arrayOf("application/pdf")) }
}

@Composable
fun rememberFolderPicker(vm: MainViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.importFolder(uri)
    }
    return { launcher.launch(null) }
}

@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}
