package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesBrowseScreen(viewModel: MainViewModel) {
    val appColors = LocalAppColors.current
    val currentUser by viewModel.currentUser.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val readingProgress by viewModel.allReadingProgress.collectAsState()
    val userOverrides by viewModel.userOverrides.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.isRead }

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("সকল") }
    val genreList = listOf("সকল", "উপন্যাস", "ছোটগল্প", "রহস্য", "রোমান্টিক", "থ্রিলার")

    var selectedSort by remember { mutableStateOf("সর্বশেষ") }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedStoryForDetails by remember { mutableStateOf<Story?>(null) }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    // Filter stories accessible to user
    val accessibleStories = remember(stories, allParts, currentUser, searchQuery, selectedGenre, selectedSort) {
        val base = stories.filter { story ->
            if (currentUser?.role == UserRole.READER) {
                // Reader only sees published stories with at least 1 published part
                story.isPublished && allParts.any { it.storyId == story.id && it.status == PartStatus.PUBLISHED }
            } else true
        }.filter { story ->
            if (searchQuery.isBlank()) true
            else story.title.contains(searchQuery, ignoreCase = true) || story.description.contains(searchQuery, ignoreCase = true)
        }.filter { story ->
            if (selectedGenre == "সকল") true
            else story.description.contains(selectedGenre, ignoreCase = true) || story.title.contains(selectedGenre, ignoreCase = true)
        }

        when (selectedSort) {
            "নাম (অ-হ)" -> base.sortedBy { it.title }
            "পর্ব সংখ্যা" -> base.sortedByDescending { s -> allParts.count { it.storyId == s.id } }
            else -> base.sortedByDescending { it.updatedAt }
        }
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "গল্প সম্ভার",
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Writer / Admin Shortcut Header (Preserve authorized writer controls inside Stories area)
                    if (currentUser?.role == UserRole.WRITER || currentUser?.role == UserRole.ADMIN) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("writer_stories_shortcut_card"),
                                colors = CardDefaults.cardColors(containerColor = appColors.card),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, appColors.primary.copy(alpha = 0.3f))
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
                                            imageVector = Icons.Default.Create,
                                            contentDescription = null,
                                            tint = appColors.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "লেখক ব্যবস্থাপনা",
                                                fontFamily = LocalReadingFont.current,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = appColors.text
                                            )
                                            Text(
                                                text = "নতুন গল্প বা পর্ব তৈরি ও শিডিউলিং করুন",
                                                fontSize = 11.sp,
                                                color = appColors.textSecondary
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.navigateTo(ScreenDestination.WriterDashboard) },
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("ড্যাশবোর্ড", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Search & Sort bar
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("গল্পের নাম বা বিষয় খুঁজুন...", fontSize = 14.sp) },
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
                                        .testTag("stories_search_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = appColors.primary,
                                        unfocusedBorderColor = appColors.border,
                                        focusedContainerColor = appColors.surface,
                                        unfocusedContainerColor = appColors.surface
                                    )
                                )

                                Box {
                                    FilledTonalIconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier
                                            .size(50.dp)
                                            .testTag("stories_sort_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = appColors.surface,
                                            contentColor = appColors.text
                                        )
                                    ) {
                                        Icon(Icons.Default.Sort, contentDescription = "সাজান")
                                    }

                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        modifier = Modifier.background(appColors.surface)
                                    ) {
                                        listOf("সর্বশেষ", "নাম (অ-হ)", "পর্ব সংখ্যা").forEach { sortOption ->
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

                            // Genre filter chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(genreList) { genre ->
                                    val isSelected = selectedGenre == genre
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedGenre = genre },
                                        label = {
                                            Text(
                                                text = genre,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = appColors.primary,
                                            selectedLabelColor = appColors.onPrimary,
                                            containerColor = appColors.surface,
                                            labelColor = appColors.text
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) appColors.primary else appColors.border
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Count header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "প্রকাশিত গল্পসমূহ (${accessibleStories.size})",
                                fontFamily = LocalReadingFont.current,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.text
                            )
                            Text(
                                text = "সাজানো: $selectedSort",
                                fontSize = 11.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }

                    // Stories List
                    if (accessibleStories.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    tint = appColors.textSecondary,
                                    modifier = Modifier.size(54.dp)
                                )
                                Text(
                                    text = "কোনো গল্প খুঁজে পাওয়া যায়নি",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.text
                                )
                                Text(
                                    text = "অনুসন্ধান বা ফিল্টার পরিবর্তন করে পুনরায় চেষ্টা করুন",
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }
                    } else {
                        items(accessibleStories) { story ->
                            val partsCount = allParts.count {
                                it.storyId == story.id &&
                                        (currentUser?.role != UserRole.READER || it.status == PartStatus.PUBLISHED)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { viewModel.navigateTo(ScreenDestination.StoryDetail(story.id)) }
                                    .testTag("story_card_${story.id}"),
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
                                    // Cover Image
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
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

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = story.description,
                                            fontSize = 12.sp,
                                            color = appColors.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = appColors.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "$partsCount টি পর্ব",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = appColors.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (story.sequentialReadingEnabled) {
                                                Surface(
                                                    color = appColors.background,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(0.5.dp, appColors.border)
                                                ) {
                                                    Text(
                                                        text = "ধারাবাহিক পাঠ",
                                                        fontSize = 10.sp,
                                                        color = appColors.textSecondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "বিস্তারিত দেখুন",
                                        tint = appColors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Story Details & Parts Bottom Sheet / Dialog
    if (selectedStoryForDetails != null) {
        val story = selectedStoryForDetails!!
        val partsForStory = allParts.filter { it.storyId == story.id }.sortedBy { it.partNumber }
        val accessibleParts = remember(currentUser, story, partsForStory, readingProgress, userOverrides) {
            SequentialReadingHelper.getVisiblePartsForUser(
                story = story,
                allPartsForStory = partsForStory,
                currentUser = currentUser,
                userReadingProgress = readingProgress,
                userOverrides = userOverrides
            )
        }

        ModalBottomSheet(
            onDismissRequest = { selectedStoryForDetails = null },
            containerColor = appColors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with cover, title & description
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.text
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = story.description,
                                fontSize = 12.sp,
                                color = appColors.textSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(color = appColors.border)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "গল্পের পর্বসমূহ (${accessibleParts.size} টি উন্মুক্ত)",
                        fontFamily = LocalReadingFont.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.primary
                    )
                }

                // Parts list with lock / unlock indicators
                items(partsForStory) { part ->
                    val isUnlocked = accessibleParts.any { it.id == part.id }
                    val isPublished = part.status == PartStatus.PUBLISHED
                    val userProg = readingProgress.find { it.userId == currentUser?.id && it.partId == part.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isUnlocked) {
                                if (isUnlocked) {
                                    selectedStoryForDetails = null
                                    viewModel.navigateTo(ScreenDestination.Reading(story.id, part.id))
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) appColors.card else appColors.card.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isUnlocked) appColors.border else appColors.border.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isUnlocked) appColors.primary.copy(alpha = 0.15f)
                                            else appColors.textSecondary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            !isUnlocked -> Icons.Default.Lock
                                            userProg?.isCompleted == true -> Icons.Default.Check
                                            else -> Icons.Default.MenuBook
                                        },
                                        contentDescription = null,
                                        tint = if (isUnlocked) appColors.primary else appColors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "পর্ব ${part.partNumber}: ${part.title}",
                                        fontFamily = LocalReadingFont.current,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) appColors.text else appColors.textSecondary
                                    )
                                    Text(
                                        text = when {
                                            !isPublished -> "অপ্রকাশিত (ড্রাফট)"
                                            !isUnlocked -> "লক করা (পূর্ববর্তী পর্ব সম্পন্ন করুন)"
                                            userProg?.isCompleted == true -> "পড়া শেষ হয়েছে"
                                            userProg != null -> "${userProg.progressPercent}% পড়া হয়েছে"
                                            else -> "এখনই পড়ুন"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isUnlocked) appColors.primary else appColors.textSecondary
                                    )
                                }
                            }

                            if (isUnlocked) {
                                FilledTonalButton(
                                    onClick = {
                                        selectedStoryForDetails = null
                                        viewModel.navigateTo(ScreenDestination.Reading(story.id, part.id))
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("পড়ুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "লক",
                                    tint = appColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
