package com.glasspdf.app.ui.vm

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.glasspdf.app.GlassPdfApp
import com.glasspdf.app.data.BookmarkEntity
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.util.PdfDocSource
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ReaderLoad {
    data object Loading : ReaderLoad
    data class Failed(val message: String) : ReaderLoad
    data class Ready(val source: PdfDocSource, val startPage: Int) : ReaderLoad
}

data class SearchHit(val page: Int, val snippet: String)

data class SearchUi(
    val query: String = "",
    val running: Boolean = false,
    val hits: List<SearchHit> = emptyList(),
    val finished: Boolean = false,
    val error: String? = null
)

class ReaderViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val container = (app as GlassPdfApp).container
    private val repo = container.repository
    private val context = app.applicationContext

    val pdfId: Long = handle.get<Long>("pdfId") ?: -1L

    val pdf: StateFlow<PdfEntity?> = repo.observePdf(pdfId).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val bookmarks: StateFlow<List<BookmarkEntity>> =
        repo.observeBookmarks(pdfId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _load = MutableStateFlow<ReaderLoad>(ReaderLoad.Loading)
    val load: StateFlow<ReaderLoad> = _load.asStateFlow()

    private val _search = MutableStateFlow(SearchUi())
    val search: StateFlow<SearchUi> = _search.asStateFlow()
    private var searchJob: Job? = null
    private var textDoc: PDDocument? = null
    private var source: PdfDocSource? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val p = repo.getPdf(pdfId)
            if (p == null) {
                _load.value = ReaderLoad.Failed("This PDF is no longer in your library.")
                return@launch
            }
            try {
                val src = PdfDocSource(context, Uri.parse(p.uri))
                if (src.pageCount <= 0) {
                    src.close()
                    _load.value = ReaderLoad.Failed("This PDF has no pages.")
                    return@launch
                }
                source = src
                _load.value = ReaderLoad.Ready(src, p.lastPage.coerceIn(0, src.pageCount - 1))
                repo.markOpened(pdfId)
            } catch (t: Throwable) {
                _load.value = ReaderLoad.Failed(
                    "Can’t open this PDF. It may be password-protected, damaged, moved, or access to it was lost."
                )
            }
        }
    }

    /** Uses the app scope so the position is still saved when leaving the reader clears this ViewModel. */
    fun saveLastPage(page: Int) {
        container.scope.launch { repo.saveLastPage(pdfId, page) }
    }

    fun toggleBookmark(page: Int) {
        val exists = bookmarks.value.any { it.page == page }
        viewModelScope.launch { repo.toggleBookmark(pdfId, page, exists) }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repo.deleteBookmark(id) }
    }

    fun removeFromLibrary() {
        viewModelScope.launch { repo.removeFromLibrary(listOf(pdfId)) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchUi()
    }

    /** Streams matches page by page so long documents never block the UI. */
    fun search(raw: String) {
        searchJob?.cancel()
        val query = raw.trim()
        if (query.length < 2) {
            _search.value = SearchUi()
            return
        }
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _search.value = SearchUi(query = query, running = true)
            try {
                val doc = textDoc ?: openTextDoc()
                if (doc == null) {
                    _search.value = SearchUi(query = query, error = "Search isn’t available for this file.")
                    return@launch
                }
                val stripper = PDFTextStripper()
                val hits = ArrayList<SearchHit>()
                val total = doc.numberOfPages
                var page = 1
                while (page <= total) {
                    ensureActive()
                    stripper.startPage = page
                    stripper.endPage = page
                    val text = stripper.getText(doc)
                    var idx = text.indexOf(query, ignoreCase = true)
                    var perPage = 0
                    while (idx >= 0 && perPage < 3) {
                        hits.add(SearchHit(page - 1, snippetAround(text, idx, query.length)))
                        perPage++
                        idx = text.indexOf(query, idx + query.length, ignoreCase = true)
                    }
                    if (page % 4 == 0 || page == total || hits.size >= 300) {
                        _search.value = SearchUi(query = query, running = page < total && hits.size < 300, hits = hits.toList())
                    }
                    if (hits.size >= 300) break
                    page++
                }
                _search.value = SearchUi(query = query, running = false, hits = hits.toList(), finished = true)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _search.value = SearchUi(query = query, error = "Search stopped: this PDF’s text couldn’t be read.")
            }
        }
    }

    private fun openTextDoc(): PDDocument? = try {
        val p = pdf.value
        val uri = Uri.parse(p?.uri ?: return null)
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input, MemoryUsageSetting.setupTempFileOnly())
        }?.also { textDoc = it }
    } catch (t: Throwable) {
        null
    }

    private fun snippetAround(text: String, idx: Int, len: Int): String {
        val start = (idx - 40).coerceAtLeast(0)
        val end = (idx + len + 60).coerceAtMost(text.length)
        val s = text.substring(start, end).replace(Regex("\\s+"), " ").trim()
        return (if (start > 0) "…" else "") + s + (if (end < text.length) "…" else "")
    }

    override fun onCleared() {
        searchJob?.cancel()
        runCatching { textDoc?.close() }
        runCatching { source?.close() }
        super.onCleared()
    }
}
