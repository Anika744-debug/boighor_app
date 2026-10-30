package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.model.AppTheme
import com.example.data.model.ReadingFont
import com.example.data.model.ReadingMood

val LocalAppColors = staticCompositionLocalOf {
    getColorsForTheme(AppTheme.BADAMI_BROWN, false)
}

@Composable
fun GolpoGhorTheme(
    appTheme: AppTheme = AppTheme.BADAMI_BROWN,
    isDarkMode: Boolean = false,
    readingMood: ReadingMood = ReadingMood.LIGHT,
    readingFont: ReadingFont = ReadingFont.NOTO_SERIF,
    content: @Composable () -> Unit
) {
    // App theme colors adapt specifically to the selected theme in light or dark mode.
    // Reading mood remains strictly independent and controls only the reading screen.
    val colors = getColorsForTheme(appTheme, isDarkMode)
    val fontFamily = getFontFamily(readingFont)
    val typography = createAppTypography(fontFamily)
    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.card,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.card,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalReadingFont provides fontFamily
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

