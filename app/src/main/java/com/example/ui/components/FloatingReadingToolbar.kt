package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppColors

@Composable
fun FloatingReadingToolbar(
    isVisible: Boolean,
    fontSize: Float,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    isPageTurnMode: Boolean,
    onTogglePageTurnMode: () -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onOpenReadingSettings: () -> Unit,
    onExportPdf: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onOpenReviews: () -> Unit,
    reviewCount: Int = 0
) {
    val appColors = LocalAppColors.current

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .border(1.dp, appColors.border, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = appColors.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dedicated, Spacious Font Size Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = appColors.card,
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = onDecreaseFontSize,
                            modifier = Modifier
                                .testTag("decrease_font_size_button")
                                .size(40.dp)
                        ) {
                            Text(
                                text = "অ-",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.text
                            )
                        }

                        Text(
                            text = "${fontSize.toInt()}sp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = onIncreaseFontSize,
                            modifier = Modifier
                                .testTag("increase_font_size_button")
                                .size(40.dp)
                        ) {
                            Text(
                                text = "অ+",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = appColors.primary
                            )
                        }
                    }
                }

                // Page Turn Mode / Vertical Scroll toggle
                IconButton(
                    onClick = onTogglePageTurnMode,
                    modifier = Modifier
                        .testTag("toggle_page_turn_button")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPageTurnMode) Icons.Default.MenuBook else Icons.Default.VerticalAlignBottom,
                        contentDescription = if (isPageTurnMode) "পৃষ্ঠা উল্টানো মোড" else "স্ক্রোল মোড",
                        tint = if (isPageTurnMode) appColors.primary else appColors.textSecondary
                    )
                }

                // Bookmark toggle
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .testTag("toggle_bookmark_button")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "বুকমার্ক",
                        tint = if (isBookmarked) appColors.primary else appColors.textSecondary
                    )
                }

                // Reviews & Comments sheet toggle
                BadgedBox(
                    badge = {
                        if (reviewCount > 0) {
                            Badge(
                                containerColor = appColors.primary,
                                contentColor = appColors.onPrimary
                            ) {
                                Text(reviewCount.toString(), fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = onOpenReviews,
                        modifier = Modifier
                            .testTag("open_reviews_button")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RateReview,
                            contentDescription = "রিভিউ ও মন্তব্য",
                            tint = appColors.textSecondary
                        )
                    }
                }

                // PDF Export button (Direct download)
                IconButton(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .testTag("export_pdf_button")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "পিডিএফ ডাউনলোড",
                        tint = appColors.primary
                    )
                }

                // Reading Settings (Fonts, Spacing, Mood)
                IconButton(
                    onClick = onOpenReadingSettings,
                    modifier = Modifier
                        .testTag("reading_settings_button")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "পড়ার সেটিংস",
                        tint = appColors.textSecondary
                    )
                }
            }
        }
    }
}
