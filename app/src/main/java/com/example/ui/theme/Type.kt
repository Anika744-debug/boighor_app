package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ReadingFont

val NotoSerifBengali = FontFamily(
    Font(R.font.noto_serif_bengali, FontWeight.Normal),
    Font(R.font.noto_serif_bengali, FontWeight.Medium),
    Font(R.font.noto_serif_bengali, FontWeight.SemiBold),
    Font(R.font.noto_serif_bengali, FontWeight.Bold)
)

val NotoSansBengali = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val HindSiliguri = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri, FontWeight.SemiBold),
    Font(R.font.hind_siliguri, FontWeight.Bold)
)

val TiroBangla = FontFamily(
    Font(R.font.tiro_bangla, FontWeight.Normal),
    Font(R.font.tiro_bangla, FontWeight.Medium),
    Font(R.font.tiro_bangla, FontWeight.SemiBold),
    Font(R.font.tiro_bangla, FontWeight.Bold)
)

fun getFontFamily(font: ReadingFont): FontFamily {
    return when (font) {
        ReadingFont.NOTO_SERIF -> NotoSerifBengali
        ReadingFont.NOTO_SANS -> NotoSansBengali
        ReadingFont.HIND_SILIGURI -> HindSiliguri
        ReadingFont.TIRO_BANGLA -> TiroBangla
        ReadingFont.SYSTEM -> FontFamily.Default
    }
}

val LocalReadingFont = staticCompositionLocalOf {
    NotoSerifBengali
}

fun createAppTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 26.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    )
}

val AppTypography = createAppTypography(NotoSerifBengali)
