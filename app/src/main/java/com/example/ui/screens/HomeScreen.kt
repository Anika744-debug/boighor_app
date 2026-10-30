package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.TopNavBar
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont
import com.example.util.SequentialReadingHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.checkScheduledPublications()
    }

    val appColors = LocalAppColors.current
    val currentUser by viewModel.currentUser.collectAsState()
    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val readingProgress by viewModel.allReadingProgress.collectAsState()
    val userOverrides by viewModel.userOverrides.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("সর্বশেষ") } // "সর্বশেষ", "নাম (অ-হ)", "অংশ সংখ্যা"
    var showSortMenu by remember { mutableStateOf(false) }

    val unreadNotifs = notifications.count { !it.isRead }

    // Filtered published stories
    val filteredStories = remember(stories, allParts, currentUser, searchQuery, selectedSort) {
        val isPrivileged = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.WRITER
        val base = stories.filter { story ->
            if (!isPrivileged) {
                // Public Story Access: Show all published stories
                story.isPublished && !story.isDeleted
            } else true
        }.filter { story ->
            if (searchQuery.isBlank()) true
            else story.title.contains(searchQuery, ignoreCase = true) || story.description.contains(searchQuery, ignoreCase = true)
        }

        when (selectedSort) {
            "নাম (অ-হ)" -> base.sortedBy { it.title }
            "অংশ সংখ্যা" -> base.sortedByDescending { s -> allParts.count { it.storyId == s.id } }
            else -> base.sortedByDescending { it.updatedAt }
        }
    }

    // Last read story part for "Continue Reading"
    val continueReadingItem = remember(readingProgress, currentUser, allParts, stories) {
        if (currentUser == null) null
        else {
            val userProgress = readingProgress
                .filter { it.userId == currentUser?.id && !it.isCompleted }
                .maxByOrNull { it.lastReadAt }

            if (userProgress != null) {
                val story = stories.find { it.id == userProgress.storyId }
                val part = allParts.find { it.id == userProgress.partId }
                if (story != null && part != null) {
                    Triple(story, part, userProgress)
                } else null
            } else null
        }
    }

    // Upcoming scheduled part banner info
    val upcomingScheduledPart = remember(allParts, stories) {
        val now = System.currentTimeMillis()
        val scheduled = allParts
            .filter { it.status == PartStatus.SCHEDULED && (it.scheduledAt ?: 0) > now }
            .minByOrNull { it.scheduledAt ?: Long.MAX_VALUE }

        if (scheduled != null) {
            val story = stories.find { it.id == scheduled.storyId }
            if (story != null) Pair(story, scheduled) else null
        } else null
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "গল্পঘর",
                currentUser = currentUser,
                unreadNotificationCount = unreadNotifs,
                currentMood = readingMood,
                onMoodSelected = { viewModel.updateMood(it) },
                showMoodSelector = true,
                onNotificationClick = { viewModel.navigateTo(ScreenDestination.Notifications) },
                onSettingsClick = { viewModel.navigateTo(ScreenDestination.Settings) },
                onProfileClick = { viewModel.navigateTo(ScreenDestination.Login) }
            )
        },
        containerColor = appColors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Guest Welcome & Login Banner
            if (currentUser == null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("guest_login_banner")
                            .clickable { viewModel.navigateTo(ScreenDestination.Login) },
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.card
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.primary.copy(alpha = 0.3f))
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(appColors.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "প্রোফাইল",
                                        tint = appColors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "গল্পঘরে স্বাগতম!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.text
                                    )
                                    Text(
                                        text = "লগইন বা নিবন্ধন করে আপনার লাইব্রেরি সাজান",
                                        fontSize = 11.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.navigateTo(ScreenDestination.Login) },
                                modifier = Modifier.testTag("banner_login_button"),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("লগইন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Writer/Admin Action Bar
            if (currentUser?.role == UserRole.WRITER || currentUser?.role == UserRole.ADMIN) {
                item {
                    WriterControlCard(
                        onOpenDashboard = { viewModel.navigateTo(ScreenDestination.WriterDashboard) },
                        onOpenReviews = { viewModel.navigateTo(ScreenDestination.ReviewsManagement) }
                    )
                }
            }

            // Continue Reading Banner (Mandatory requirement)
            if (continueReadingItem != null) {
                val (story, part, progress) = continueReadingItem
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("continue_reading_card")
                            .clickable {
                                viewModel.navigateTo(ScreenDestination.Reading(story.id, part.id))
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(appColors.onPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "পড়া চালিয়ে যান",
                                    tint = appColors.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "পড়া চালিয়ে যান",
                                    fontSize = 12.sp,
                                    color = appColors.onPrimary.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${story.title} • পর্ব ${part.partNumber}",
                                    fontSize = 16.sp,
                                    fontFamily = LocalReadingFont.current,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.onPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LinearProgressIndicator(
                                        progress = { (progress.progressPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = appColors.onPrimary,
                                        trackColor = appColors.onPrimary.copy(alpha = 0.3f),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${progress.progressPercent}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming Publication Banner (When scheduled part exists)
            if (upcomingScheduledPart != null) {
                val (uStory, uPart) = upcomingScheduledPart
                val dateFormat = SimpleDateFormat("d MMMM, hh:mm a", Locale("bn"))
                val dateStr = uPart.scheduledAt?.let { "${dateFormat.format(Date(it))} (Asia/Dhaka)" } ?: "শীঘ্রই"

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = appColors.card
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "আসন্ন প্রকাশনা",
                                tint = appColors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "আসন্ন প্রকাশনা: “${uStory.title}”",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.text
                                )
                                Text(
                                    text = "${uPart.title} — প্রকাশের সময়: $dateStr",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Sorting Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("গল্পের নাম দিয়ে খুঁজুন...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "অনুসন্ধান",
                                tint = appColors.textSecondary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "মুছুন",
                                        tint = appColors.textSecondary
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("story_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.border,
                            focusedContainerColor = appColors.surface,
                            unfocusedContainerColor = appColors.surface
                        )
                    )

                    // Sort button & menu
                    Box {
                        FilledTonalIconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .testTag("story_sort_button")
                                .size(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = appColors.surface,
                                contentColor = appColors.text
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "সাজান"
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(appColors.surface)
                        ) {
                            listOf("সর্বশেষ", "নাম (অ-হ)", "অংশ সংখ্যা").forEach { sortOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sortOption,
                                            fontWeight = if (selectedSort == sortOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSort == sortOption) appColors.primary else appColors.text
                                        )
                                    },
                                    onClick = {
                                        selectedSort = sortOption
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section Header: গল্প সম্ভার
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "গল্প সম্ভার (${filteredStories.size})",
                        fontFamily = LocalReadingFont.current,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )
                    Text(
                        text = "সাজানো: $selectedSort",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            // Story List
            if (filteredStories.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = if (searchQuery.isNotEmpty()) "কোনো গল্প খুঁজে পাওয়া যায়নি" else "এখনো কোনো গল্প প্রকাশিত হয়নি"
                    )
                }
            } else {
                items(filteredStories) { story ->
                    val isPrivileged = currentUser?.role == UserRole.WRITER || currentUser?.role == UserRole.ADMIN
                    val storyParts = allParts.filter { it.storyId == story.id }
                    val visibleParts = SequentialReadingHelper.getVisiblePartsForUser(
                        story = story,
                        allPartsForStory = storyParts,
                        currentUser = currentUser,
                        userReadingProgress = readingProgress,
                        userOverrides = userOverrides
                    )
                    val targetPart = visibleParts.lastOrNull() ?: storyParts.firstOrNull()

                    StoryCard(
                        story = story,
                        parts = visibleParts,
                        latestPart = targetPart,
                        isPrivileged = isPrivileged,
                        onStoryClick = {
                            viewModel.navigateTo(ScreenDestination.StoryDetail(story.id))
                        },
                        onPartClick = { partId ->
                            viewModel.navigateTo(ScreenDestination.Reading(story.id, partId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StoryCard(
    story: Story,
    parts: List<StoryPart>,
    latestPart: StoryPart?,
    isPrivileged: Boolean = false,
    onStoryClick: () -> Unit,
    onPartClick: (String) -> Unit
) {
    val appColors = LocalAppColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("story_card_${story.id}")
            .clickable { onStoryClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Story Cover Art
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, appColors.border, RoundedCornerShape(10.dp))
                        .background(appColors.card),
                    contentAlignment = Alignment.Center
                ) {
                    if (!story.coverUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = story.coverUri,
                            contentDescription = story.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val coverResId = when (story.coverResName) {
                            "cover_meghmallar" -> R.drawable.cover_meghmallar
                            else -> R.drawable.cover_nilkantha
                        }

                        Image(
                            painter = painterResource(id = coverResId),
                            contentDescription = story.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Story Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = appColors.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "ধারাবাহিক গল্প",
                                fontSize = 10.sp,
                                color = appColors.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (isPrivileged || !story.sequentialReadingEnabled) {
                            if (parts.isNotEmpty()) {
                                Text(
                                    text = "মোট পর্ব: ${parts.size}",
                                    fontSize = 11.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        } else if (parts.isNotEmpty()) {
                            Text(
                                text = "উন্মুক্ত পর্ব: ${parts.size}",
                                fontSize = 11.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = story.title,
                        fontFamily = LocalReadingFont.current,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "লেখক: ${story.authorName}",
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = story.description,
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(modifier = Modifier.height(10.dp))

            // Latest published / unlocked parts section
            Text(
                text = if (story.sequentialReadingEnabled && !isPrivileged) "আপনার উন্মুক্ত পর্বসমূহ:" else "প্রকাশিত পর্বসমূহ:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = appColors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(parts.sortedBy { it.partNumber }) { part ->
                    val isScheduled = part.status == PartStatus.SCHEDULED
                    val isDraft = part.status == PartStatus.DRAFT

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPartClick(part.id) },
                        color = if (part == latestPart) appColors.primary.copy(alpha = 0.12f) else appColors.card,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (part == latestPart) appColors.primary else appColors.border
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "পর্ব ${part.partNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (part == latestPart) appColors.primary else appColors.text
                            )

                            if (part == latestPart && !isScheduled && !isDraft) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = appColors.primary
                                ) {
                                    Text(
                                        text = "নতুন",
                                        fontSize = 9.sp,
                                        color = appColors.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            if (isScheduled) {
                                Text(
                                    text = " (আসন্ন)",
                                    fontSize = 10.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WriterControlCard(
    onOpenDashboard: () -> Unit,
    onOpenReviews: () -> Unit
) {
    val appColors = LocalAppColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = appColors.card
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = appColors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "লেখক ব্যবস্থাপনা প্যানেল",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = appColors.text
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenDashboard,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("writer_dashboard_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.primary,
                        contentColor = appColors.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন গল্প / পর্ব", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onOpenReviews,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("writer_reviews_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = appColors.text
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = appColors.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রিভিউ ও উত্তর", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    val appColors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = appColors.primary.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                fontSize = 15.sp,
                color = appColors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
