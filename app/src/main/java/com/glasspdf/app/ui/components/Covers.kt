package com.glasspdf.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasspdf.app.data.AppSettings
import com.glasspdf.app.data.CoverStyle
import com.glasspdf.app.data.PdfEntity
import com.glasspdf.app.data.ThumbnailManager
import com.glasspdf.app.data.title
import com.glasspdf.app.ui.theme.LocalIsDark

val LocalThumbs = staticCompositionLocalOf<ThumbnailManager> { error("ThumbnailManager not provided") }

const val COVER_RATIO = 0.72f

@Composable
fun CoverPlaceholder(title: String, modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    val colors = if (dark) listOf(Color(0xFF3B3487), Color(0xFF1C2A5E)) else listOf(Color(0xFFB9B0FF), Color(0xFF9CC9FF))
    Box(modifier.fillMaxSize().background(Brush.linearGradient(colors)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.PictureAsPdf, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(34.dp))
            Text(
                title.take(2).uppercase(),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** First page (or custom cover) of a PDF, loaded lazily and cached. */
@Composable
fun PdfCoverImage(pdf: PdfEntity, modifier: Modifier = Modifier) {
    val thumbs = LocalThumbs.current
    val bitmap by produceState<ImageBitmap?>(null, pdf.id, pdf.customCoverPath, pdf.uri) {
        value = thumbs.cover(pdf)?.asImageBitmap()
    }
    Crossfade(targetState = bitmap, modifier = modifier, label = "cover") { bmp ->
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CoverPlaceholder(pdf.title)
        }
    }
}

/** Image from a file path (folder covers). Falls back to [fallback]. */
@Composable
fun PathImage(path: String, modifier: Modifier = Modifier, fallback: @Composable () -> Unit = {}) {
    val thumbs = LocalThumbs.current
    val bitmap by produceState<ImageBitmap?>(null, path) { value = thumbs.image(path)?.asImageBitmap() }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        fallback()
    }
}

/** The book-style cover with badges, favourite star and selection marker. */
@Composable
fun PdfCover(
    pdf: PdfEntity,
    settings: AppSettings,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    corner: Dp = 14.dp
) {
    val shape = RoundedCornerShape(corner)
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .aspectRatio(COVER_RATIO)
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        PdfCoverImage(pdf, Modifier.fillMaxSize())
        if (settings.coverStyle == CoverStyle.BOOK) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(11.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.30f), Color.White.copy(alpha = 0.16f), Color.Transparent)))
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.10f))))
        )
        Box(Modifier.fillMaxSize().border(1.dp, Color.White.copy(alpha = 0.22f), shape))

        if (settings.showPageBadge && pdf.pageCount > 0) {
            Text(
                "${pdf.pageCount} p",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        if (pdf.isFavorite) {
            Icon(
                Icons.Rounded.Star, null, tint = Color(0xFFFFD35C),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
            )
        }
        if (pdf.broken) {
            Icon(
                Icons.Rounded.Warning, null, tint = Color(0xFFFF9A9A),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(20.dp)
            )
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(primary.copy(alpha = 0.28f)).border(3.dp, primary, shape))
        }
        if (selectionMode) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) primary else Color.Black.copy(alpha = 0.35f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
