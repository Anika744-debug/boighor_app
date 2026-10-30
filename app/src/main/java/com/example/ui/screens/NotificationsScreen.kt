package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationType
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.TopNavBar
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val currentUser by viewModel.currentUser.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var filterType by remember { mutableStateOf("ALL") } // "ALL", "UNREAD", "READ"
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val filteredNotifications = remember(notifications, filterType) {
        when (filterType) {
            "UNREAD" -> notifications.filter { !it.isRead }
            "READ" -> notifications.filter { it.isRead }
            else -> notifications
        }
    }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "বিজ্ঞপ্তি",
                currentUser = currentUser,
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(ScreenDestination.Home) },
                currentMood = readingMood,
                onMoodSelected = { viewModel.updateMood(it) },
                showMoodSelector = true,
                onSettingsClick = { viewModel.navigateTo(ScreenDestination.Settings) },
                onNotificationClick = {},
                onProfileClick = { viewModel.navigateTo(ScreenDestination.Login) }
            )
        },
        containerColor = appColors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Action Bar: Filter tabs & Mark all read / Delete all
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সকল বিজ্ঞপ্তি (${notifications.size})",
                        fontFamily = LocalReadingFont.current,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.markAllNotificationsRead()
                                Toast.makeText(context, "সকল বিজ্ঞপ্তি পঠিত হিসেবে চিহ্নিত", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("সব পঠিত", fontSize = 12.sp, color = appColors.primary)
                        }

                        if (notifications.isNotEmpty()) {
                            TextButton(
                                onClick = { showDeleteAllDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("সব মুছুন", fontSize = 12.sp, color = Color.Red)
                            }
                        }
                    }
                }
            }

            // Filter Chips (All, Unread, Read)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { filterType = "ALL" },
                        label = { Text("সকল (${notifications.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = filterType == "UNREAD",
                        onClick = { filterType = "UNREAD" },
                        label = { Text("অপঠিত (${notifications.count { !it.isRead }})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = filterType == "READ",
                        onClick = { filterType = "READ" },
                        label = { Text("পঠিত (${notifications.count { it.isRead }})", fontSize = 12.sp) }
                    )
                }
            }

            // Notifications List
            if (filteredNotifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো বিজ্ঞপ্তি নেই।",
                            color = appColors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredNotifications) { item ->
                    NotificationCard(
                        item = item,
                        onClick = {
                            coroutineScope.launch {
                                viewModel.repository.updateNotificationRead(item, true)
                            }
                            if (item.storyId != null && item.partId != null) {
                                viewModel.navigateTo(ScreenDestination.Reading(item.storyId, item.partId))
                            }
                        },
                        onToggleRead = {
                            coroutineScope.launch {
                                viewModel.repository.updateNotificationRead(item, !item.isRead)
                            }
                        },
                        onDelete = {
                            viewModel.deleteNotification(item.id)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("সকল বিজ্ঞপ্তি মুছে ফেলুন", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিতভাবে আপনার সকল বিজ্ঞপ্তি মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllNotifications()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("সব মুছুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("বাতিল")
                }
            },
            containerColor = appColors.surface
        )
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    onDelete: () -> Unit
) {
    val appColors = LocalAppColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("notification_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) appColors.primary.copy(alpha = 0.08f) else appColors.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!item.isRead) appColors.primary.copy(alpha = 0.4f) else appColors.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.type) {
                            NotificationType.REPLY_ON_REVIEW -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                            NotificationType.NEW_PART_PUBLISHED -> Color(0xFF10B981).copy(alpha = 0.2f)
                            NotificationType.PART_UNLOCKED -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> appColors.primary.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.type) {
                        NotificationType.REPLY_ON_REVIEW -> Icons.Default.Chat
                        NotificationType.NEW_PART_PUBLISHED -> Icons.Default.AutoStories
                        NotificationType.PART_UNLOCKED -> Icons.Default.LockOpen
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = appColors.primary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = appColors.text
                    )

                    val dateStr = SimpleDateFormat("d MMM, hh:mm a", Locale("bn")).format(Date(item.createdAt))
                    Text(text = dateStr, fontSize = 10.sp, color = appColors.textSecondary)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = appColors.text,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onToggleRead,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (item.isRead) "অপঠিত করুন" else "পঠিত চিহ্নিত",
                            fontSize = 11.sp,
                            color = appColors.primary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "মুছুন",
                            modifier = Modifier.size(16.dp),
                            tint = appColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
