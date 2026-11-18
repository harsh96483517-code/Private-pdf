package com.glasspdf.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalIsDark = staticCompositionLocalOf { true }

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9D8DFF),
    onPrimary = Color(0xFF120E3A),
    primaryContainer = Color(0xFF2E2A6B),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF6CC4FF),
    onSecondary = Color(0xFF00243A),
    tertiary = Color(0xFFFF8CC6),
    background = Color(0xFF0A0B18),
    onBackground = Color(0xFFEDEBFF),
    surface = Color(0xFF171934),
    onSurface = Color(0xFFEDEBFF),
    surfaceVariant = Color(0xFF24274F),
    onSurfaceVariant = Color(0xFFB9B7D9),
    surfaceContainer = Color(0xFF1B1D3C),
    surfaceContainerHigh = Color(0xFF212446),
    surfaceContainerHighest = Color(0xFF292C52),
    outline = Color(0xFF6E6C9A),
    outlineVariant = Color(0xFF3A3D68),
    error = Color(0xFFFF8A8A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BEA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3DFFF),
    onPrimaryContainer = Color(0xFF1B1360),
    secondary = Color(0xFF1C8DDB),
    onSecondary = Color.White,
    tertiary = Color(0xFFD6408F),
    background = Color(0xFFEFF2FF),
    onBackground = Color(0xFF16152E),
    surface = Color(0xFFF7F6FF),
    onSurface = Color(0xFF16152E),
    surfaceVariant = Color(0xFFE4E2F7),
    onSurfaceVariant = Color(0xFF4B4A6E),
    surfaceContainer = Color(0xFFF1EFFE),
    surfaceContainerHigh = Color(0xFFEAE8FC),
    surfaceContainerHighest = Color(0xFFE3E1F8),
    outline = Color(0xFF8A88AE),
    outlineVariant = Color(0xFFC9C7E4),
    error = Color(0xFFC7324A)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun GlassPdfTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
