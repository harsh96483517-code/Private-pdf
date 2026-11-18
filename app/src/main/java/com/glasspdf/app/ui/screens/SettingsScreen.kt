package com.glasspdf.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasspdf.app.data.CoverStyle
import com.glasspdf.app.data.LayoutMode
import com.glasspdf.app.data.ThemeMode
import com.glasspdf.app.ui.components.LayoutSelector
import com.glasspdf.app.ui.components.ScreenHeader
import com.glasspdf.app.ui.components.rememberFolderPicker
import com.glasspdf.app.ui.theme.GlassIconButton
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.theme.SegmentedPills
import com.glasspdf.app.ui.theme.SwitchRow
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.MainViewModel
import com.glasspdf.app.util.formatSize
import kotlin.math.roundToInt

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val pdfs by vm.pdfs.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val storage by vm.storageInfo.collectAsStateWithLifecycle()
    LaunchedEffect(pdfs) { vm.refreshStorage() }

    val pickFolder = rememberFolderPicker(vm)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) vm.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.restoreBackup(uri)
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 10.dp, bottom = bottomInset + 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Settings", subtitle = "Everything stays on this device") }

        item {
            SettingsCard("Appearance") {
                SegmentedPills(ThemeMode.values().toList(), s.theme, { it.label }, { vm.setTheme(it) })
            }
        }

        item {
            SettingsCard("Library layout") {
                Text("Choose how PDFs are shown. Your choice is remembered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SegmentedPills(
                    listOf(LayoutMode.GRID2, LayoutMode.GRID3), s.layout,
                    { if (it == LayoutMode.GRID2) "2 columns" else "3 columns" }, { vm.setLayout(it) }
                )
                LayoutSelector(s.layout, onSelect = { vm.setLayout(it) })
                Text(s.layout.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            SettingsCard("PDF covers") {
                SwitchRow("Show titles under covers", null, s.showTitles) { vm.setShowTitles(it) }
                SwitchRow("Page count badge", "Small “12 p” label on each cover", s.showPageBadge) { vm.setShowBadge(it) }
                Text("Cover style", style = MaterialTheme.typography.titleSmall)
                SegmentedPills(CoverStyle.values().toList(), s.coverStyle, { it.label }, { vm.setCoverStyle(it) })
                Text(
                    "Covers use the first page automatically. Use ⋮ → Change cover on any PDF to pick your own image.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsCard("Auto Arrange") {
                var conf by remember(s.arrangeMinConfidence) { mutableFloatStateOf(s.arrangeMinConfidence) }
                Text("Minimum confidence: ${(conf * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = conf, onValueChange = { conf = it }, onValueChangeFinished = { vm.setArrangeConfidence(conf) },
                    valueRange = 0.3f..0.9f
                )
                Text(
                    "Matches below this go to “Unsorted” instead of a folder.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Smallest folder", style = MaterialTheme.typography.titleSmall)
                        Text("${s.arrangeMinGroupSize} PDF${if (s.arrangeMinGroupSize == 1) "" else "s"} or more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    GlassIconButton(Icons.Rounded.Remove, "Fewer", onClick = { if (s.arrangeMinGroupSize > 1) vm.setArrangeGroupSize(s.arrangeMinGroupSize - 1) }, size = 38.dp)
                    GlassIconButton(Icons.Rounded.Add, "More", onClick = { if (s.arrangeMinGroupSize < 6) vm.setArrangeGroupSize(s.arrangeMinGroupSize + 1) }, size = 38.dp)
                }
                SwitchRow("Only PDFs outside folders", "Keeps folders you made by hand untouched", s.arrangeOnlyUnfiled) { vm.setArrangeUnfiled(it) }
                SwitchRow("Read PDF text", "Use the first pages’ text, not just file names", s.arrangeUseContent) { vm.setArrangeContent(it) }
            }
        }

        item {
            SettingsCard("Library sources") {
                Text(
                    "PDFs are read in place with Android’s file picker access. Nothing is copied.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (s.sources.isEmpty()) {
                    Text("No folders added yet.", style = MaterialTheme.typography.bodyMedium)
                }
                s.sources.forEach { src ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            Uri.parse(src).lastPathSegment ?: src,
                            style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        GlassPill("Remove", onClick = { vm.removeSource(src) })
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPill("Add folder", onClick = pickFolder, selected = true)
                    GlassPill("Rescan now", onClick = { vm.rescan() })
                }
            }
        }

        item {
            SettingsCard("Storage") {
                StorageRow("PDFs in library", "${pdfs?.size ?: 0}")
                StorageRow("Folders", "${folders.size}")
                StorageRow("Thumbnail cache", formatSize(storage.thumbBytes))
                StorageRow("Custom covers", formatSize(storage.coverBytes))
                StorageRow("Library database", formatSize(storage.dbBytes))
                GlassPill("Clear thumbnail cache", onClick = { vm.clearThumbnailCache() })
                Text(
                    "Thumbnails are rebuilt automatically when needed. Your PDFs and custom covers are not affected.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsCard("Backup & restore") {
                Text(
                    "Saves settings, folders, custom titles, favorites, bookmarks and reading progress to a file. " +
                        "Custom cover images are not included.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPill("Back up", onClick = { exportLauncher.launch("glasspdf-backup.json") }, selected = true)
                    GlassPill("Restore", onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                }
            }
        }

        item {
            SettingsCard("About") {
                Text("GlassPDF 1.0.0", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Fully offline. No account, no server, no API key.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
