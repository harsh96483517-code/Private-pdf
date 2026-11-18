package com.glasspdf.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val PillShape = RoundedCornerShape(50)
val CardShape = RoundedCornerShape(28.dp)

/** Soft gradient with colour blobs, so translucent glass surfaces have something to refract. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val dark = LocalIsDark.current
    val base = if (dark) listOf(Color(0xFF0A0B18), Color(0xFF14153A)) else listOf(Color(0xFFEFF2FF), Color(0xFFF8F1FF))
    val c1 = if (dark) Color(0xFF7C5CFF).copy(alpha = 0.42f) else Color(0xFF9DB0FF).copy(alpha = 0.70f)
    val c2 = if (dark) Color(0xFF2D9CFF).copy(alpha = 0.28f) else Color(0xFFFFB8DC).copy(alpha = 0.60f)
    val c3 = if (dark) Color(0xFFFF5CA8).copy(alpha = 0.20f) else Color(0xFF9EE2FF).copy(alpha = 0.60f)
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(base))
            .drawBehind {
                val w = size.width
                val h = size.height
                fun blob(color: Color, cx: Float, cy: Float, r: Float) {
                    val center = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = r),
                        radius = r,
                        center = center
                    )
                }
                blob(c1, w * 0.12f, h * 0.10f, w * 0.95f)
                blob(c2, w * 0.95f, h * 0.45f, w * 0.85f)
                blob(c3, w * 0.20f, h * 0.88f, w * 0.90f)
            },
        content = content
    )
}

/** Frosted-glass look: translucent gradient fill + specular border. */
@Composable
fun Modifier.glass(shape: Shape = CardShape, strong: Boolean = false): Modifier {
    val dark = LocalIsDark.current
    val topFill = if (dark) Color.White.copy(alpha = if (strong) 0.20f else 0.13f) else Color.White.copy(alpha = if (strong) 0.78f else 0.58f)
    val bottomFill = if (dark) Color.White.copy(alpha = if (strong) 0.09f else 0.05f) else Color.White.copy(alpha = if (strong) 0.52f else 0.34f)
    val borderTop = if (dark) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.95f)
    val borderBottom = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.40f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(topFill, bottomFill)))
        .border(1.dp, Brush.verticalGradient(listOf(borderTop, borderBottom)), shape)
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bouncyClick(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    strong: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(modifier.glass(shape, strong)) { content() }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    selected: Boolean = false
) {
    val bg = if (selected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), CircleShape) else Modifier
    Box(
        modifier
            .size(size)
            .glass(CircleShape, strong = true)
            .then(bg)
            .bouncyClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun GlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .glass(PillShape)
            .then(if (selected) Modifier.background(primary.copy(alpha = 0.30f)) else Modifier)
            .bouncyClick(onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Accent pill (used for the floating Auto Arrange action). */
@Composable
fun AccentPill(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val a = MaterialTheme.colorScheme.primary
    val b = MaterialTheme.colorScheme.secondary
    Row(
        modifier
            .clip(PillShape)
            .background(Brush.horizontalGradient(listOf(a, b)))
            .border(1.dp, Color.White.copy(alpha = 0.45f), PillShape)
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun <T> SegmentedPills(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            GlassPill(text = label(o), onClick = { onSelect(o) }, selected = o == selected, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun glassBorder(): BorderStroke {
    val dark = LocalIsDark.current
    return BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.8f))
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(84.dp).glass(CircleShape, strong = true), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) action()
    }
}
