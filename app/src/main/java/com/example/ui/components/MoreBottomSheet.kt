package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppTheme
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont
import com.example.ui.theme.getColorsForTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreBottomSheet(
    viewModel: MainViewModel,
    currentUser: User?,
    onDismiss: () -> Unit,
    onNavigate: (ScreenDestination) -> Unit
) {
    val appColors = LocalAppColors.current
    val currentTheme by viewModel.appTheme.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val readingProgress by viewModel.allReadingProgress.collectAsState()
    val bookmarks by viewModel.userBookmarks.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    // Dialog states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editDisplayName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var editAvatarUri by remember(currentUser) { mutableStateOf(currentUser?.avatarUri) }

    var showThemesDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editAvatarUri = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = appColors.surface,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("more_profile_card"),
                    colors = CardDefaults.cardColors(containerColor = appColors.card),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, appColors.border)
                ) {
                    if (currentUser != null) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(appColors.primary)
                                            .border(1.5.dp, appColors.border, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!currentUser.avatarUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = currentUser.avatarUri,
                                                contentDescription = currentUser.displayName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = currentUser.avatarLetter,
                                                color = appColors.onPrimary,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = currentUser.displayName,
                                            fontFamily = LocalReadingFont.current,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appColors.text
                                        )
                                        Text(
                                            text = currentUser.username,
                                            fontSize = 12.sp,
                                            color = appColors.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val (roleLabel, roleBg) = when (currentUser.role) {
                                            UserRole.ADMIN -> "অ্যাডমিন (মালিক)" to appColors.primary
                                            UserRole.WRITER -> "লেখক" to appColors.primary.copy(alpha = 0.85f)
                                            UserRole.READER -> "পাঠক" to appColors.textSecondary
                                        }
                                        Surface(
                                            color = roleBg,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = roleLabel,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        editDisplayName = currentUser.displayName
                                        editAvatarUri = currentUser.avatarUri
                                        showEditProfileDialog = true
                                    },
                                    modifier = Modifier.testTag("more_edit_profile_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, appColors.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "এডিট",
                                        modifier = Modifier.size(14.dp),
                                        tint = appColors.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "এডিট",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = appColors.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = appColors.border)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.logout()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "লগআউট",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("লগআউট করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Guest banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "প্রোফাইল",
                                    tint = appColors.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "অতিথি পাঠক",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.text
                                    )
                                    Text(
                                        text = "লাইব্রেরি ও মন্তব্য সিঙ্ক করতে লগইন করুন",
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onDismiss()
                                    onNavigate(ScreenDestination.Login)
                                },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("লগইন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // General Reader Options
            item {
                Text(
                    text = "সাধারণ ও পঠন সেটিংস",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.card),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, appColors.border)
                ) {
                    Column {
                        // 1. Reading Progress
                        MoreMenuItem(
                            icon = Icons.Outlined.AutoGraph,
                            title = "পাঠের অগ্রগতি (Reading Progress)",
                            subtitle = "পঠিত গল্প ও সক্রিয় সময়ের পরিসংখ্যান",
                            onClick = { showProgressDialog = true }
                        )

                        HorizontalDivider(color = appColors.border, modifier = Modifier.padding(horizontal = 16.dp))

                        // 2. Website Themes (Preserves Badami Brown, Olive Moss, etc.)
                        MoreMenuItem(
                            icon = Icons.Outlined.Palette,
                            title = "ওয়েবসাইট থিমসমূহ (Website Themes)",
                            subtitle = "বর্তমান: ${currentTheme.displayName}",
                            onClick = { showThemesDialog = true }
                        )

                        HorizontalDivider(color = appColors.border, modifier = Modifier.padding(horizontal = 16.dp))

                        // 3. Settings (Font & Reading style)
                        MoreMenuItem(
                            icon = Icons.Outlined.Settings,
                            title = "অ্যাপ সেটিংস ও ফন্ট",
                            subtitle = "বাংলা ফন্ট, সাইজ ও লাইন স্পেসিং",
                            onClick = {
                                onDismiss()
                                onNavigate(ScreenDestination.Settings)
                            }
                        )

                        HorizontalDivider(color = appColors.border, modifier = Modifier.padding(horizontal = 16.dp))

                        // 4. Notifications
                        MoreMenuItem(
                            icon = Icons.Outlined.Notifications,
                            title = "বিজ্ঞপ্তি (Notifications)",
                            subtitle = if (unreadCount > 0) "$unreadCount টি নতুন বিজ্ঞপ্তি আছে" else "কোনো নতুন বিজ্ঞপ্তি নেই",
                            badgeText = if (unreadCount > 0) "$unreadCount" else null,
                            onClick = {
                                onDismiss()
                                onNavigate(ScreenDestination.Notifications)
                            }
                        )

                        HorizontalDivider(color = appColors.border, modifier = Modifier.padding(horizontal = 16.dp))

                        // 5. Help & Guidelines
                        MoreMenuItem(
                            icon = Icons.Outlined.HelpOutline,
                            title = "সহায়তা ও নির্দেশিকা",
                            subtitle = "ধারাবাহিক পড়া ও আনলকিং নীতি",
                            onClick = { showHelpDialog = true }
                        )
                    }
                }
            }

            // Role-Authorized Management Sections
            // Writer & Admin Features
            if (currentUser?.role == UserRole.WRITER || currentUser?.role == UserRole.ADMIN) {
                item {
                    Text(
                        text = "লেখক ম্যানেজমেন্ট টুলস",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = appColors.card),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, appColors.border)
                    ) {
                        Column {
                            MoreMenuItem(
                                icon = Icons.Outlined.Create,
                                title = "লেখক ড্যাশবোর্ড (Writer Dashboard)",
                                subtitle = "গল্প ও পর্ব প্রকাশনা, শিডিউলিং ও আনলক",
                                onClick = {
                                    onDismiss()
                                    onNavigate(ScreenDestination.WriterDashboard)
                                }
                            )

                            HorizontalDivider(color = appColors.border, modifier = Modifier.padding(horizontal = 16.dp))

                            MoreMenuItem(
                                icon = Icons.Outlined.Forum,
                                title = "লেখক রিভিউ ব্যবস্থাপনা",
                                subtitle = "পাঠকদের মন্তব্যের উত্তর ও পাঠকভিত্তিক ফিল্টার",
                                onClick = {
                                    onDismiss()
                                    onNavigate(ScreenDestination.ReviewsManagement)
                                }
                            )
                        }
                    }
                }
            }

            // Owner/Admin Only Administrative Controls
            if (currentUser?.role == UserRole.ADMIN) {
                item {
                    Text(
                        text = "মালিক / অ্যাডমিন প্রশাসন (Owner Controls)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC53030)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = appColors.card),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFC53030).copy(alpha = 0.3f))
                    ) {
                        Column {
                            MoreMenuItem(
                                icon = Icons.Outlined.AdminPanelSettings,
                                title = "অ্যাডমিন প্রশাসন ও পাঠক রোল",
                                subtitle = "ব্যবহারকারী রোল পরিবর্তন, অ্যাকাউন্ট অনুমোদন ও অডিট লগ",
                                onClick = {
                                    onDismiss()
                                    onNavigate(ScreenDestination.WriterDashboard)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "প্রোফাইল সম্পাদনা",
                    fontFamily = LocalReadingFont.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = appColors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(appColors.primary)
                            .border(2.dp, appColors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!editAvatarUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = editAvatarUri,
                                contentDescription = editDisplayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (editDisplayName.isNotEmpty()) editDisplayName.first().toString() else "U",
                                color = appColors.onPrimary,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ছবি আপলোড", fontSize = 12.sp)
                        }

                        if (!editAvatarUri.isNullOrEmpty()) {
                            TextButton(
                                onClick = { editAvatarUri = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                            ) {
                                Text("ছবি মুছুন", fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("ব্যবহারকারীর নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDisplayName.isNotBlank()) {
                            viewModel.updateUserProfile(editDisplayName.trim(), editAvatarUri)
                            showEditProfileDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("বাতিল")
                }
            },
            containerColor = appColors.surface
        )
    }

    // Website Themes Dialog (Completely separate from reading moods!)
    if (showThemesDialog) {
        AlertDialog(
            onDismissRequest = { showThemesDialog = false },
            title = {
                Text(
                    text = "ওয়েবসাইট থিম নির্বাচন করুন",
                    fontFamily = LocalReadingFont.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = appColors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Website Dark Mode Toggle Switch
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("modal_dark_mode_surface"),
                        color = appColors.card,
                        border = BorderStroke(1.dp, appColors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = appColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ওয়েবসাইট ডার্ক মোড (Dark Mode)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.text
                                    )
                                    Text(
                                        text = if (isDarkMode) "গাঢ় অন্ধকার মুড চালু আছে" else "উজ্জ্বল লাইট মুড চালু আছে",
                                        fontSize = 11.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.setDarkMode(it) },
                                modifier = Modifier.testTag("modal_dark_mode_switch")
                            )
                        }
                    }

                    Text(
                        text = "এটি প্ল্যাটফর্মের মূল থিম পরিবর্তন করে। পড়ার আবহাওয়ার (Reading Mood) ওপর এটি কোনো প্রভাব ফেলে না।",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )

                    AppTheme.entries.forEach { theme ->
                        val previewColors = getColorsForTheme(theme, isDarkMode)
                        val isSelected = theme == currentTheme
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme(theme)
                                    showThemesDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) previewColors.card else appColors.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) previewColors.primary else appColors.border
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(previewColors.primary)
                                            .border(1.dp, Color.Gray, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = theme.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = appColors.text
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "নির্বাচিত",
                                        tint = previewColors.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemesDialog = false }) {
                    Text("বন্ধ করুন")
                }
            },
            containerColor = appColors.surface
        )
    }

    // Reading Progress Dialog
    if (showProgressDialog) {
        val userProgress = remember(readingProgress, currentUser) {
            readingProgress.filter { it.userId == currentUser?.id }
        }
        val completedCount = userProgress.count { it.isCompleted }
        val inProgressCount = userProgress.count { !it.isCompleted }
        val totalActiveSeconds = userProgress.fold(0L) { acc, p -> acc + p.activeSeconds }
        val activeMinutes = totalActiveSeconds / 60

        AlertDialog(
            onDismissRequest = { showProgressDialog = false },
            title = {
                Text(
                    text = "আপনার পাঠের সামগ্রিক অগ্রগতি",
                    fontFamily = LocalReadingFont.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = appColors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatRow(label = "চলমান গল্প/পর্ব সংখ্যা", value = "$inProgressCount টি", color = appColors.primary)
                    StatRow(label = "সম্পূর্ণ পঠিত গল্প", value = "$completedCount টি", color = Color(0xFF2E7D32))
                    StatRow(label = "সংরক্ষিত বুকমার্ক", value = "${bookmarks.size} টি", color = appColors.primary)
                    StatRow(label = "মোট সক্রিয় পড়ার সময়", value = "$activeMinutes মিনিট", color = appColors.text)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProgressDialog = false }) {
                    Text("ঠিক আছে")
                }
            },
            containerColor = appColors.surface
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "গল্পঘর ব্যবহারের নির্দেশিকা",
                    fontFamily = LocalReadingFont.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = appColors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "• ধারাবাহিক পড়া (Sequential Reading): লেখক কর্তৃক ধারাবাহিক নিয়ম চালু থাকলে পূর্ববর্তী পর্ব পড়ে শেষ করার পর পরবর্তী পর্ব স্বয়ংক্রিয়ভাবে উন্মুক্ত হবে।",
                        fontSize = 13.sp,
                        color = appColors.text
                    )
                    Text(
                        text = "• উদ্ধৃতি রিভিউ (Passage Review): গল্প পড়ার সময় যেকোনো বাক্য বা অনুচ্ছেদ ট্যাপ করে নির্বাচন করে লেখককে সরাসরি উদ্ধৃতিসহ প্রতিক্রিয়া ও স্টার রেটিং পাঠাতে পারবেন।",
                        fontSize = 13.sp,
                        color = appColors.text
                    )
                    Text(
                        text = "• পড়ার আবহাওয়া (Reading Mood): টপ বারের মুড বাটনে ক্লিক করে যেকোনো সময় সাদা, সেপিয়া, ডার্ক অথবা আখরোট মুড নির্বাচন করতে পারেন।",
                        fontSize = 13.sp,
                        color = appColors.text
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("বুঝেছি")
                }
            },
            containerColor = appColors.surface
        )
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(appColors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = appColors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.text
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = appColors.textSecondary
                )
            }
        }

        if (badgeText != null) {
            Surface(
                color = appColors.primary,
                shape = CircleShape
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = appColors.textSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    val appColors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = appColors.textSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
