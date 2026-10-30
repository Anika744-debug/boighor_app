package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontStyle
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderReviewsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current

    val currentUser by viewModel.currentUser.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val allReplies by viewModel.allReplies.collectAsState()
    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifs = notifications.count { !it.isRead }

    // Filters: All, Passage Reviews, Whole-Part Reviews, Replied, Unreplied
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filterOptions = listOf(
        "ALL" to "সকল",
        "PASSAGE" to "উদ্ধৃতি রিভিউ",
        "WHOLE_PART" to "সম্পূর্ণ পর্বের রিভিউ",
        "REPLIED" to "উত্তর এসেছে",
        "UNREPLIED" to "উত্তরের অপেক্ষায়"
    )

    // Edit Review Dialog state
    var editingReview by remember { mutableStateOf<PassageReview?>(null) }
    var editComment by remember { mutableStateOf("") }
    var editRating by remember { mutableStateOf<Int?>(null) }

    // Delete confirmation dialog
    var deletingReviewId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    // Privacy rule: Readers only see their own reviews!
    val userReviews = remember(allReviews, currentUser) {
        if (currentUser == null) emptyList()
        else allReviews.filter { it.userId == currentUser?.id }
    }

    // Apply filters
    val filteredReviews = remember(userReviews, selectedFilter, allReplies) {
        when (selectedFilter) {
            "PASSAGE" -> userReviews.filter { it.reviewType == ReviewType.PASSAGE }
            "WHOLE_PART" -> userReviews.filter { it.reviewType == ReviewType.WHOLE_PART }
            "REPLIED" -> userReviews.filter { review ->
                review.isAnswered || allReplies.any { it.reviewId == review.id }
            }
            "UNREPLIED" -> userReviews.filter { review ->
                !review.isAnswered && allReplies.none { it.reviewId == review.id }
            }
            else -> userReviews
        }
    }

    val repliedCount = remember(userReviews, allReplies) {
        userReviews.count { r -> r.isAnswered || allReplies.any { it.reviewId == r.id } }
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "আমার রিভিউ",
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
                    // Writer / Admin Toggle Header
                    if (currentUser?.role == UserRole.WRITER || currentUser?.role == UserRole.ADMIN) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("writer_review_mgmt_banner"),
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
                                            imageVector = Icons.Outlined.Forum,
                                            contentDescription = null,
                                            tint = appColors.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "লেখক রিভিউ ড্যাশবোর্ড",
                                                fontFamily = LocalReadingFont.current,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = appColors.text
                                            )
                                            Text(
                                                text = "পাঠকদের সব পর্যালোচনার উত্তর ও পাঠকভিত্তিক ফিল্টার",
                                                fontSize = 11.sp,
                                                color = appColors.textSecondary
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.navigateTo(ScreenDestination.ReviewsManagement) },
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("উত্তর দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Reader Summary Stats Card
                    if (currentUser != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = appColors.card),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, appColors.border)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ReviewStatItem(
                                        count = "${userReviews.size}",
                                        label = "মোট রিভিউ",
                                        color = appColors.primary
                                    )
                                    VerticalDivider(
                                        modifier = Modifier.height(36.dp),
                                        color = appColors.border
                                    )
                                    ReviewStatItem(
                                        count = "$repliedCount",
                                        label = "লেখকের উত্তর",
                                        color = Color(0xFF2E7D32)
                                    )
                                    VerticalDivider(
                                        modifier = Modifier.height(36.dp),
                                        color = appColors.border
                                    )
                                    ReviewStatItem(
                                        count = "${userReviews.size - repliedCount}",
                                        label = "উত্তরের অপেক্ষায়",
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        // Guest banner
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
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
                                    Text(
                                        text = "আপনার দেওয়া মন্তব্য ও লেখকের উত্তর দেখতে লগইন করুন",
                                        fontSize = 12.sp,
                                        color = appColors.text,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = { viewModel.navigateTo(ScreenDestination.Login) },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("লগইন", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Filter chips row: All, Passage Reviews, Whole-Part Reviews, Replied, Unreplied
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(filterOptions) { (key, label) ->
                                val isSelected = selectedFilter == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = key },
                                    label = {
                                        Text(
                                            text = label,
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
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("review_filter_$key")
                                )
                            }
                        }
                    }

                    // Reviews list
                    if (filteredReviews.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RateReview,
                                    contentDescription = null,
                                    tint = appColors.textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = if (currentUser == null) "লগইন করে আপনার রিভিউ দেখুন" else "কোনো রিভিউ খুঁজে পাওয়া যায়নি",
                                    fontFamily = LocalReadingFont.current,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.text
                                )
                                Text(
                                    text = "গল্প পড়ার সময় যেকোনো অনুচ্ছেদে আপনার মন্তব্য ও রেটিং প্রদান করুন।",
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                if (currentUser != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { viewModel.navigateTo(ScreenDestination.Stories) },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("গল্প পড়ুন", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredReviews) { review ->
                            val story = stories.find { it.id == review.storyId }
                            val part = allParts.find { it.id == review.partId }
                            val repliesForReview = allReplies.filter { it.reviewId == review.id }

                            ReaderReviewCard(
                                review = review,
                                story = story,
                                part = part,
                                replies = repliesForReview,
                                onOpenStoryPart = {
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
                                onEdit = {
                                    editingReview = review
                                    editComment = review.comment
                                    editRating = review.rating
                                },
                                onDelete = {
                                    deletingReviewId = review.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Review Dialog
    if (editingReview != null) {
        val review = editingReview!!
        AlertDialog(
            onDismissRequest = { editingReview = null },
            title = {
                Text(
                    text = "রিভিউ সম্পাদনা",
                    fontFamily = LocalReadingFont.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = appColors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!review.passageText.isNullOrBlank()) {
                        Surface(
                            color = appColors.background,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, appColors.border)
                        ) {
                            Text(
                                text = "“${review.passageText}”",
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp,
                                color = appColors.text,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editComment,
                        onValueChange = { editComment = it },
                        label = { Text("আপনার মন্তব্য") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )

                    // Star rating selector (1-5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "স্টার রেটিং:", fontSize = 13.sp, color = appColors.text)
                        Row {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = {
                                        editRating = if (editRating == star) null else star
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if ((editRating ?: 0) >= star) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "$star তারা",
                                        tint = if ((editRating ?: 0) >= star) Color(0xFFFFB300) else appColors.textSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editComment.isNotBlank()) {
                            viewModel.updateReview(
                                reviewId = review.id,
                                newComment = editComment.trim(),
                                newRating = editRating
                            ) { result ->
                                if (result.isSuccess) {
                                    Toast.makeText(context, "রিভিউ সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                                    editingReview = null
                                } else {
                                    Toast.makeText(context, "আপডেট ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingReview = null }) {
                    Text("বাতিল")
                }
            },
            containerColor = appColors.surface
        )
    }

    // Delete confirmation dialog
    if (deletingReviewId != null) {
        val reviewId = deletingReviewId!!
        AlertDialog(
            onDismissRequest = { deletingReviewId = null },
            title = {
                Text(
                    text = "রিভিউ মুছবেন?",
                    fontFamily = LocalReadingFont.current,
                    fontWeight = FontWeight.Bold,
                    color = appColors.text
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে এই রিভিউটি মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না।",
                    fontSize = 13.sp,
                    color = appColors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReview(reviewId) {
                            Toast.makeText(context, "রিভিউ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            deletingReviewId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReviewId = null }) {
                    Text("বাতিল")
                }
            },
            containerColor = appColors.surface
        )
    }
}

@Composable
private fun ReaderReviewCard(
    review: PassageReview,
    story: Story?,
    part: StoryPart?,
    replies: List<ReviewReply>,
    onOpenStoryPart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val appColors = LocalAppColors.current
    val hasReply = review.isAnswered || replies.isNotEmpty()

    val dateStr = remember(review.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(review.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("reader_review_card_${review.id}"),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, appColors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Story & Part title + Clickable badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story?.title ?: "গল্প",
                        fontFamily = LocalReadingFont.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "পর্ব ${part?.partNumber ?: 1}: ${part?.title ?: ""}",
                        fontSize = 12.sp,
                        color = appColors.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Type Badge (উদ্ধৃতি রিভিউ vs সম্পূর্ণ পর্বের রিভিউ)
                Surface(
                    color = if (review.reviewType == ReviewType.PASSAGE) appColors.primary.copy(alpha = 0.12f)
                    else Color(0xFF6A4C93).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (review.reviewType == ReviewType.PASSAGE) "উদ্ধৃতি রিভিউ" else "সম্পূর্ণ পর্ব",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (review.reviewType == ReviewType.PASSAGE) appColors.primary else Color(0xFF6A4C93),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            // Quoted passage if passage review
            if (!review.passageText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = appColors.background,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, appColors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .background(appColors.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "“${review.passageText}”",
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = appColors.text,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User's Review comment
            Text(
                text = review.comment,
                fontSize = 14.sp,
                color = appColors.text,
                lineHeight = 20.sp
            )

            // Rating row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (review.rating != null && review.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            (1..review.rating).forEach { _ ->
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }

                // Reply status badge
                Surface(
                    color = if (hasReply) Color(0xFF2E7D32).copy(alpha = 0.12f) else appColors.border.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        if (hasReply) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "লেখকের উত্তর এসেছে",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            Text(
                                text = "উত্তরের অপেক্ষায়",
                                fontSize = 10.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                }
            }

            // Writer's replies display
            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                replies.forEach { reply ->
                    val replyDate = remember(reply.createdAt) {
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        sdf.format(Date(reply.createdAt))
                    }

                    Surface(
                        color = appColors.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, appColors.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Reply,
                                        contentDescription = null,
                                        tint = appColors.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${reply.userName} (লেখক)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.primary
                                    )
                                }
                                Text(
                                    text = replyDate,
                                    fontSize = 10.sp,
                                    color = appColors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = reply.replyText,
                                fontSize = 13.sp,
                                color = appColors.text,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Actions row: Jump to story/part/passage, Edit, Delete
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = appColors.border)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onOpenStoryPart,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, appColors.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = appColors.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("পর্বে দেখুন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.primary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "সম্পাদনা",
                            tint = appColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "মুছুন",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStatItem(count: String, label: String, color: Color) {
    val appColors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = appColors.textSecondary
        )
    }
}
