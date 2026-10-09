package com.autocheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.autocheck.app.data.LogLevel
import com.autocheck.app.data.ThemeMode

/** Фирменные цвета: жёлто-оранжевый градиент и дополнительный синий. */
object Brand {
    val Yellow = Color(0xFFFFC21A)
    val Amber = Color(0xFFFFA11A)
    val Orange = Color(0xFFFF6B00)
    val DeepOrange = Color(0xFFF2560A)

    /** Дополнительный цвет. */
    val Blue = Color(0xFF3666FF)
    val BlueLight = Color(0xFF5E8AFF)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFFE65F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3CC),
    onPrimaryContainer = Color(0xFF4A1E00),
    secondary = Brand.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE5FF),
    onSecondaryContainer = Color(0xFF0A2270),
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
    primary = Color(0xFFFFB067),
    onPrimary = Color(0xFF4A2000),
    primaryContainer = Color(0xFF6B3200),
    onPrimaryContainer = Color(0xFFFFE0C7),
    secondary = Color(0xFF8FA9FF),
    onSecondary = Color(0xFF0A1F6B),
    secondaryContainer = Color(0xFF1F3794),
    onSecondaryContainer = Color(0xFFDCE4FF),
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

/** Тёмная ли тема сейчас на самом деле: учитывает и выбор пользователя, и настройку системы. */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Нужна ли тёмная тема при выбранном режиме. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AutoCheckTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}

/** Цвет точки/текста уровня лога. */
@Composable
fun LogLevel.color(): Color {
    val dark = LocalDarkTheme.current
    return when (this) {
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.SUCCESS -> if (dark) Color(0xFF5EDC9A) else Color(0xFF14833F)
        LogLevel.WARNING -> if (dark) Color(0xFFFFC857) else Color(0xFFB36B00)
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }
}
