package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Bookmark
import com.example.data.model.ReadingProgress
import com.example.data.model.Story
import com.example.data.model.StoryPart
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.TopNavBar
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: MainViewModel) {
    val appColors = LocalAppColors.current
    val currentUser by viewModel.currentUser.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val readingProgress by viewModel.allReadingProgress.collectAsState()
    val bookmarks by viewModel.userBookmarks.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.isRead }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("চলমান পাঠ", "বুকমার্কসমূহ", "সম্পন্ন গল্প")

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    // Filter in-progress reading items for current user
    val inProgressItems = remember(readingProgress, currentUser, stories, allParts) {
        if (currentUser == null) emptyList()
        else {
            readingProgress
                .filter { it.userId == currentUser?.id && !it.isCompleted }
                .sortedByDescending { it.lastReadAt }
                .mapNotNull { prog ->
                    val story = stories.find { it.id == prog.storyId }
                    val part = allParts.find { it.id == prog.partId }
                    if (story != null && part != null) Triple(story, part, prog) else null
                }
        }
    }

    // Filter completed items for current user
    val completedItems = remember(readingProgress, currentUser, stories, allParts) {
        if (currentUser == null) emptyList()
        else {
            readingProgress
                .filter { it.userId == currentUser?.id && it.isCompleted }
                .sortedByDescending { it.lastReadAt }
                .mapNotNull { prog ->
                    val story = stories.find { it.id == prog.storyId }
                    val part = allParts.find { it.id == prog.partId }
                    if (story != null && part != null) Triple(story, part, prog) else null
                }
        }
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "আমার লাইব্রেরি",
                currentUser = currentUser,
                unreadNotificationCount = unreadNotifs,
                currentMood = readingMood,
                onMoodSelected = { viewModel.updateMood(it) },
                showMoodSelector = true,
                onNotificationClick = { viewModel.navigateTo(ScreenDestination.Notifications) }
            )
        },
        containerColor = appColors.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column {
                    // Segmented Tabs
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = appColors.surface,
                        contentColor = appColors.primary,
                        divider = { HorizontalDivider(color = appColors.border) }
                    ) {
                        tabs.forEachIndexed { index, tabTitle ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = tabTitle,
                                        fontFamily = LocalReadingFont.current,
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("library_tab_$index")
                            )
                        }
                    }

                    // Guest notice if not logged in
                    if (currentUser == null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = appColors.card),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, appColors.border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = appColors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "বুকমার্ক ও পড়ার অগ্রগতি স্থায়ী রাখতে লগইন করুন",
                                        fontSize = 12.sp,
                                        color = appColors.text
                                    )
                                }
                                Button(
                                    onClick = { viewModel.navigateTo(ScreenDestination.Login) },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("লগইন", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Content list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // Tab 0: In Progress Reading
                                if (inProgressItems.isEmpty()) {
                                    item {
                                        EmptyLibraryState(
                                            title = "কোনো চলমান পাঠ নেই",
                                            description = "আপনি এখনও কোনো গল্প পড়া শুরু করেননি। গল্প সম্ভার থেকে নতুন গল্প আবিষ্কার করুন।",
                                            actionLabel = "গল্প খুঁজুন",
                                            onAction = { viewModel.navigateTo(ScreenDestination.Stories) }
                                        )
                                    }
                                } else {
                                    items(inProgressItems) { (story, part, prog) ->
                                        InProgressCard(
                                            story = story,
                                            part = part,
                                            progress = prog,
                                            onContinueReading = {
                                                viewModel.navigateTo(ScreenDestination.Reading(story.id, part.id))
                                            }
                                        )
                                    }
                                }
                            }

                            1 -> {
                                // Tab 1: Bookmarks
                                if (bookmarks.isEmpty()) {
                                    item {
                                        EmptyLibraryState(
                                            title = "সংরক্ষিত বুকমার্ক নেই",
                                            description = "গল্প পড়ার সময় যেকোনো অংশ বা সুন্দর উদ্ধৃতি বুকমার্ক করে রাখলে তা এখানে দেখতে পাবেন।",
                                            actionLabel = "গল্প পড়ুন",
                                            onAction = { viewModel.navigateTo(ScreenDestination.Stories) }
                                        )
                                    }
                                } else {
                                    items(bookmarks) { bookmark ->
                                        val story = stories.find { it.id == bookmark.storyId }
                                        val part = allParts.find { it.id == bookmark.partId }
                                        BookmarkCard(
                                            bookmark = bookmark,
                                            story = story,
                                            part = part,
                                            onOpen = {
                                                if (story != null && part != null) {
                                                    viewModel.navigateTo(
                                                        ScreenDestination.Reading(
                                                            storyId = story.id,
                                                            partId = part.id,
                                                            highlightPassage = bookmark.passageText
                                                        )
                                                    )
                                                }
                                            },
                                            onDelete = {
                                                viewModel.removeBookmark(bookmark.id)
                                            }
                                        )
                                    }
                                }
                            }

                            2 -> {
                                // Tab 2: Completed Stories
                                if (completedItems.isEmpty()) {
                                    item {
                                        EmptyLibraryState(
                                            title = "পড়া শেষ হওয়া কোনো গল্প নেই",
                                            description = "যেকোনো গল্পের পর্ব সম্পূর্ণ পড়লে তা এখানে আপনার সফল পাঠ হিসেবে প্রদর্শিত হবে।",
                                            actionLabel = "গল্পে ফিরে যান",
                                            onAction = { viewModel.navigateTo(ScreenDestination.Stories) }
                                        )
                                    }
                                } else {
                                    items(completedItems) { (story, part, prog) ->
                                        CompletedCard(
                                            story = story,
                                            part = part,
                                            progress = prog,
                                            onReread = {
                                                viewModel.navigateTo(ScreenDestination.Reading(story.id, part.id))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InProgressCard(
    story: Story,
    part: StoryPart,
    progress: ReadingProgress,
    onContinueReading: () -> Unit
) {
    val appColors = LocalAppColors.current
    val percent = progress.progressPercent
    val dateStr = remember(progress.lastReadAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(progress.lastReadAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContinueReading() }
            .testTag("in_progress_card_${story.id}"),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, appColors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Story cover
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(appColors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!story.coverUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = story.coverUri,
                        contentDescription = story.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val coverRes = when (story.coverResName) {
                        "cover_meghmallar" -> R.drawable.cover_meghmallar
                        else -> R.drawable.cover_nilkantha
                    }
                    Image(
                        painter = painterResource(id = coverRes),
                        contentDescription = story.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = story.title,
                    fontFamily = LocalReadingFont.current,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "পর্ব ${part.partNumber}: ${part.title}",
                    fontSize = 12.sp,
                    color = appColors.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = appColors.primary,
                    trackColor = appColors.border
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$percent% সম্পন্ন",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = appColors.textSecondary
                    )
                    Text(
                        text = "সর্বশেষ: $dateStr",
                        fontSize = 10.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onContinueReading,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(appColors.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "পড়া চালিয়ে যান",
                    tint = appColors.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmark: Bookmark,
    story: Story?,
    part: StoryPart?,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val appColors = LocalAppColors.current
    val dateStr = remember(bookmark.createdAt) {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        sdf.format(Date(bookmark.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bookmark_card_${bookmark.id}"),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, appColors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (bookmark.passageText != null) Icons.Default.FormatQuote else Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = appColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (bookmark.passageText != null) "উদ্ধৃতি বুকমার্ক" else "পর্ব বুকমার্ক",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "বুকমার্ক মুছুন",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${story?.title ?: "গল্প"} • পর্ব ${part?.partNumber ?: 1}: ${part?.title ?: ""}",
                fontFamily = LocalReadingFont.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.text
            )

            if (!bookmark.passageText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = appColors.background,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, appColors.border)
                ) {
                    Text(
                        text = "“${bookmark.passageText}”",
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        color = appColors.text,
                        modifier = Modifier.padding(10.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!bookmark.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "নোট: ${bookmark.note}",
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, appColors.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = appColors.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("পড়ুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.primary)
                }
            }
        }
    }
}

@Composable
private fun CompletedCard(
    story: Story,
    part: StoryPart,
    progress: ReadingProgress,
    onReread: () -> Unit
) {
    val appColors = LocalAppColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, appColors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "সম্পন্ন",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = story.title,
                        fontFamily = LocalReadingFont.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )
                    Text(
                        text = "পর্ব ${part.partNumber}: ${part.title} (পড়া শেষ)",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedButton(
                onClick = onReread,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("পুনরায় পড়ুন", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CollectionsBookmark,
            contentDescription = null,
            tint = appColors.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = title,
            fontFamily = LocalReadingFont.current,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.text
        )
        Text(
            text = description,
            fontSize = 13.sp,
            color = appColors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
