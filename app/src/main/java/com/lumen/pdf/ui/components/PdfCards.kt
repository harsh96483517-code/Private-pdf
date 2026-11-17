package com.lumen.pdf.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lumen.pdf.data.PdfFile
import com.lumen.pdf.ui.icons.LIcon
import java.io.File

data class PdfActions(
    val onOpen: (PdfFile) -> Unit = {},
    val onRename: (PdfFile) -> Unit = {},
    val onMove: (PdfFile) -> Unit = {},
    val onDelete: (PdfFile) -> Unit = {},
    val onInfo: (PdfFile) -> Unit = {},
    val onCover: (PdfFile) -> Unit = {},
    val onRestoreCover: (PdfFile) -> Unit = {},
    val onFavorite: (PdfFile) -> Unit = {},
    val onToggleSelect: (PdfFile) -> Unit = {}
)

@Composable
fun CoverImage(
    coverPath: String?,
    modifier: Modifier = Modifier,
    corner: Int = 20
) {
    val shape = RoundedCornerShape(corner.dp)
    val file = coverPath?.let { File(it) }
    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                LIcon.PictureAsPdf,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfGridCard(
    pdf: PdfFile,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    actions: PdfActions = PdfActions()
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    Column(modifier = modifier) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(shape)
                    .background(glassFill(), shape)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        brush = if (selected) {
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        } else glassBorder(),
                        shape = shape
                    )
                    .combinedClickable(
                        onClick = {
                            if (selectionMode) actions.onToggleSelect(pdf) else actions.onOpen(pdf)
                        },
                        onLongClick = { actions.onToggleSelect(pdf) }
                    )
                    .padding(5.dp)
            ) {
                CoverImage(pdf.coverPath, Modifier.fillMaxSize(), corner = 18)
                if (pdf.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            LIcon.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD86B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        LIcon.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            LIcon.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            PdfMenu(pdf, menuOpen, { menuOpen = false }, actions)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            pdf.title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 3.dp)
        )
        if (pdf.pageCount > 0) {
            Text(
                "${pdf.pageCount} pages",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfListRow(
    pdf: PdfFile,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    actions: PdfActions = PdfActions()
) {
    var menuOpen by remember { mutableStateOf(false) }
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) actions.onToggleSelect(pdf) else actions.onOpen(pdf) },
                onLongClick = { actions.onToggleSelect(pdf) }
            ),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverImage(
                pdf.coverPath,
                Modifier
                    .width(56.dp)
                    .height(74.dp),
                corner = 14
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pdf.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    buildString {
                        append("${pdf.pageCount} pages")
                        if (pdf.size > 0) append(" · ${formatSize(pdf.size)}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                GlassIconButton(
                    icon = LIcon.MoreVert,
                    contentDescription = "More",
                    onClick = { menuOpen = true },
                    size = 36
                )
                PdfMenu(pdf, menuOpen, { menuOpen = false }, actions)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfStripCard(
    pdf: PdfFile,
    modifier: Modifier = Modifier,
    cardWidth: Int = 118,
    actions: PdfActions = PdfActions()
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier = modifier.width(cardWidth.dp)) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(glassFill(), RoundedCornerShape(18.dp))
                    .border(1.dp, glassBorder(), RoundedCornerShape(18.dp))
                    .combinedClickable(
                        onClick = { actions.onOpen(pdf) },
                        onLongClick = { menuOpen = true }
                    )
                    .padding(4.dp)
            ) {
                CoverImage(pdf.coverPath, Modifier.fillMaxSize(), corner = 15)
            }
            PdfMenu(pdf, menuOpen, { menuOpen = false }, actions)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            pdf.title,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun PdfMenu(
    pdf: PdfFile,
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: PdfActions
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Open") },
            leadingIcon = { Icon(LIcon.PictureAsPdf, null) },
            onClick = { onDismiss(); actions.onOpen(pdf) }
        )
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(LIcon.Rename, null) },
            onClick = { onDismiss(); actions.onRename(pdf) }
        )
        DropdownMenuItem(
            text = { Text("Move to folder") },
            leadingIcon = { Icon(LIcon.FolderOpen, null) },
            onClick = { onDismiss(); actions.onMove(pdf) }
        )
        DropdownMenuItem(
            text = { Text(if (pdf.isFavorite) "Remove favorite" else "Add to favorites") },
            leadingIcon = {
                Icon(if (pdf.isFavorite) LIcon.Star else LIcon.StarBorder, null)
            },
            onClick = { onDismiss(); actions.onFavorite(pdf) }
        )
        DropdownMenuItem(
            text = { Text("Set cover from gallery") },
            leadingIcon = { Icon(LIcon.Image, null) },
            onClick = { onDismiss(); actions.onCover(pdf) }
        )
        DropdownMenuItem(
            text = { Text("Restore first page cover") },
            leadingIcon = { Icon(LIcon.Refresh, null) },
            onClick = { onDismiss(); actions.onRestoreCover(pdf) }
        )
        DropdownMenuItem(
            text = { Text("File information") },
            leadingIcon = { Icon(LIcon.Info, null) },
            onClick = { onDismiss(); actions.onInfo(pdf) }
        )
        DropdownMenuItem(
            text = { Text("Remove from library") },
            leadingIcon = { Icon(LIcon.Delete, null) },
            onClick = { onDismiss(); actions.onDelete(pdf) }
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = LIcon.Info,
    action: @Composable () -> Unit = {}
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        contentPadding = PaddingValues(26.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.toInt()} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}

fun formatDate(millis: Long): String {
    if (millis <= 0) return "—"
    val format = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
    return format.format(java.util.Date(millis))
}
