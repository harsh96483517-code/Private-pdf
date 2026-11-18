package com.glasspdf.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glasspdf.app.data.title
import com.glasspdf.app.domain.ProposedItem
import com.glasspdf.app.ui.components.MenuEntry
import com.glasspdf.app.ui.components.PdfCoverImage
import com.glasspdf.app.ui.components.ReorderableColumn
import com.glasspdf.app.ui.theme.AccentPill
import com.glasspdf.app.ui.theme.EmptyState
import com.glasspdf.app.ui.theme.GlassIconButton
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.ArrangeState
import com.glasspdf.app.ui.vm.ArrangeViewModel
import com.glasspdf.app.ui.vm.UiGroup

private val Good = Color(0xFF3DDC97)
private val Medium = Color(0xFFFFC857)
private val Low = Color(0xFFFF7A7A)

private fun confColor(c: Float): Color = when {
    c >= 0.75f -> Good
    c >= 0.55f -> Medium
    else -> Low
}

@Composable
fun ArrangeScreen(onBack: () -> Unit, onShowFolders: () -> Unit, vm: ArrangeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var renaming by remember { mutableStateOf<UiGroup?>(null) }
    var newGroupFor by remember { mutableStateOf<Long?>(null) }
    var confirming by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = topInset + 10.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
                Column(Modifier.weight(1f)) {
                    Text("Auto Arrange", style = MaterialTheme.typography.headlineMedium)
                    Text("Review before anything changes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GlassPill("Re-scan", icon = Icons.Rounded.Refresh, onClick = { vm.scan() })
            }

            when (val s = state) {
                ArrangeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text("Analyzing your PDFs…", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(Modifier.fillMaxWidth(0.5f))
                    }
                }

                ArrangeState.Empty -> EmptyState(
                    Icons.Rounded.Folder, "Nothing to arrange",
                    "All your PDFs are already in folders. You can include them in Settings → Auto Arrange."
                )

                is ArrangeState.Done -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.Rounded.Check, "All sorted",
                        "${s.moved} PDF${if (s.moved == 1) "" else "s"} filed into ${s.folders} folder${if (s.folders == 1) "" else "s"}. " +
                            "Your files on the device were not touched.",
                        action = { GlassPill("View folders", onClick = onShowFolders, selected = true) }
                    )
                }

                is ArrangeState.Ready -> {
                    val enabledGroups = s.groups.filter { it.enabled }
                    val total = enabledGroups.sumOf { it.items.size }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = bottomInset + 120.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "${s.docs.size} PDFs analyzed → ${s.groups.count { !it.unsorted }} suggested folder${if (s.groups.count { !it.unsorted } == 1) "" else "s"}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Uses file names, embedded titles, first-page text, part/chapter numbers and content similarity. " +
                                        "Drag ⠿ to reorder, use ⋮ to correct a PDF, switch a folder off to skip it. Nothing is moved until you apply.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (progress != null || s.analyzing) {
                                    Text(
                                        "Still reading some PDFs — tap Re-scan when it finishes for better results.",
                                        style = MaterialTheme.typography.bodySmall, color = Medium
                                    )
                                }
                            }
                        }
                        items(s.groups, key = { it.key }) { g ->
                            GroupCard(
                                group = g,
                                allGroups = s.groups,
                                vm = vm,
                                docs = s.docs,
                                onRename = { renaming = g },
                                onNewGroup = { id -> newGroupFor = id }
                            )
                        }
                        if (s.skipped.isNotEmpty()) {
                            item {
                                Text(
                                    "${s.skipped.size} PDF${if (s.skipped.size == 1) "" else "s"} skipped — they stay where they are.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        val ready = state as? ArrangeState.Ready
        if (ready != null) {
            val total = ready.groups.filter { it.enabled }.sumOf { it.items.size }
            val folders = ready.groups.count { it.enabled && it.items.isNotEmpty() }
            AccentPill(
                if (total == 0) "Nothing selected" else "Apply · $total PDFs → $folders folders",
                Icons.Rounded.Check,
                onClick = { if (total > 0) confirming = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset + 20.dp)
            )
        }
    }

    // rename a proposed folder
    val rg = renaming
    if (rg != null) {
        var text by remember(rg.key) { mutableStateOf(rg.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Folder name") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, shape = RoundedCornerShape(16.dp)) },
            confirmButton = { TextButton(onClick = { vm.renameGroup(rg.key, text); renaming = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }

    // move a PDF to a brand-new folder
    val ng = newGroupFor
    if (ng != null) {
        var text by remember(ng) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newGroupFor = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("New folder") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text("Folder name") }, shape = RoundedCornerShape(16.dp)) },
            confirmButton = {
                TextButton(enabled = text.isNotBlank(), onClick = { vm.moveItemToNewGroup(ng, text); newGroupFor = null }) { Text("Move here") }
            },
            dismissButton = { TextButton(onClick = { newGroupFor = null }) { Text("Cancel") } }
        )
    }

    // final confirmation
    if (confirming) {
        val r = state as? ArrangeState.Ready
        val total = r?.groups?.filter { it.enabled }?.sumOf { it.items.size } ?: 0
        val n = r?.groups?.count { it.enabled && it.items.isNotEmpty() } ?: 0
        AlertDialog(
            onDismissRequest = { confirming = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Apply arrangement?") },
            text = {
                Text(
                    "$total PDF${if (total == 1) "" else "s"} will be filed into $n folder${if (n == 1) "" else "s"} in your library, " +
                        "in the order shown. Your PDF files are not moved, renamed or deleted, and you can move things again any time."
                )
            },
            confirmButton = { TextButton(onClick = { confirming = false; vm.apply() }) { Text("Apply") } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun GroupCard(
    group: UiGroup,
    allGroups: List<UiGroup>,
    vm: ArrangeViewModel,
    docs: Map<Long, com.glasspdf.app.data.PdfEntity>,
    onRename: () -> Unit,
    onNewGroup: (Long) -> Unit
) {
    val alpha = if (group.enabled) 1f else 0.5f
    Column(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp), strong = true).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (group.unsorted) Icons.Rounded.Search else Icons.Rounded.Folder, null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha), modifier = Modifier.size(26.dp)
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        group.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!group.unsorted) {
                        Icon(
                            Icons.Rounded.Edit, "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp).clickable(onClick = onRename)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${group.items.size} PDF${if (group.items.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!group.unsorted) ConfidenceChip(group.confidence)
                }
            }
            Switch(checked = group.enabled, onCheckedChange = { vm.toggleGroup(group.key) })
        }
        Text(group.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        var expanded by remember(group.key) { mutableStateOf(false) }
        val limit = 12
        val shown = if (expanded || group.items.size <= limit) group.items else group.items.take(limit)
        ReorderableColumn(
            items = shown,
            key = { it.docId },
            rowHeight = 70.dp,
            onMove = { from, to -> vm.reorder(group.key, from, to) }
        ) { item, index, handle ->
            ItemRow(item, index, handle, docs, group, allGroups, vm, alpha, onNewGroup)
        }
        if (group.items.size > limit) {
            GlassPill(
                if (expanded) "Show fewer" else "Show all ${group.items.size} PDFs",
                onClick = { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun ConfidenceChip(c: Float) {
    val color = confColor(c)
    Text(
        "${(c * 100).toInt()}% match",
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun ItemRow(
    item: ProposedItem,
    index: Int,
    handle: Modifier,
    docs: Map<Long, com.glasspdf.app.data.PdfEntity>,
    group: UiGroup,
    allGroups: List<UiGroup>,
    vm: ArrangeViewModel,
    alpha: Float,
    onNewGroup: (Long) -> Unit
) {
    val pdf = docs[item.docId] ?: return
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxSize().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = handle.size(30.dp).padding(3.dp))
        Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(width = 14.dp, height = 16.dp))
        PdfCoverImage(
            pdf,
            Modifier.fillMaxHeight().aspectRatio(0.72f).clip(RoundedCornerShape(6.dp))
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(pdf.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            val lead = listOfNotNull(item.orderLabel, if (group.unsorted) null else "${(item.confidence * 100).toInt()}%").joinToString(" · ")
            if (lead.isNotEmpty()) {
                Text(lead, style = MaterialTheme.typography.labelSmall, color = if (group.unsorted) MaterialTheme.colorScheme.onSurfaceVariant else confColor(item.confidence), maxLines = 1)
            }
            Text(item.reason, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            Box(
                Modifier.size(32.dp).clip(CircleShape).clickable { menu = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.MoreVert, "Correct", modifier = Modifier.size(20.dp)) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(20.dp)) {
                allGroups.filter { it.key != group.key }.forEach { g ->
                    MenuEntry("Move to “${g.name}”", Icons.Rounded.Folder) { menu = false; vm.moveItem(item.docId, g.key) }
                }
                MenuEntry("Move to new folder…", Icons.Rounded.Folder) { menu = false; onNewGroup(item.docId) }
                MenuEntry("Skip — leave where it is", Icons.Rounded.Check) { menu = false; vm.skipItem(item.docId) }
            }
        }
    }
}
