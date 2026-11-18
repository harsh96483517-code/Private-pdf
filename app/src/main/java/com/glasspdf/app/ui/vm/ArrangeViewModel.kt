package com.glasspdf.app.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glasspdf.app.GlassPdfApp
import com.glasspdf.app.data.ApplyGroup
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.domain.ArrangeConfig
import com.glasspdf.app.domain.ArrangeDoc
import com.glasspdf.app.domain.AutoArranger
import com.glasspdf.app.domain.ProposedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiGroup(
    val key: String,
    val name: String,
    val unsorted: Boolean,
    val enabled: Boolean,
    val items: List<ProposedItem>,
    val note: String
) {
    val confidence: Float
        get() = if (items.isEmpty()) 0f else items.map { it.confidence }.average().toFloat()
}

sealed interface ArrangeState {
    data object Loading : ArrangeState
    data object Empty : ArrangeState
    data class Ready(
        val groups: List<UiGroup>,
        val docs: Map<Long, PdfEntity>,
        val skipped: Set<Long>,
        val analyzing: Boolean
    ) : ArrangeState

    data class Done(val moved: Int, val folders: Int) : ArrangeState
}

class ArrangeViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as GlassPdfApp).container
    private val repo = container.repository
    private val store = container.settingsStore

    private val _state = MutableStateFlow<ArrangeState>(ArrangeState.Loading)
    val state: StateFlow<ArrangeState> = _state.asStateFlow()
    val progress = repo.progress

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _state.value = ArrangeState.Loading
            val s = store.current()
            val pool = repo.allPdfs().filter { !it.broken && (!s.arrangeOnlyUnfiled || it.folderId == null) }
            if (pool.isEmpty()) {
                _state.value = ArrangeState.Empty
                return@launch
            }
            val texts = if (s.arrangeUseContent) repo.textsFor(pool.map { it.id }) else emptyMap()
            val docs = pool.map {
                val title = listOfNotNull(it.customTitle, it.metaTitle).joinToString(" ").ifBlank { null }
                ArrangeDoc(it.id, it.fileName, title, texts[it.id].orEmpty(), it.modifiedAt)
            }
            val groups = withContext(Dispatchers.Default) {
                AutoArranger.arrange(docs, ArrangeConfig(s.arrangeMinConfidence, s.arrangeMinGroupSize, s.arrangeUseContent))
            }
            _state.value = ArrangeState.Ready(
                groups = groups.map { UiGroup(it.key, it.name, it.isUnsorted, true, it.items, it.orderNote) },
                docs = pool.associateBy { it.id },
                skipped = emptySet(),
                analyzing = repo.progress.value != null
            )
        }
    }

    private fun updateReady(block: (ArrangeState.Ready) -> ArrangeState.Ready) {
        val cur = _state.value
        if (cur is ArrangeState.Ready) _state.value = block(cur)
    }

    fun toggleGroup(key: String) = updateReady { r ->
        r.copy(groups = r.groups.map { if (it.key == key) it.copy(enabled = !it.enabled) else it })
    }

    fun renameGroup(key: String, name: String) = updateReady { r ->
        val clean = name.trim()
        if (clean.isEmpty()) r else r.copy(groups = r.groups.map { if (it.key == key) it.copy(name = clean) else it })
    }

    fun reorder(key: String, from: Int, to: Int) = updateReady { r ->
        r.copy(groups = r.groups.map { g ->
            if (g.key != key || from !in g.items.indices || to !in g.items.indices) g
            else {
                val list = g.items.toMutableList()
                val item = list.removeAt(from)
                list.add(to, item)
                g.copy(items = list)
            }
        })
    }

    /** Moves a PDF into another proposed group (manual correction). */
    fun moveItem(docId: Long, toKey: String) = updateReady { r ->
        val item = r.groups.flatMap { it.items }.firstOrNull { it.docId == docId } ?: return@updateReady r
        val moved = item.copy(reason = "Moved by you", confidence = if (toKey == "unsorted") 0f else maxOf(item.confidence, 0.5f))
        r.copy(
            groups = r.groups
                .map { g ->
                    when {
                        g.key == toKey -> g.copy(items = g.items.filter { it.docId != docId } + moved)
                        else -> g.copy(items = g.items.filter { it.docId != docId })
                    }
                }
                .filter { it.items.isNotEmpty() || it.key == toKey }
        )
    }

    fun moveItemToNewGroup(docId: Long, name: String) = updateReady { r ->
        val clean = name.trim()
        if (clean.isEmpty()) return@updateReady r
        val item = r.groups.flatMap { it.items }.firstOrNull { it.docId == docId } ?: return@updateReady r
        val moved = item.copy(reason = "Moved by you", confidence = 0.9f)
        val existing = r.groups.firstOrNull { !it.unsorted && it.name.equals(clean, ignoreCase = true) }
        val without = r.groups.map { it.copy(items = it.items.filter { i -> i.docId != docId }) }
        val groups = if (existing != null) {
            without.map { if (it.key == existing.key) it.copy(items = it.items + moved) else it }
        } else {
            val newGroup = UiGroup("manual:${System.nanoTime()}", clean, false, true, listOf(moved), "Created by you")
            val unsortedIdx = without.indexOfFirst { it.unsorted }
            val list = without.toMutableList()
            if (unsortedIdx >= 0) list.add(unsortedIdx, newGroup) else list.add(newGroup)
            list
        }
        r.copy(groups = groups.filter { it.items.isNotEmpty() })
    }

    /** Leave this PDF exactly where it is. */
    fun skipItem(docId: Long) = updateReady { r ->
        r.copy(
            groups = r.groups.map { g -> g.copy(items = g.items.filter { it.docId != docId }) }.filter { it.items.isNotEmpty() },
            skipped = r.skipped + docId
        )
    }

    fun apply() {
        val cur = _state.value as? ArrangeState.Ready ?: return
        viewModelScope.launch {
            val groups = cur.groups.filter { it.enabled && it.items.isNotEmpty() }
                .map { ApplyGroup(if (it.unsorted) "Unsorted" else it.name, it.items.map { i -> i.docId }) }
            val moved = repo.applyArrangement(groups)
            _state.value = ArrangeState.Done(moved, groups.size)
        }
    }
}
