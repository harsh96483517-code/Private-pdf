package com.lumen.pdf.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lumen.pdf.data.PdfFile
import com.lumen.pdf.ui.LibraryViewModel
import com.lumen.pdf.ui.components.EmptyState
import com.lumen.pdf.ui.components.PdfListRow
import com.lumen.pdf.ui.components.ScreenHeader
import com.lumen.pdf.ui.icons.LIcon

@Composable
fun RecentScreen(vm: LibraryViewModel, onOpenPdf: (Long) -> Unit) {
    val recent by vm.recent.collectAsState()
    PdfCollectionScreen(
        title = "Recent",
        subtitle = "${recent.size} opened recently",
        pdfs = recent,
        vm = vm,
        onOpenPdf = onOpenPdf,
        emptyTitle = "Nothing opened yet",
        emptyMessage = "PDFs you read will appear here with your last reading position.",
        icon = LIcon.History
    )
}

@Composable
fun FavoritesScreen(vm: LibraryViewModel, onOpenPdf: (Long) -> Unit) {
    val favorites by vm.favorites.collectAsState()
    PdfCollectionScreen(
        title = "Favorites",
        subtitle = "${favorites.size} starred",
        pdfs = favorites,
        vm = vm,
        onOpenPdf = onOpenPdf,
        emptyTitle = "No favorites yet",
        emptyMessage = "Long press any PDF cover and tap the star to keep it here.",
        icon = LIcon.Star
    )
}

@Composable
fun PdfCollectionScreen(
    title: String,
    subtitle: String,
    pdfs: List<PdfFile>,
    vm: LibraryViewModel,
    onOpenPdf: (Long) -> Unit,
    emptyTitle: String,
    emptyMessage: String,
    icon: ImageVector
) {
    val actions = rememberLibraryActions(vm, onOpen = { onOpenPdf(it.id) })
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = title, subtitle = subtitle)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pdfs.isEmpty()) {
                item {
                    EmptyState(title = emptyTitle, message = emptyMessage, icon = icon)
                }
            }
            items(pdfs, key = { it.id }) { pdf ->
                PdfListRow(pdf = pdf, actions = actions)
            }
        }
    }
}
