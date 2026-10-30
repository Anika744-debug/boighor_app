package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppColors

/**
 * Converts integers to Bengali numerals (e.g. 45 -> ৪৫)
 */
fun toBengaliDigits(num: Int): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return num.toString().map { if (it in '0'..'9') bnDigits[it - '0'] else it }.joinToString("")
}

@Composable
fun ReadingProgressBar(
    progressFraction: Float,
    chapterLabel: String,
    surfaceColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        label = "reading_progress_anim"
    )
    val percentInt = (progressFraction.coerceIn(0f, 1f) * 100).toInt()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Continuous smooth progress indicator line
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = appColors.primary,
            trackColor = borderColor.copy(alpha = 0.25f)
        )

        // Reading info & progress badge bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor.copy(alpha = 0.95f),
            border = BorderStroke(0.5.dp, borderColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = appColors.primary
                    )
                    Text(
                        text = chapterLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                }

                // Bengali percentage progress badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = appColors.primary.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, appColors.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "পড়ার অগ্রগতি: ${toBengaliDigits(percentInt)}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary
                        )
                    }
                }
            }
        }
    }
}
