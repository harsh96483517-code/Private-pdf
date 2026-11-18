package com.glasspdf.app.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Fixed-row-height list where each row exposes a drag handle modifier. Dropping calls [onMove] with
 * the old and new index. Other rows slide out of the way while dragging.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    rowHeight: Dp,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, index: Int, handle: Modifier) -> Unit
) {
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    var dragFrom by remember { mutableStateOf(-1) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val count = items.size
    val currentOnMove by rememberUpdatedState(onMove)
    val currentCount by rememberUpdatedState(count)

    val target = if (dragFrom >= 0) (dragFrom + (dragDy / rowPx).roundToInt()).coerceIn(0, (count - 1).coerceAtLeast(0)) else -1

    Box(modifier.fillMaxWidth().height(rowHeight * count)) {
        items.forEachIndexed { index, item ->
            androidx.compose.runtime.key(key(item)) {
                val dragging = index == dragFrom
                val slot = when {
                    dragFrom < 0 -> index
                    dragging -> index
                    dragFrom < target && index in (dragFrom + 1)..target -> index - 1
                    target < dragFrom && index in target until dragFrom -> index + 1
                    else -> index
                }
                val y = if (dragging) (index * rowPx + dragDy).roundToInt() else (slot * rowPx).roundToInt()
                val index2 by rememberUpdatedState(index)
                val handle = Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragFrom = index2
                            dragDy = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragDy += amount.y
                        },
                        onDragEnd = {
                            val from = dragFrom
                            val to = (from + (dragDy / rowPx).roundToInt()).coerceIn(0, (currentCount - 1).coerceAtLeast(0))
                            dragFrom = -1
                            dragDy = 0f
                            if (from >= 0 && from != to) currentOnMove(from, to)
                        },
                        onDragCancel = {
                            dragFrom = -1
                            dragDy = 0f
                        }
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .offset { IntOffset(0, y) }
                        .zIndex(if (dragging) 1f else 0f)
                ) {
                    row(item, index, handle)
                }
            }
        }
    }
}
