package com.lucianmarin.num.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.lucianmarin.num.math.DarkSyntaxPalette
import com.lucianmarin.num.math.LightSyntaxPalette
import com.lucianmarin.num.math.SyntaxPalette

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Immutable
data class NumColors(
    val bgApp: Color,
    val bgSurface: Color,
    val bgSubtle: Color,
    val bgHover: Color,
    val borderLight: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentOrange: Color,
    val accentOrangeSoft: Color,
    val accentBlue: Color,
    val accentBlueSoft: Color,
    val accentGreen: Color,
    val errorText: Color,
    val errorBg: Color,
    val syntax: SyntaxPalette,
    val isDark: Boolean
)

val LightNumColors = NumColors(
    bgApp = Color(0xFFF3F4F6),
    bgSurface = Color(0xFFFFFFFF),
    bgSubtle = Color(0xFFF8F9FA),
    bgHover = Color(0xFFEDF0F3),
    borderLight = Color(0xFFE5E7EB),
    borderStrong = Color(0xFFD1D5DB),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF9CA3AF),
    accentOrange = Color(0xFF2563EB),
    accentOrangeSoft = Color(0x142563EB),
    accentBlue = Color(0xFF2563EB),
    accentBlueSoft = Color(0x142563EB),
    accentGreen = Color(0xFF059669),
    errorText = Color(0xFFDC2626),
    errorBg = Color(0x14DC2626),
    syntax = LightSyntaxPalette,
    isDark = false
)

val DarkNumColors = NumColors(
    bgApp = Color(0xFF0F1115),
    bgSurface = Color(0xFF161920),
    bgSubtle = Color(0xFF1E222B),
    bgHover = Color(0xFF262B36),
    borderLight = Color(0xFF262B36),
    borderStrong = Color(0xFF373E4D),
    textPrimary = Color(0xFFF3F4F6),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF6B7280),
    accentOrange = Color(0xFF3B82F6),
    accentOrangeSoft = Color(0x263B82F6),
    accentBlue = Color(0xFF3B82F6),
    accentBlueSoft = Color(0x263B82F6),
    accentGreen = Color(0xFF34D399),
    errorText = Color(0xFFF87171),
    errorBg = Color(0x26F87171),
    syntax = DarkSyntaxPalette,
    isDark = true
)

val LocalNumColors = staticCompositionLocalOf { LightNumColors }

private val LightColorScheme = lightColorScheme(
    primary = LightNumColors.accentOrange,
    secondary = LightNumColors.accentBlue,
    tertiary = LightNumColors.accentGreen,
    background = LightNumColors.bgApp,
    surface = LightNumColors.bgSurface,
    surfaceVariant = LightNumColors.bgSubtle,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightNumColors.textPrimary,
    onSurface = LightNumColors.textPrimary,
    onSurfaceVariant = LightNumColors.textSecondary,
    outline = LightNumColors.borderLight,
    error = LightNumColors.errorText
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkNumColors.accentOrange,
    secondary = DarkNumColors.accentBlue,
    tertiary = DarkNumColors.accentGreen,
    background = DarkNumColors.bgApp,
    surface = DarkNumColors.bgSurface,
    surfaceVariant = DarkNumColors.bgSubtle,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkNumColors.textPrimary,
    onSurface = DarkNumColors.textPrimary,
    onSurfaceVariant = DarkNumColors.textSecondary,
    outline = DarkNumColors.borderLight,
    error = DarkNumColors.errorText
)

@Composable
fun NumTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val numColors = if (useDark) DarkNumColors else LightNumColors
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalNumColors provides numColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
