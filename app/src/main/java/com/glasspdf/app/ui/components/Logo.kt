package com.glasspdf.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlassPDF mark: a white page with a folded corner and a frosted glass pane layered over it.
 * Same artwork as the launcher icon (108 x 108 design grid).
 */
@Composable
fun AppLogo(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 108f
        // tile
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF2A2A2F), Color(0xFF050506))),
            size = this.size,
            cornerRadius = CornerRadius(this.size.minDimension * 0.28f)
        )
        scale(s, s, pivot = Offset.Zero) {
            // page with cut corner (rounded via stroke join)
            val page = Path().apply {
                moveTo(35f, 31f)
                lineTo(59f, 31f)
                lineTo(71f, 43f)
                lineTo(71f, 75f)
                lineTo(35f, 75f)
                close()
            }
            drawPath(page, Color.White)
            drawPath(page, Color.White, style = Stroke(width = 6f, join = StrokeJoin.Round))
            // fold
            val fold = Path().apply {
                moveTo(59f, 31f)
                lineTo(71f, 43f)
                lineTo(59f, 43f)
                close()
            }
            drawPath(fold, Color(0xFFBDBDC4))
            // text lines
            drawRoundRect(Color(0xFF0A0A0B), Offset(41f, 45f), Size(14f, 3f), CornerRadius(1.5f))
            drawRoundRect(Color(0xFF0A0A0B), Offset(41f, 52f), Size(22f, 3f), CornerRadius(1.5f))
            // glass pane
            drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(28f, 56f), Size(30f, 30f), CornerRadius(6f))
            drawRoundRect(
                Color.White.copy(alpha = 0.70f), Offset(28f, 56f), Size(30f, 30f), CornerRadius(6f),
                style = Stroke(width = 1.6f)
            )
        }
    }
}
