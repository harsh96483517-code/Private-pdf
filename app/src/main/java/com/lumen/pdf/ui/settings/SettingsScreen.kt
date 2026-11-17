package com.lumen.pdf.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.pdf.data.LibraryLayout
import com.lumen.pdf.data.ThemeMode
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.GlassPanel
import com.lumen.pdf.ui.components.GlassToggle
import com.lumen.pdf.ui.components.PillButton
import com.lumen.pdf.ui.components.ScreenHeader
import com.lumen.pdf.ui.icons.LIcon

@Composable
fun SettingsScreen(vm: LibraryViewModel) {
    val context = LocalContext.current
    val theme by vm.theme.collectAsStateWithLifecycle()
    val layout by vm.layout.collectAsStateWithLifecycle()
    val showRecent by vm.showRecentStrip.collectAsStateWithLifecycle()
    val autoCovers by vm.autoCovers.collectAsStateWithLifecycle()
    val manualOrder by vm.manualOrder.collectAsStateWithLifecycle()
    val threshold by vm.threshold.collectAsStateWithLifecycle()
    val pdfs by vm.pdfs.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()

    val exportSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(vm.exportSettings().toByteArray())
                }
            }.onSuccess {
                Toast.makeText(context, "Settings backup saved", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Could not write the backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().decodeToString()
                }
            }.getOrNull()
            if (raw != null) vm.importSettings(raw)
            else Toast.makeText(context, "Could not read that file", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Settings", subtitle = "Everything stays on this device")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSection("Appearance", LIcon.Palette) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassToggle("System", theme == ThemeMode.SYSTEM, { vm.setTheme(ThemeMode.SYSTEM) })
                        GlassToggle("Light", theme == ThemeMode.LIGHT, { vm.setTheme(ThemeMode.LIGHT) })
                        GlassToggle("Dark", theme == ThemeMode.DARK, { vm.setTheme(ThemeMode.DARK) })
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Library layout",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassToggle("2 col", layout == LibraryLayout.GRID2, { vm.setLayout(LibraryLayout.GRID2) })
                        GlassToggle("3 col", layout == LibraryLayout.GRID3, { vm.setLayout(LibraryLayout.GRID3) })
                        GlassToggle("List", layout == LibraryLayout.LIST, { vm.setLayout(LibraryLayout.LIST) })
                        GlassToggle("Strip", layout == LibraryLayout.STRIP, { vm.setLayout(LibraryLayout.STRIP) })
                    }
                }
            }

            item {
                SettingsSection("Library", LIcon.Storage) {
                    SettingSwitch(
                        title = "Recently opened strip",
                        subtitle = "Show a horizontal strip on Home",
                        checked = showRecent,
                        onCheckedChange = vm::setShowRecentStrip
                    )
                    SettingSwitch(
                        title = "Automatic covers",
                        subtitle = "Render the first page as the cover",
                        checked = autoCovers,
                        onCheckedChange = vm::setAutoCovers
                    )
                    SettingSwitch(
                        title = "Manual ordering",
                        subtitle = "Keep the order you set by dragging covers",
                        checked = manualOrder,
                        onCheckedChange = vm::setManualOrder
                    )
                }
            }

            item {
                SettingsSection("Auto arrange", LIcon.AutoAwesome) {
                    Text(
                        "Match sensitivity",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(threshold * 100).toInt()}% similarity needed to group documents",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = threshold,
                        onValueChange = { vm.setThreshold(it) },
                        valueRange = 0.15f..0.60f
                    )
                    Text(
                        "Lower values group more aggressively. Nothing is moved without your confirmation.",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    PillButton(
                        text = "Preview auto arrange",
                        icon = LIcon.AutoAwesome,
                        filled = true,
                        onClick = { vm.previewArrange() }
                    )
                }
            }

            item {
                SettingsSection("Storage", LIcon.Storage) {
                    Text(
                        "${pdfs.size} PDFs · ${folders.size} folders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Covers and page cache live in the app's private storage. Your PDFs are never copied.",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            text = "Clear cache",
                            icon = LIcon.Delete,
                            onClick = { vm.clearCache() }
                        )
                        PillButton(
                            text = "Rescan metadata",
                            icon = LIcon.Refresh,
                            onClick = { vm.rescanLibrary() }
                        )
                    }
                }
            }

            item {
                SettingsSection("Backup", LIcon.Backup) {
                    Text(
                        "Save or restore your layout, theme and arrange settings as a small JSON file.",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            text = "Export",
                            icon = LIcon.Backup,
                            onClick = { exportSettings.launch("lumen-settings.json") }
                        )
                        PillButton(
                            text = "Restore",
                            icon = LIcon.Refresh,
                            onClick = { importSettings.launch(arrayOf("application/json", "text/*")) }
                        )
                    }
                }
            }

            item {
                SettingsSection("About", LIcon.Info) {
                    Text(
                        "Lumen PDF 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "A fully offline PDF reader and smart library. No account, no server, no ads.",
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
