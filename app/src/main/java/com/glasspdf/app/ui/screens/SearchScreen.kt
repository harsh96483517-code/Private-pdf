package com.glasspdf.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.data.title
import com.glasspdf.app.domain.FolderTree
import com.glasspdf.app.domain.sortPdfs
import com.glasspdf.app.ui.components.PdfCover
import com.glasspdf.app.ui.theme.EmptyState
import com.glasspdf.app.ui.theme.GlassIconButton
import com.glasspdf.app.ui.theme.GlassPill
import com.glasspdf.app.ui.theme.PillShape
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.ui.vm.MainViewModel
import kotlinx.coroutines.delay

private enum class SearchScope(val label: String) {
    ALL("Everything"), NAMES("File names"), CONTENT("PDF content"), FOLDERS("Folders")
}

@Composable
fun SearchScreen(vm: MainViewModel, onBack: () -> Unit, onOpenPdf: (Long) -> Unit, onOpenFolder: (Long) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val all by vm.pdfs.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var scope by remember { mutableStateOf(SearchScope.ALL) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val q = query.trim()
    val contentHits by produceState<Map<Long, String>>(emptyMap(), q, scope) {
        if (q.length >= 2 && (scope == SearchScope.ALL || scope == SearchScope.CONTENT)) {
            delay(250)
            value = vm.repo.searchContent(q)
        } else {
            value = emptyMap()
        }
    }

    val pdfs = all.orEmpty()
    val nameHits: List<PdfEntity> = remember(q, pdfs, scope) {
        if (q.isEmpty() || scope == SearchScope.CONTENT || scope == SearchScope.FOLDERS) emptyList()
        else pdfs.filter {
            it.title.contains(q, true) || it.fileName.contains(q, true) || (it.metaTitle?.contains(q, true) == true)
        }
    }
    val pdfResults: List<PdfEntity> = remember(nameHits, contentHits, pdfs, settings, scope) {
        if (scope == SearchScope.FOLDERS) emptyList()
        else {
            val ids = nameHits.map { it.id }.toSet()
            val extra = pdfs.filter { it.id in contentHits.keys && it.id !in ids }
            sortPdfs(nameHits, settings) + sortPdfs(extra, settings)
        }
    }
    val folderResults = remember(q, folders, scope) {
        if (q.isEmpty() || scope == SearchScope.NAMES || scope == SearchScope.CONTENT) emptyList()
        else folders.filter { it.name.contains(q, true) }
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize().padding(top = topInset + 10.dp).imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
            Box(Modifier.weight(1f).glass(PillShape, strong = true)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search PDFs, folders, content…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(Icons.Rounded.Close, "Clear", modifier = Modifier.clickable { query = "" })
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchScope.values().forEach { s -> GlassPill(s.label, onClick = { scope = s }, selected = s == scope) }
        }

        if (q.isEmpty()) {
            EmptyState(Icons.Rounded.Search, "Search your library", "Find PDFs by file name, folder, or words inside the PDF (first pages).")
        } else if (pdfResults.isEmpty() && folderResults.isEmpty()) {
            EmptyState(Icons.Rounded.Search, "No results", "Nothing matched “$q”.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomInset + 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (folderResults.isNotEmpty()) {
                    item { Text("Folders", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(folderResults, key = { "f${it.id}" }) { f ->
                        val count = FolderTree.pdfsRecursive(folders, pdfs, f.id).size
                        Row(
                            Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).clickable { onOpenFolder(f.id) }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$count PDF${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (pdfResults.isNotEmpty()) {
                    item { Text("PDFs · ${pdfResults.size}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(pdfResults, key = { it.id }) { p ->
                        Row(
                            Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).clickable { onOpenPdf(p.id) }.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PdfCover(p, settings, Modifier.width(52.dp), corner = 8.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(p.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val snippet = contentHits[p.id]
                                if (snippet != null) {
                                    Text(snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                } else {
                                    Text(p.fileName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
