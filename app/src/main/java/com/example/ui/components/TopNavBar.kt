package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ReadingMood
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
    title: String = "গল্পঘর",
    currentUser: User? = null,
    unreadNotificationCount: Int = 0,
    currentMood: ReadingMood = ReadingMood.LIGHT,
    onMoodSelected: (ReadingMood) -> Unit = {},
    showMoodSelector: Boolean = true,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val appColors = LocalAppColors.current
    var moodMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = appColors.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(0.5.dp, appColors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left section: Back button or Logo + Title
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .testTag("top_bar_back_button")
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = appColors.text
                        )
                    }
                } else {
                    // গল্পঘর Logo
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, appColors.border, RoundedCornerShape(8.dp))
                            .background(appColors.background)
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.golpoghor_logo),
                            contentDescription = "গল্পঘর লোগো",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.clickable { onBackClick() }
                ) {
                    Text(
                        text = title,
                        fontFamily = LocalReadingFont.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = appColors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentUser != null) {
                        val roleLabel = when (currentUser.role) {
                            UserRole.ADMIN -> "অ্যাডমিন"
                            UserRole.WRITER -> "লেখক"
                            UserRole.READER -> "পাঠক"
                        }
                        Text(
                            text = "$roleLabel • ${currentUser.displayName}",
                            fontSize = 11.sp,
                            color = appColors.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right section: Adjacent Mood button & Notification Bell (Compact, fits narrow screens)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Reading Mood button
                if (showMoodSelector) {
                    Box {
                        OutlinedButton(
                            onClick = { moodMenuExpanded = true },
                            modifier = Modifier
                                .testTag("top_bar_mood_button")
                                .height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = appColors.text
                            ),
                            border = BorderStroke(1.dp, appColors.border)
                        ) {
                            // Small colored indicator circle for current mood
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (currentMood) {
                                            ReadingMood.LIGHT -> Color.White
                                            ReadingMood.DARK -> Color.Black
                                            ReadingMood.SEPIA -> Color(0xFFF4ECD8)
                                            ReadingMood.WALNUT -> Color(0xFF8B6248)
                                        }
                                    )
                                    .border(0.5.dp, Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = currentMood.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Mood selection dropdown menu (Exactly 4 moods with distinct color previews)
                        DropdownMenu(
                            expanded = moodMenuExpanded,
                            onDismissRequest = { moodMenuExpanded = false },
                            modifier = Modifier.background(appColors.surface)
                        ) {
                            Text(
                                text = "পড়ার আবহাওয়া (Reading Mood)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            HorizontalDivider(color = appColors.border)

                            ReadingMood.entries.forEach { mood ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (mood) {
                                                                ReadingMood.LIGHT -> Color.White
                                                                ReadingMood.DARK -> Color.Black
                                                                ReadingMood.SEPIA -> Color(0xFFF4ECD8)
                                                                ReadingMood.WALNUT -> Color(0xFF8B6248)
                                                            }
                                                        )
                                                        .border(0.5.dp, Color.Gray, CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = mood.displayName,
                                                    fontSize = 14.sp,
                                                    color = appColors.text,
                                                    fontWeight = if (mood == currentMood) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                            if (mood == currentMood) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = "নির্বাচিত",
                                                    tint = appColors.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onMoodSelected(mood)
                                        moodMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Notification Bell with Badge (Adjacent to Mood button)
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(
                                containerColor = appColors.primary,
                                contentColor = appColors.onPrimary
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .testTag("top_bar_notification_button")
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "বিজ্ঞপ্তি",
                            tint = appColors.text
                        )
                    }
                }
            }
        }
    }
}
