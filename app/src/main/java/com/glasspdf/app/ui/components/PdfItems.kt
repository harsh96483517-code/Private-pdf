package com.glasspdf.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glasspdf.app.data.AppSettings
import com.glasspdf.app.data.LayoutMode
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.data.title
import com.glasspdf.app.ui.theme.SectionHeader
import com.glasspdf.app.ui.theme.appear
import com.glasspdf.app.ui.theme.bouncyClick
import com.glasspdf.app.ui.theme.glass
import com.glasspdf.app.util.formatSize

/** Everything a PDF card needs from its screen. */
@Stable
class PdfUi(
    val settings: AppSettings,
    val selected: Set<Long>,
    val selectionMode: Boolean,
    val onClick: (PdfEntity) -> Unit,
    val onLongClick: (PdfEntity) -> Unit,
    val menu: PdfMenuController,
    /** Extra modifier per card (e.g. drag source). */
    val extra: (PdfEntity) -> Modifier = { Modifier },
    /** When false, long-press is left free (used for drag and drop). */
    val longPressEnabled: Boolean = true
)

/** The grid uses 6 columns so 2-up (span 3), 3-up (span 2) and full-width rows can mix freely. */
const val GRID_COLUMNS = 6

fun spanFor(layout: LayoutMode): Int = when (layout) {
    LayoutMode.GRID2 -> 3
    LayoutMode.GRID3 -> 2
    else -> GRID_COLUMNS
}

@Composable
fun PdfMenuButton(pdf: PdfEntity, ui: PdfUi, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(20.dp)) {
            MenuEntry("Open", Icons.AutoMirrored.Rounded.OpenInNew) { open = false; ui.menu.onOpen(pdf) }
            MenuEntry("Rename", Icons.Rounded.Edit) { open = false; ui.menu.dialog = PdfDialog.Rename(pdf) }
            MenuEntry("Change cover", Icons.Rounded.Image) { open = false; ui.menu.dialog = PdfDialog.Cover(pdf) }
            MenuEntry(
                if (pdf.isFavorite) "Remove from favorites" else "Add to favorites",
                if (pdf.isFavorite) Icons.Rounded.StarBorder else Icons.Rounded.Star
            ) { open = false; ui.menu.onToggleFavorite(pdf) }
            MenuEntry("Move to folder", Icons.Rounded.DriveFileMove) { open = false; ui.menu.dialog = PdfDialog.Move(listOf(pdf.id)) }
            MenuEntry("File information", Icons.Rounded.Info) { open = false; ui.menu.dialog = PdfDialog.Info(pdf) }
            MenuEntry("Delete", Icons.Rounded.Delete, danger = true) { open = false; ui.menu.dialog = PdfDialog.Delete(listOf(pdf.id)) }
        }
    }
}

@Composable
fun MenuEntry(text: String, icon: ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(text, color = color) },
        leadingIcon = { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) },
        onClick = onClick
    )
}

private fun PdfUi.longClickFor(pdf: PdfEntity): (() -> Unit)? =
    if (longPressEnabled) ({ onLongClick(pdf) }) else null

@Composable
fun PdfGridItem(pdf: PdfEntity, ui: PdfUi, modifier: Modifier = Modifier) {
    Column(modifier.appear().then(ui.extra(pdf))) {
        PdfCover(
            pdf, ui.settings,
            Modifier
                .fillMaxWidth()
                .bouncyClick(onLongClick = ui.longClickFor(pdf), onClick = { ui.onClick(pdf) }),
            selected = pdf.id in ui.selected,
            selectionMode = ui.selectionMode
        )
        if (ui.settings.showTitles) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pdf.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!ui.selectionMode) PdfMenuButton(pdf, ui)
            }
        }
    }
}

@Composable
fun PdfListItem(pdf: PdfEntity, ui: PdfUi, modifier: Modifier = Modifier) {
    Row(
        modifier
            .appear()
            .then(ui.extra(pdf))
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp))
            .bouncyClick(onLongClick = ui.longClickFor(pdf), onClick = { ui.onClick(pdf) })
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PdfCover(pdf, ui.settings, Modifier.width(58.dp), selected = pdf.id in ui.selected, selectionMode = ui.selectionMode, corner = 10.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pdf.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val pages = if (pdf.pageCount > 0) "${pdf.pageCount} pages · " else ""
            Text(
                pages + formatSize(pdf.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!ui.selectionMode) PdfMenuButton(pdf, ui)
    }
}

@Composable
fun PdfLargeItem(pdf: PdfEntity, ui: PdfUi, modifier: Modifier = Modifier) {
    Column(
        modifier.appear().then(ui.extra(pdf)).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PdfCover(
            pdf, ui.settings,
            Modifier
                .fillMaxWidth(0.78f)
                .bouncyClick(onLongClick = ui.longClickFor(pdf), onClick = { ui.onClick(pdf) }),
            selected = pdf.id in ui.selected, selectionMode = ui.selectionMode, corner = 20.dp
        )
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                pdf.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!ui.selectionMode) PdfMenuButton(pdf, ui)
        }
    }
}

@Composable
fun PdfStripItem(pdf: PdfEntity, ui: PdfUi, width: Dp, modifier: Modifier = Modifier) {
    Column(modifier.width(width).then(ui.extra(pdf))) {
        PdfCover(
            pdf, ui.settings,
            Modifier
                .fillMaxWidth()
                .bouncyClick(onLongClick = ui.longClickFor(pdf), onClick = { ui.onClick(pdf) }),
            selected = pdf.id in ui.selected, selectionMode = ui.selectionMode,
            corner = if (width < 100.dp) 10.dp else 14.dp
        )
        if (ui.settings.showTitles) {
            Text(
                pdf.title,
                style = if (width < 100.dp) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Horizontal strip with a section heading. [width] controls the cover size (compact / normal / large). */
@Composable
fun PdfStrip(title: String, pdfs: List<PdfEntity>, ui: PdfUi, width: Dp = 128.dp, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title, subtitle = "${pdfs.size} PDF${if (pdfs.size == 1) "" else "s"}")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(pdfs, key = { it.id }) { PdfStripItem(it, ui, width) }
        }
    }
}

fun stripWidthFor(layout: LayoutMode): Dp = if (layout == LayoutMode.STRIP_COMPACT) 88.dp else 150.dp

/** Adds the PDFs to a 6-column grid according to the chosen layout. */
fun LazyGridScope.pdfGalleryItems(pdfs: List<PdfEntity>, layout: LayoutMode, ui: PdfUi, stripTitle: String = "PDFs") {
    when (layout) {
        LayoutMode.GRID2, LayoutMode.GRID3 ->
            items(pdfs, key = { it.id }, span = { GridItemSpan(spanFor(layout)) }) { PdfGridItem(it, ui) }
        LayoutMode.LIST ->
            items(pdfs, key = { it.id }, span = { GridItemSpan(GRID_COLUMNS) }) { PdfListItem(it, ui) }
        LayoutMode.LARGE ->
            items(pdfs, key = { it.id }, span = { GridItemSpan(GRID_COLUMNS) }) { PdfLargeItem(it, ui) }
        LayoutMode.STRIP, LayoutMode.STRIP_COMPACT ->
            item(span = { GridItemSpan(GRID_COLUMNS) }) { PdfStrip(stripTitle, pdfs, ui, stripWidthFor(layout)) }
    }
}
