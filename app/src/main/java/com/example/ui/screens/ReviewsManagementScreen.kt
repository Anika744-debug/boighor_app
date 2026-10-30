package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
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
fun ReviewsManagementScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val currentUser by viewModel.currentUser.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val allReplies by viewModel.allReplies.collectAsState()
    val allReactions by viewModel.allReactions.collectAsState()
    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val readers by viewModel.allReaders.collectAsState()

    // Filters State
    var primaryFilter by remember { mutableStateOf("ALL") } // "ALL", "UNANSWERED", "ANSWERED", "PASSAGE", "WHOLE_PART"
    var selectedReaderId by remember { mutableStateOf<String?>(null) } // Reader-wise filter (MANDATORY)
    var selectedStoryId by remember { mutableStateOf<String?>(null) }
    var selectedRatingFilter by remember { mutableStateOf<String?>(null) } // null, "5", "4+", "3-"
    var sortByNewest by remember { mutableStateOf(true) }
    var showReaderDropdown by remember { mutableStateOf(false) }

    // Writer reply state
    var replyingReviewId by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }

    // Readers who submitted reviews
    val reviewingReaders = remember(allReviews, readers) {
        val reviewerIds = allReviews.map { it.userId }.toSet()
        readers.filter { it.id in reviewerIds }
    }

    // Filtered Reviews list
    val filteredReviews = remember(allReviews, primaryFilter, selectedReaderId, selectedStoryId, selectedRatingFilter, sortByNewest) {
        var list = allReviews

        // Primary status / type filter
        list = when (primaryFilter) {
            "UNANSWERED" -> list.filter { !it.isAnswered }
            "ANSWERED" -> list.filter { it.isAnswered }
            "PASSAGE" -> list.filter { it.reviewType == ReviewType.PASSAGE }
            "WHOLE_PART" -> list.filter { it.reviewType == ReviewType.WHOLE_PART }
            else -> list
        }

        // Reader filter
        if (selectedReaderId != null) {
            list = list.filter { it.userId == selectedReaderId }
        }

        // Story filter
        if (selectedStoryId != null) {
            list = list.filter { it.storyId == selectedStoryId }
        }

        // Rating filter
        if (selectedRatingFilter != null) {
            list = when (selectedRatingFilter) {
                "5" -> list.filter { it.rating == 5 }
                "4+" -> list.filter { (it.rating ?: 0) >= 4 }
                "3-" -> list.filter { (it.rating ?: 0) <= 3 && it.rating != null }
                else -> list
            }
        }

        if (sortByNewest) list.sortedByDescending { it.createdAt }
        else list.sortedBy { it.createdAt }
    }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "রিভিউ ব্যবস্থাপনা",
                currentUser = currentUser,
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(ScreenDestination.Home) },
                currentMood = readingMood,
                onMoodSelected = { viewModel.updateMood(it) },
                showMoodSelector = true,
                onSettingsClick = { viewModel.navigateTo(ScreenDestination.Settings) },
                onNotificationClick = { viewModel.navigateTo(ScreenDestination.Notifications) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Clear Filter row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "সকল পর্যালোচনা ও মন্তব্য (${filteredReviews.size})",
                            fontFamily = LocalReadingFont.current,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text
                        )
                        if (selectedReaderId != null) {
                            val rName = readers.find { it.id == selectedReaderId }?.displayName ?: ""
                            Text(
                                text = "নির্বাচিত পাঠক: $rName (${filteredReviews.size}টি রিভিউ)",
                                fontSize = 12.sp,
                                color = appColors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (selectedReaderId != null || selectedStoryId != null || primaryFilter != "ALL" || selectedRatingFilter != null) {
                        TextButton(
                            onClick = {
                                selectedReaderId = null
                                selectedStoryId = null
                                primaryFilter = "ALL"
                                selectedRatingFilter = null
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ফিল্টার মুছুন", fontSize = 12.sp, color = Color.Red)
                        }
                    }
                }
            }

            // Primary Quick-Access Filter Tabs
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickFilters = listOf(
                        "ALL" to "সকল",
                        "UNANSWERED" to "উত্তরহীন",
                        "ANSWERED" to "উত্তর দেওয়া",
                        "PASSAGE" to "প্যারাগ্রাফ মন্তব্য",
                        "WHOLE_PART" to "পর্ব পর্যালোচনা"
                    )
                    items(quickFilters) { (key, label) ->
                        FilterChip(
                            selected = primaryFilter == key,
                            onClick = { primaryFilter = key },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }

            // Mandatory Filter by Reader Dropdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "পাঠকভিত্তিক ফিল্টার (Filter by Reader):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box {
                            OutlinedButton(
                                onClick = { showReaderDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("filter_by_reader_dropdown_button"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedReaderId == null) "সকল পাঠক" else readers.find { it.id == selectedReaderId }?.displayName ?: "পাঠক নির্বাচন করুন",
                                        color = appColors.text,
                                        fontSize = 13.sp
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = appColors.text)
                                }
                            }

                            DropdownMenu(
                                expanded = showReaderDropdown,
                                onDismissRequest = { showReaderDropdown = false },
                                modifier = Modifier.background(appColors.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("সকল পাঠক", fontWeight = if (selectedReaderId == null) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedReaderId = null
                                        showReaderDropdown = false
                                    }
                                )
                                HorizontalDivider(color = appColors.border)
                                reviewingReaders.forEach { reader ->
                                    val rCount = allReviews.count { it.userId == reader.id }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(reader.displayName, color = appColors.text)
                                                Text("($rCount টি রিভিউ)", fontSize = 11.sp, color = appColors.textSecondary)
                                            }
                                        },
                                        onClick = {
                                            selectedReaderId = reader.id
                                            showReaderDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rating & Sort options
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedRatingFilter == "5",
                        onClick = { selectedRatingFilter = if (selectedRatingFilter == "5") null else "5" },
                        label = { Text("★ ৫ তারা", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedRatingFilter == "4+",
                        onClick = { selectedRatingFilter = if (selectedRatingFilter == "4+") null else "4+" },
                        label = { Text("★ ৪+ তারা", fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = { sortByNewest = !sortByNewest },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (sortByNewest) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = appColors.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (sortByNewest) "নতুন আগে" else "পুরনো আগে", fontSize = 12.sp, color = appColors.primary)
                    }
                }
            }

            // Reviews List
            if (filteredReviews.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো রিভিউ খুঁজে পাওয়া যায়নি।",
                            color = appColors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredReviews) { review ->
                    val story = stories.find { it.id == review.storyId }
                    val part = allParts.find { it.id == review.partId }
                    val replies = allReplies.filter { it.reviewId == review.id }
                    val isReplying = replyingReviewId == review.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_card_${review.id}"),
                        colors = CardDefaults.cardColors(containerColor = appColors.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Info Row: Reader, Story, Part, Rating & Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(appColors.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = review.userName.take(1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = appColors.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(review.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = appColors.text)
                                        Text(
                                            text = "${story?.title ?: "গল্প"} • ${part?.title ?: "পর্ব"}",
                                            fontSize = 11.sp,
                                            color = appColors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    if (review.rating != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${review.rating}/৫", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = appColors.text)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (review.isAnswered) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (review.isAnswered) "উত্তর দেওয়া হয়েছে" else "উত্তরহীন",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (review.isAnswered) Color(0xFF047857) else Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Quoted passage if passage review
                            if (!review.passageText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = appColors.card,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, appColors.border)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FormatQuote, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "“${review.passageText}”",
                                            fontSize = 12.sp,
                                            color = appColors.textSecondary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Review Comment
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = review.comment,
                                fontSize = 14.sp,
                                color = appColors.text,
                                lineHeight = 20.sp
                            )

                            // Replies if any
                            if (replies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    replies.forEach { rep ->
                                        Surface(
                                            color = appColors.card,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(rep.userName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = appColors.text)
                                                    if (rep.userRole == UserRole.WRITER) {
                                                        Surface(shape = RoundedCornerShape(3.dp), color = appColors.primary) {
                                                            Text("লেখক", fontSize = 8.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                }
                                                Text(rep.replyText, fontSize = 12.sp, color = appColors.text)
                                            }
                                        }
                                    }
                                }
                            }

                            // Action Buttons: "Go to Text" ("মূল লেখায় দেখুন") & Reply
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "Go to Text" Action (Mandatory Feature)
                                OutlinedButton(
                                    onClick = {
                                        if (story != null && part != null) {
                                            viewModel.navigateTo(
                                                ScreenDestination.Reading(
                                                    storyId = story.id,
                                                    partId = part.id,
                                                    highlightPassage = review.passageText,
                                                    highlightAnchor = review.passageAnchor
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("go_to_text_button_${review.id}"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = appColors.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("মূল লেখায় দেখুন", fontSize = 12.sp, color = appColors.text)
                                }

                                // Reply Button
                                Button(
                                    onClick = {
                                        replyingReviewId = if (isReplying) null else review.id
                                        replyText = ""
                                    },
                                    modifier = Modifier.testTag("reply_review_button_${review.id}"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = appColors.primary,
                                        contentColor = appColors.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isReplying) "বাতিল" else "উত্তর দিন", fontSize = 12.sp)
                                }
                            }

                            // Writer Reply Box
                            if (isReplying) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = { Text("লেখকের উত্তর লিখুন...", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            if (replyText.isNotBlank() && currentUser != null) {
                                                coroutineScope.launch {
                                                    viewModel.repository.addReply(
                                                        reviewId = review.id,
                                                        userId = currentUser!!.id,
                                                        userName = currentUser!!.displayName,
                                                        userRole = currentUser!!.role,
                                                        replyText = replyText
                                                    )
                                                    replyingReviewId = null
                                                    replyText = ""
                                                    Toast.makeText(context, "উত্তর পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                                    ) {
                                        Text("পাঠান", fontSize = 12.sp)
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
