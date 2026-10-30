package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AppTheme
import com.example.data.model.ReadingMood

// Badami Brown (Default)
val BadamiBrownPrimary = Color(0xFF8B6248)
val BadamiBrownBg = Color(0xFFFAF7F0)
val BadamiBrownSurface = Color(0xFFFFFFFF)
val BadamiBrownCard = Color(0xFFF5EFEB)
val BadamiBrownText = Color(0xFF2D231B)
val BadamiBrownBorder = Color(0xFFE5DACF)
val BadamiBrownAccent = Color(0xFFA67858)

data class AppThemeColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val isDark: Boolean = false
)

fun getColorsForTheme(theme: AppTheme, isDarkMode: Boolean = false): AppThemeColors {
    return if (isDarkMode) {
        when (theme) {
            // 1. Badami Brown: Warm dark brown surfaces, deep coffee-colored backgrounds, muted brown borders, warm off-white text, and brown accent
            AppTheme.BADAMI_BROWN -> AppThemeColors(
                background = Color(0xFF1C1511),
                surface = Color(0xFF271F19),
                card = Color(0xFF332922),
                primary = Color(0xFFA67858),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFFF5EDE4),
                textSecondary = Color(0xFFB5A396),
                border = Color(0xFF43352B),
                isDark = true
            )
            // 2. Pure White: Neutral charcoal backgrounds, dark-gray cards, soft white text, and restrained neutral accents
            AppTheme.PURE_WHITE -> AppThemeColors(
                background = Color(0xFF131518),
                surface = Color(0xFF1E2126),
                card = Color(0xFF272B32),
                primary = Color(0xFF8B9BB4),
                onPrimary = Color(0xFF111827),
                text = Color(0xFFEDEDF2),
                textSecondary = Color(0xFFA2A8B5),
                border = Color(0xFF353B45),
                isDark = true
            )
            // 3. White & Red: Deep charcoal backgrounds with controlled red accents. Keep red buttons and indicators readable
            AppTheme.WHITE_AND_RED -> AppThemeColors(
                background = Color(0xFF171314),
                surface = Color(0xFF221A1D),
                card = Color(0xFF2D2327),
                primary = Color(0xFFE53E3E),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFFF7FAFC),
                textSecondary = Color(0xFFA8B2C1),
                border = Color(0xFF4A2B33),
                isDark = true
            )
            // 4. Blue Sky: Deep navy and slate-blue surfaces with soft blue accents
            AppTheme.BLUE_SKY -> AppThemeColors(
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B),
                card = Color(0xFF28364D),
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF0F172A),
                text = Color(0xFFF1F5F9),
                textSecondary = Color(0xFF94A3B8),
                border = Color(0xFF334155),
                isDark = true
            )
            // 5. Forest Green: Deep forest-green backgrounds, muted green surfaces, and readable sage-green accents
            AppTheme.FOREST_GREEN -> AppThemeColors(
                background = Color(0xFF0D1910),
                surface = Color(0xFF16271A),
                card = Color(0xFF203525),
                primary = Color(0xFF4ADE80),
                onPrimary = Color(0xFF0D1910),
                text = Color(0xFFF0FDF4),
                textSecondary = Color(0xFF92AB96),
                border = Color(0xFF2C4533),
                isDark = true
            )
            // 6. Lavender Dream: Deep charcoal with subtle plum and muted lavender accents. Avoid excessive purple brightness
            AppTheme.LAVENDER_DREAM -> AppThemeColors(
                background = Color(0xFF15121C),
                surface = Color(0xFF211B2C),
                card = Color(0xFF2C243A),
                primary = Color(0xFFB39DDB),
                onPrimary = Color(0xFF15121C),
                text = Color(0xFFF4EFFC),
                textSecondary = Color(0xFFA598B8),
                border = Color(0xFF3F3252),
                isDark = true
            )
            // 7. Olive Moss: Dark olive-charcoal backgrounds with muted moss-green surfaces and subdued olive accents
            AppTheme.OLIVE_MOSS -> AppThemeColors(
                background = Color(0xFF161912),
                surface = Color(0xFF20251B),
                card = Color(0xFF2B3224),
                primary = Color(0xFF9CB46E),
                onPrimary = Color(0xFF161912),
                text = Color(0xFFF2F4EB),
                textSecondary = Color(0xFFA3AB94),
                border = Color(0xFF3B4432),
                isDark = true
            )
        }
    } else {
        when (theme) {
            AppTheme.BADAMI_BROWN -> AppThemeColors(
                background = Color(0xFFFAF7F0),
                surface = Color(0xFFFFFFFF),
                card = Color(0xFFF5EFEB),
                primary = Color(0xFF8B6248),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFF2D231B),
                textSecondary = Color(0xFF7A6B60),
                border = Color(0xFFE5DACF),
                isDark = false
            )
            AppTheme.PURE_WHITE -> AppThemeColors(
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFF9FAFB),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFF2B2D42),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFF111827),
                textSecondary = Color(0xFF6B7280),
                border = Color(0xFFE5E7EB),
                isDark = false
            )
            AppTheme.WHITE_AND_RED -> AppThemeColors(
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFFFF5F5),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFFC53030),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFF2D3748),
                textSecondary = Color(0xFF718096),
                border = Color(0xFFFED7D7),
                isDark = false
            )
            AppTheme.BLUE_SKY -> AppThemeColors(
                background = Color(0xFFF0F7FF),
                surface = Color(0xFFFFFFFF),
                card = Color(0xFFF4F9FF),
                primary = Color(0xFF1E88E5),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFF1A202C),
                textSecondary = Color(0xFF5A6E85),
                border = Color(0xFFD0E3F7),
                isDark = false
            )
            AppTheme.FOREST_GREEN -> AppThemeColors(
                background = Color(0xFFF2F7F2),
                surface = Color(0xFFFFFFFF),
                card = Color(0xFFF5FAF5),
                primary = Color(0xFF2E7D32),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFF1B2E1C),
                textSecondary = Color(0xFF536E55),
                border = Color(0xFFD5E5D5),
                isDark = false
            )
            AppTheme.LAVENDER_DREAM -> AppThemeColors(
                background = Color(0xFFF7F5FC),
                surface = Color(0xFFFFFFFF),
                card = Color(0xFFFAF8FE),
                primary = Color(0xFF6A4C93),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFF2E1F47),
                textSecondary = Color(0xFF6E6083),
                border = Color(0xFFE8E1F5),
                isDark = false
            )
            AppTheme.OLIVE_MOSS -> AppThemeColors(
                background = Color(0xFFF5F2E9), // Strictly preserved #F5F2E9 as mandated
                surface = Color(0xFFFAF8F2),
                card = Color(0xFFEDE9DE),
                primary = Color(0xFF556B2F),
                onPrimary = Color(0xFFFFFFFF),
                text = Color(0xFF2C351B),
                textSecondary = Color(0xFF616B4F),
                border = Color(0xFFDDD8CA),
                isDark = false
            )
        }
    }
}

