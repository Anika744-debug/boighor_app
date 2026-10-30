package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppColors

@Composable
fun BengaliEditorToolbar(
    onInsertSymbol: (String) -> Unit
) {
    val appColors = LocalAppColors.current
    val symbols = listOf(
        "।" to "দাঁড়ি",
        " — " to "ড্যাশ",
        "“" to "উদ্ধৃতি শুরু",
        "”" to "উদ্ধৃতি শেষ",
        "‘" to "একক উদ্ধৃতি",
        "’" to "একক উদ্ধৃতি",
        "্" to "হসন্ত",
        "ৎ" to "খণ্ড-ত",
        "ং" to "অনুস্বার",
        "ঃ" to "বিসর্গ",
        "ঁ" to "চন্দ্রবিন্দু",
        "…" to "ইলিপসিস",
        "?" to "প্রশ্নবোধক",
        "!" to "বিস্ময়বোধক",
        "\n\n" to "নতুন প্যারাগ্রাফ"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = appColors.card,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "দ্রুত চিহ্ন:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textSecondary,
                modifier = Modifier.padding(end = 4.dp)
            )

            symbols.forEach { (sym, desc) ->
                FilledTonalButton(
                    onClick = { onInsertSymbol(sym) },
                    modifier = Modifier
                        .testTag("insert_symbol_${desc}")
                        .height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = appColors.surface,
                        contentColor = appColors.text
                    )
                ) {
                    Text(
                        text = if (sym == "\n\n") "¶ প্যারা" else sym,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
