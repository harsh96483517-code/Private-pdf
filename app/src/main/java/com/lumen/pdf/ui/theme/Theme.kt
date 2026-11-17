package com.lumen.pdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lumen.pdf.data.ThemeMode

val LocalLumenDark = staticCompositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF4B6BFB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E8FF),
    onPrimaryContainer = Color(0xFF0E1B4D),
    secondary = Color(0xFF7A5AF8),
    onSecondary = Color.White,
    tertiary = Color(0xFF17B8A6),
    background = Color(0xFFF3F5FA),
    onBackground = Color(0xFF101322),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101322),
    surfaceVariant = Color(0xFFEDF0F8),
    onSurfaceVariant = Color(0xFF5A6178),
    outline = Color(0xFFC9CFDE),
    error = Color(0xFFD64545)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB1FF),
    onPrimary = Color(0xFF0A1030),
    primaryContainer = Color(0xFF243166),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFFB9A6FF),
    onSecondary = Color(0xFF170B3C),
    tertiary = Color(0xFF57DCC9),
    background = Color(0xFF0B0D12),
    onBackground = Color(0xFFEEF1F8),
    surface = Color(0xFF151A24),
    onSurface = Color(0xFFEEF1F8),
    surfaceVariant = Color(0xFF1D2432),
    onSurfaceVariant = Color(0xFFA3ACC2),
    outline = Color(0xFF2C3444),
    error = Color(0xFFFF8A8A)
)

private val LumenTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif, lineHeight = 20.sp),
        labelMedium = base.labelMedium.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.3.sp)
    )
}

@Composable
fun LumenTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalLumenDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = LumenTypography,
            content = content
        )
    }
}