data class ReadingMoodColors(
    val background: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val highlight: Color,
    val border: Color
)

fun getColorsForMood(mood: ReadingMood): ReadingMoodColors {
    return when (mood) {
        ReadingMood.LIGHT -> ReadingMoodColors(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF8F9FA),
            text = Color(0xFF1A1A1A),
            textSecondary = Color(0xFF555555),
            highlight = Color(0xFFFFF3C4),
            border = Color(0xFFE9ECEF)
        )
        ReadingMood.DARK -> ReadingMoodColors(
            background = Color(0xFF181818),
            surface = Color(0xFF222222),
            text = Color(0xFFE6E6E6),
            textSecondary = Color(0xFFA0A0A0),
            highlight = Color(0xFF42371E),
            border = Color(0xFF333333)
        )
        ReadingMood.SEPIA -> ReadingMoodColors(
            // Sepia: Background #F4ECD8, text #433629, secondary text #796A56, highlight #C4A77D
            background = Color(0xFFF4ECD8),
            surface = Color(0xFFEDE3CB),
            text = Color(0xFF433629),
            textSecondary = Color(0xFF796A56),
            highlight = Color(0xFFC4A77D),
            border = Color(0xFFDED2B9)
        )
        ReadingMood.WALNUT -> ReadingMoodColors(
            // Walnut: Warm rich walnut-brown palette, main brown #8B6248
            background = Color(0xFF251D18),
            surface = Color(0xFF362B24),
            text = Color(0xFFF2E9E4),
            textSecondary = Color(0xFFB8A499),
            highlight = Color(0xFF8B6248),
            border = Color(0xFF483930)
        )
    }
}

fun getEffectiveAppColors(theme: AppTheme, mood: ReadingMood): AppThemeColors {
    return when (mood) {
        ReadingMood.DARK -> AppThemeColors(
            background = Color(0xFF141414),
            surface = Color(0xFF1E1E1E),
            card = Color(0xFF262626),
            primary = Color(0xFFA67858),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFFE8E8E8),
            textSecondary = Color(0xFFA8A8A8),
            border = Color(0xFF383838)
        )
        ReadingMood.SEPIA -> AppThemeColors(
            background = Color(0xFFF4ECD8),
            surface = Color(0xFFEDE3CB),
            card = Color(0xFFE5DAC0),
            primary = Color(0xFF7A5438),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFF3E3125),
            textSecondary = Color(0xFF6E5D4E),
            border = Color(0xFFDCD0B5)
        )
        ReadingMood.WALNUT -> AppThemeColors(
            background = Color(0xFF251D18),
            surface = Color(0xFF322721),
            card = Color(0xFF3D312A),
            primary = Color(0xFFA67858),
            onPrimary = Color(0xFFFFFFFF),
            text = Color(0xFFEADCCF),
            textSecondary = Color(0xFFB8A697),
            border = Color(0xFF4A3C34)
        )
        ReadingMood.LIGHT -> getColorsForTheme(theme)
    }
}

