package com.autocheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.autocheck.app.data.LogLevel

private val Indigo = Color(0xFF4F5BD5)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E4FF),
    onPrimaryContainer = Color(0xFF151C6B),
    secondary = Color(0xFF0E8F7E),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF14161F),
    surface = Color(0xFFF5F6FA),
    onSurface = Color(0xFF14161F),
    surfaceVariant = Color(0xFFE8EAF3),
    onSurfaceVariant = Color(0xFF50556B),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF0F8),
    outlineVariant = Color(0xFFD5D8E6),
    error = Color(0xFFC62840),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AA5FF),
    onPrimary = Color(0xFF0C1245),
    primaryContainer = Color(0xFF2B3482),
    onPrimaryContainer = Color(0xFFE0E3FF),
    secondary = Color(0xFF5EDCC8),
    background = Color(0xFF0E1016),
    onBackground = Color(0xFFE6E8F2),
    surface = Color(0xFF0E1016),
    onSurface = Color(0xFFE6E8F2),
    surfaceVariant = Color(0xFF232736),
    onSurfaceVariant = Color(0xFFA7ACC2),
    surfaceContainer = Color(0xFF171A24),
    surfaceContainerHigh = Color(0xFF1F2330),
    outlineVariant = Color(0xFF2E3345),
    error = Color(0xFFFF7A8A),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)

@Composable
fun AutoCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

/** Цвет точки/текста уровня лога. */
@Composable
fun LogLevel.color(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.SUCCESS -> if (dark) Color(0xFF5EDC9A) else Color(0xFF14833F)
        LogLevel.WARNING -> if (dark) Color(0xFFFFC857) else Color(0xFFB36B00)
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }
}
