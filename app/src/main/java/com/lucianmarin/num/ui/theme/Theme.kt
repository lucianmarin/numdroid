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
    bgApp = Color(0xFFE8E6DF),
    bgSurface = Color(0xFFFFFFFF),
    bgSubtle = Color(0xFFF6F5F1),
    bgHover = Color(0xFFEFEDE7),
    borderLight = Color(0xFFE3E0D7),
    borderStrong = Color(0xFFCDC9BE),
    textPrimary = Color(0xFF1A1B1E),
    textSecondary = Color(0xFF55575D),
    textMuted = Color(0xFF8C8F96),
    accentOrange = Color(0xFFF04E23),
    accentOrangeSoft = Color(0x1AF04E23),
    accentBlue = Color(0xFF4364F7),
    accentBlueSoft = Color(0x1A4364F7),
    accentGreen = Color(0xFF0D7A5F),
    errorText = Color(0xFFD9381E),
    errorBg = Color(0x1AD9381E),
    syntax = LightSyntaxPalette,
    isDark = false
)

val DarkNumColors = NumColors(
    bgApp = Color(0xFF111216),
    bgSurface = Color(0xFF1B1D24),
    bgSubtle = Color(0xFF23262F),
    bgHover = Color(0xFF2B2F3A),
    borderLight = Color(0xFF2B2F3A),
    borderStrong = Color(0xFF3E4352),
    textPrimary = Color(0xFFF0F1F5),
    textSecondary = Color(0xFFA0A5B5),
    textMuted = Color(0xFF666B7A),
    accentOrange = Color(0xFFF2542D),
    accentOrangeSoft = Color(0x26F2542D),
    accentBlue = Color(0xFF5271FF),
    accentBlueSoft = Color(0x265271FF),
    accentGreen = Color(0xFF3DD6AD),
    errorText = Color(0xFFFF6B52),
    errorBg = Color(0x26FF6B52),
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
