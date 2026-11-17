package com.lumen.pdf.ui.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumen.pdf.data.PdfFile
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.EmptyState
import com.lumen.pdf.ui.components.GlassPanel
import com.lumen.pdf.ui.components.GlassToggle
import com.lumen.pdf.ui.components.PdfListRow
import com.lumen.pdf.ui.components.ScreenHeader
import com.lumen.pdf.ui.icons.LIcon
import com.lumen.pdf.ui.library.rememberLibraryActions

private enum class Filter { ALL, RECENT, FAVORITES }
private enum class SortBy { TITLE, RECENT, SIZE, PAGES }

@Composable
fun SearchScreen(vm: LibraryViewModel, onBack: () -> Unit, onOpenPdf: (Long) -> Unit) {
    val pdfs by vm.pdfs.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }
    var sortBy by remember { mutableStateOf(SortBy.TITLE) }

    val actions = rememberLibraryActions(vm, onOpen = { onOpenPdf(it.id) })

    val results = remember(query, filter, sortBy, pdfs) {
        val base = pdfs.filter { pdf ->
            when (filter) {
                Filter.ALL -> true
                Filter.RECENT -> pdf.lastOpened > 0
                Filter.FAVORITES -> pdf.isFavorite
            }
        }
        val trimmed = query.trim().lowercase()
        val searched = if (trimmed.isEmpty()) base else base.filter { pdf ->
            pdf.title.lowercase().contains(trimmed) ||
                pdf.fileName.lowercase().contains(trimmed) ||
                pdf.keywords.lowercase().contains(trimmed) ||
                pdf.snippet.lowercase().contains(trimmed)
        }
        when (sortBy) {
            SortBy.TITLE -> searched.sortedBy { it.title.lowercase() }
            SortBy.RECENT -> searched.sortedByDescending { it.lastOpened }
            SortBy.SIZE -> searched.sortedByDescending { it.size }
            SortBy.PAGES -> searched.sortedByDescending { it.pageCount }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Search", subtitle = "${results.size} results", onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search title or PDF content") },
                    leadingIcon = { Icon(LIcon.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassToggle("All", filter == Filter.ALL, { filter = Filter.ALL })
                GlassToggle("Recent", filter == Filter.RECENT, { filter = Filter.RECENT })
                GlassToggle("Favorites", filter == Filter.FAVORITES, { filter = Filter.FAVORITES })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassToggle("A-Z", sortBy == SortBy.TITLE, { sortBy = SortBy.TITLE })
                GlassToggle("Recent", sortBy == SortBy.RECENT, { sortBy = SortBy.RECENT })
                GlassToggle("Size", sortBy == SortBy.SIZE, { sortBy = SortBy.SIZE })
                GlassToggle("Pages", sortBy == SortBy.PAGES, { sortBy = SortBy.PAGES })
            }
            Spacer(Modifier.height(10.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (results.isEmpty()) {
                item {
                    EmptyState(
                        title = "No matches",
                        message = "Try a different word. Content search uses the text extracted from your PDFs.",
                        icon = LIcon.Search
                    )
                }
            }
            items(results, key = { it.id }) { pdf: PdfFile ->
                PdfListRow(pdf = pdf, actions = actions)
            }
        }
    }
}
