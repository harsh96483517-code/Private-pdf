package com.glasspdf.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

const val DROP_ROOT = -1L

data class DragPayload(val isFolder: Boolean, val id: Long, val label: String)

/** Long-press a PDF or folder, drag it onto a folder (or a breadcrumb) and let go. */
@Stable
class DragDropState {
    var dragging by mutableStateOf<DragPayload?>(null)
    var position by mutableStateOf(Offset.Zero)
    var hover by mutableStateOf<Long?>(null)
    val targets = mutableStateMapOf<Long, Rect>()

    fun update(payload: DragPayload) {
        hover = targets.entries.firstOrNull { (id, rect) ->
            rect.contains(position) && !(payload.isFolder && id == payload.id)
        }?.key
    }

    fun end(): Long? {
        val t = hover
        dragging = null
        hover = null
        return t
    }
}

fun Modifier.dropTarget(state: DragDropState, id: Long): Modifier = composed {
    DisposableEffect(id) { onDispose { state.targets.remove(id) } }
    this.onGloballyPositioned { state.targets[id] = it.boundsInRoot() }
}

fun Modifier.dragSource(state: DragDropState, payload: DragPayload, onDrop: (Long) -> Unit): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val drop by rememberUpdatedState(onDrop)
    this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .pointerInput(payload.id, payload.isFolder) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    state.dragging = payload
                    state.position = origin + offset
                    state.update(payload)
                },
                onDrag = { change, amount ->
                    change.consume()
                    state.position += amount
                    state.update(payload)
                },
                onDragEnd = {
                    val target = state.end()
                    if (target != null) drop(target)
                },
                onDragCancel = { state.end() }
            )
        }
}
