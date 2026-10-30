package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenDestination
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont

@Composable
fun GolpoGhorBottomBar(
    currentDestination: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = appColors.surface,
        shadowElevation = 8.dp,
        border = BorderStroke(0.5.dp, appColors.border)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                containerColor = appColors.surface,
                contentColor = appColors.text,
                tonalElevation = 0.dp
            ) {
                // 1. Home (আবিষ্কার ও সাম্প্রতিক)
                val isHome = currentDestination is ScreenDestination.Home
                NavigationBarItem(
                    selected = isHome,
                    onClick = { onNavigate(ScreenDestination.Home) },
                    icon = {
                        Icon(
                            imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "হোম",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "হোম",
                            fontFamily = LocalReadingFont.current,
                            fontSize = 11.sp,
                            fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = appColors.primary,
                        selectedTextColor = appColors.primary,
                        indicatorColor = appColors.primary.copy(alpha = 0.15f),
                        unselectedIconColor = appColors.textSecondary,
                        unselectedTextColor = appColors.textSecondary
                    ),
                    modifier = Modifier.testTag("bottom_nav_home")
                )

                // 2. My Library (বুকমার্ক ও অগ্রগতি)
                val isLibrary = currentDestination is ScreenDestination.Library
                NavigationBarItem(
                    selected = isLibrary,
                    onClick = { onNavigate(ScreenDestination.Library) },
                    icon = {
                        Icon(
                            imageVector = if (isLibrary) Icons.Filled.LocalLibrary else Icons.Outlined.LocalLibrary,
                            contentDescription = "লাইব্রেরি",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "লাইব্রেরি",
                            fontFamily = LocalReadingFont.current,
                            fontSize = 11.sp,
                            fontWeight = if (isLibrary) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = appColors.primary,
                        selectedTextColor = appColors.primary,
                        indicatorColor = appColors.primary.copy(alpha = 0.15f),
                        unselectedIconColor = appColors.textSecondary,
                        unselectedTextColor = appColors.textSecondary
                    ),
                    modifier = Modifier.testTag("bottom_nav_library")
                )

                // 3. Stories (গল্প সম্ভার ও অন্বেষণ)
                val isStories = currentDestination is ScreenDestination.Stories
                NavigationBarItem(
                    selected = isStories,
                    onClick = { onNavigate(ScreenDestination.Stories) },
                    icon = {
                        Icon(
                            imageVector = if (isStories) Icons.Filled.AutoStories else Icons.Outlined.AutoStories,
                            contentDescription = "গল্পসমূহ",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "গল্পসমূহ",
                            fontFamily = LocalReadingFont.current,
                            fontSize = 11.sp,
                            fontWeight = if (isStories) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = appColors.primary,
                        selectedTextColor = appColors.primary,
                        indicatorColor = appColors.primary.copy(alpha = 0.15f),
                        unselectedIconColor = appColors.textSecondary,
                        unselectedTextColor = appColors.textSecondary
                    ),
                    modifier = Modifier.testTag("bottom_nav_stories")
                )

                // 4. Reviews (পাঠক রিভিউ)
                val isReviews = currentDestination is ScreenDestination.Reviews ||
                        currentDestination is ScreenDestination.ReviewsManagement
                NavigationBarItem(
                    selected = isReviews,
                    onClick = { onNavigate(ScreenDestination.Reviews) },
                    icon = {
                        Icon(
                            imageVector = if (isReviews) Icons.Filled.RateReview else Icons.Outlined.RateReview,
                            contentDescription = "রিভিউ",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "রিভিউ",
                            fontFamily = LocalReadingFont.current,
                            fontSize = 11.sp,
                            fontWeight = if (isReviews) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = appColors.primary,
                        selectedTextColor = appColors.primary,
                        indicatorColor = appColors.primary.copy(alpha = 0.15f),
                        unselectedIconColor = appColors.textSecondary,
                        unselectedTextColor = appColors.textSecondary
                    ),
                    modifier = Modifier.testTag("bottom_nav_reviews")
                )

                // 5. More (প্রোফাইল, থিম, সেটিংস, অগ্রগতি, ম্যানেজমেন্ট)
                val isMore = currentDestination is ScreenDestination.Settings ||
                        currentDestination is ScreenDestination.Notifications ||
                        currentDestination is ScreenDestination.WriterDashboard
                NavigationBarItem(
                    selected = isMore,
                    onClick = onOpenMore,
                    icon = {
                        Icon(
                            imageVector = if (isMore) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                            contentDescription = "আরও",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "আরও",
                            fontFamily = LocalReadingFont.current,
                            fontSize = 11.sp,
                            fontWeight = if (isMore) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = appColors.primary,
                        selectedTextColor = appColors.primary,
                        indicatorColor = appColors.primary.copy(alpha = 0.15f),
                        unselectedIconColor = appColors.textSecondary,
                        unselectedTextColor = appColors.textSecondary
                    ),
                    modifier = Modifier.testTag("bottom_nav_more")
                )
            }
        }
    }
}
