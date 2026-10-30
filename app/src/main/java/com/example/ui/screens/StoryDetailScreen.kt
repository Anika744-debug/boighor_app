package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.util.StoryExporter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private fun toBengaliDigits(number: Number): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val str = number.toString()
    val sb = StringBuilder()
    for (ch in str) {
        if (ch in '0'..'9') {
            sb.append(bnDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

private fun toBengaliDigitsWithCommas(number: Number): String {
    val formatter = NumberFormat.getInstance(Locale("bn"))
    return formatter.format(number.toLong())
}

private fun countBengaliWords(text: String): Int {
    if (text.isBlank()) return 0
    return text.trim()
        .split(Regex("[\\s\\p{Punct}।—–-]+"))
        .filter { it.isNotBlank() }
        .size
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryDetailScreen(
    storyId: String,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val readingProgress by viewModel.allReadingProgress.collectAsState()
    val userOverrides by viewModel.userOverrides.collectAsState()
    val userBookmarks by viewModel.userBookmarks.collectAsState()
    val offlineSavedStories by viewModel.offlineSavedStoryIds.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifs = remember(notifications) { notifications.count { !it.isRead } }

    val story = stories.find { it.id == storyId }
    val partsForStory = remember(allParts, storyId) {
        allParts.filter { it.storyId == storyId }.sortedBy { it.partNumber }
    }

    val isWriterOrAdmin = currentUser?.role == UserRole.WRITER || currentUser?.role == UserRole.ADMIN

    // Accessible parts filtered for readers
    val accessibleParts = remember(currentUser, story, partsForStory, readingProgress, userOverrides) {
        if (story == null) emptyList()
        else SequentialReadingHelper.getVisiblePartsForUser(
            story = story,
            allPartsForStory = partsForStory,
            currentUser = currentUser,
            userReadingProgress = readingProgress,
            userOverrides = userOverrides
        )
    }

    val totalWords = remember(partsForStory) {
        partsForStory.sumOf { countBengaliWords(it.content) }
    }

    val isSavedOffline = offlineSavedStories.contains(storyId)

    // Dialog States
    var showEditStoryDialog by remember { mutableStateOf(false) }
    var showNewPartDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }
    var showUnpublishConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteStoryConfirmDialog by remember { mutableStateOf(false) }

    var editingPart by remember { mutableStateOf<StoryPart?>(null) }
    var deletingPart by remember { mutableStateOf<StoryPart?>(null) }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Stories)
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = story?.title ?: "গল্পের বিবরণ",
                currentUser = currentUser,
                unreadNotificationCount = unreadNotifs,
                currentMood = readingMood,
                onMoodSelected = { viewModel.updateMood(it) },
                showMoodSelector = true,
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(ScreenDestination.Stories) },
                onNotificationClick = { viewModel.navigateTo(ScreenDestination.Notifications) },
                onSettingsClick = { viewModel.navigateTo(ScreenDestination.Settings) },
                onProfileClick = { viewModel.navigateTo(ScreenDestination.Login) }
            )
        },
        containerColor = appColors.background
    ) { paddingValues ->
        if (story == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "গল্পটি খুঁজে পাওয়া যায়নি",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )
                    Button(onClick = { viewModel.navigateTo(ScreenDestination.Stories) }) {
                        Text("গল্প সম্ভারে ফিরে যান")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // 3.1 Cover Image (Approx 24px top margin below navbar, width ~36% of screen, portrait 3:4, left-aligned, radius ~12px, subtle shadow)
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.36f)
                                .aspectRatio(3f / 4f)
                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.surface)
                                .border(1.dp, appColors.border, RoundedCornerShape(12.dp)),
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

                        // 3.2 Status Badges (Approx 16px below cover, left-aligned in one row)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // "চলমান" or "সম্পূর্ণ": pill-shaped, bold white text, theme primary background
                            val completionLabel = if (story.isCompleted) "সম্পূর্ণ" else "চলমান"
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = appColors.primary
                            ) {
                                Text(
                                    text = completionLabel,
                                    color = appColors.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }

                            // Writer view only: "প্রকাশিত" (or "অপ্রকাশিত"), muted sage-green background, approx 12px spacing
                            if (isWriterOrAdmin) {
                                Spacer(modifier = Modifier.width(12.dp))
                                val pubStatusLabel = if (story.isPublished) "প্রকাশিত" else "অপ্রকাশিত"
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (story.isPublished) Color(0xFF6B8E23).copy(alpha = 0.22f) else Color(0xFFD97706).copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, if (story.isPublished) Color(0xFF6B8E23).copy(alpha = 0.4f) else Color(0xFFD97706).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = pubStatusLabel,
                                        color = if (story.isPublished) Color(0xFF2E462B) else Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // 3.3 Story Title (Large, bold Bengali heading, left-aligned)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = story.title,
                            fontFamily = LocalReadingFont.current,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text,
                            textAlign = TextAlign.Start
                        )

                        // 3.4 Story Description (One line beneath title, regular weight, muted secondary text)
                        Spacer(modifier = Modifier.height(6.dp))
                        val descText = story.description.trim().ifEmpty { "কোনো বিবরণ নেই" }
                        Text(
                            text = descText,
                            fontSize = 14.sp,
                            color = appColors.textSecondary,
                            textAlign = TextAlign.Start
                        )

                        // 3.5 Metadata Row (Document icon + "২ অধ্যায়", Open-book icon + "৫,১২৪ শব্দ")
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Article,
                                    contentDescription = null,
                                    tint = appColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${toBengaliDigits(partsForStory.size)} অধ্যায়",
                                    fontSize = 13.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = appColors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${toBengaliDigitsWithCommas(totalWords)} শব্দ",
                                    fontSize = 13.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 4. Writer View — Actions (Placed below metadata row, wrapping rows, ~12px gaps, left-aligned, content-width)
                        if (isWriterOrAdmin) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Row 1: "✏️ সম্পাদনা", "＋ নতুন অধ্যায়"
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showEditStoryDialog = true },
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("action_edit_story"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = appColors.surface,
                                            contentColor = appColors.text
                                        ),
                                        border = BorderStroke(1.dp, appColors.border)
                                    ) {
                                        Text("✏️", fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("সম্পাদনা", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = appColors.text)
                                    }

                                    OutlinedButton(
                                        onClick = { showNewPartDialog = true },
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("action_new_chapter"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = appColors.surface,
                                            contentColor = appColors.text
                                        ),
                                        border = BorderStroke(1.dp, appColors.border)
                                    ) {
                                        Text("＋", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("নতুন অধ্যায়", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = appColors.text)
                                    }
                                }

                                // Row 2: "✔ সম্পূর্ণ করুন", "👁🗨 অপ্রকাশিত করুন"
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showCompleteConfirmDialog = true },
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("action_complete_story"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = appColors.surface,
                                            contentColor = appColors.text
                                        ),
                                        border = BorderStroke(1.dp, appColors.border)
                                    ) {
                                        Text("✔", fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (story.isCompleted) "চলমান করুন" else "সম্পূর্ণ করুন",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = appColors.text
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { showUnpublishConfirmDialog = true },
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("action_unpublish_story"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = appColors.surface,
                                            contentColor = appColors.text
                                        ),
                                        border = BorderStroke(1.dp, appColors.border)
                                    ) {
                                        Text("👁🗨", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (story.isPublished) "অপ্রকাশিত করুন" else "প্রকাশ করুন",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = appColors.text
                                        )
                                    }
                                }

                                // Row 3: "🗑 মুছুন" (Danger style: terracotta/red-brown background, white text and white trash icon)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { showDeleteStoryConfirmDialog = true },
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("action_delete_story"),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFB84A39), // Terracotta danger
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("মুছুন", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            // 6. Reader View — Offline Save Button (Below metadata row, wide rounded rectangle, ~12px radius, ~52px height, content width)
                            Spacer(modifier = Modifier.height(20.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("action_offline_save")
                                    .clickable {
                                        if (isSavedOffline) {
                                            viewModel.removeStoryOffline(story.id)
                                            Toast.makeText(context, "গল্পটি অফলাইন তালিকা থেকে সরানো হয়েছে", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.saveStoryOffline(story.id)
                                            Toast.makeText(context, "গোটা গল্পটি সফলভাবে অফলাইনে সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = appColors.card,
                                border = BorderStroke(1.dp, appColors.border)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSavedOffline) Icons.Default.CheckCircle else Icons.Default.Download,
                                        contentDescription = null,
                                        tint = if (isSavedOffline) appColors.primary else appColors.text,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isSavedOffline) "✓ গোটা গল্প অফলাইনে সেভ করা আছে" else "গোটা গল্প অফলাইনে সেভ করুন",
                                        fontFamily = LocalReadingFont.current,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSavedOffline) appColors.primary else appColors.text
                                    )
                                }
                            }

                            // 7. Reader View — Download Row ("গোটা গল্প ডাউনলোড:" followed by "PDF", "Word (.docx)", "TXT")
                            Spacer(modifier = Modifier.height(14.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "গোটা গল্প ডাউনলোড:",
                                    fontSize = 13.sp,
                                    color = appColors.textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)
                                )

                                // PDF Chip
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = appColors.card,
                                    border = BorderStroke(1.dp, appColors.border),
                                    modifier = Modifier
                                        .testTag("download_chip_pdf")
                                        .clickable {
                                            coroutineScope.launch {
                                                StoryExporter.exportStoryToPdf(context, story, accessibleParts)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp), tint = appColors.primary)
                                        Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                    }
                                }

                                // Word (.docx) Chip
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = appColors.card,
                                    border = BorderStroke(1.dp, appColors.border),
                                    modifier = Modifier
                                        .testTag("download_chip_docx")
                                        .clickable {
                                            coroutineScope.launch {
                                                StoryExporter.exportStoryToDocx(context, story, accessibleParts)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp), tint = appColors.primary)
                                        Text("Word (.docx)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                    }
                                }

                                // TXT Chip
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = appColors.card,
                                    border = BorderStroke(1.dp, appColors.border),
                                    modifier = Modifier
                                        .testTag("download_chip_txt")
                                        .clickable {
                                            coroutineScope.launch {
                                                StoryExporter.exportStoryToTxt(context, story, accessibleParts)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(15.dp), tint = appColors.primary)
                                        Text("TXT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                    }
                                }
                            }
                        }

                        // 5 & 8: অধ্যায়সমূহ Section Heading (Approx 48px above chapter list, large, bold, left-aligned Bengali heading)
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = "অধ্যায়সমূহ",
                            fontFamily = LocalReadingFont.current,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Chapters list: Writer vs Reader card structure
                val partsToDisplay = if (isWriterOrAdmin) partsForStory else accessibleParts

                if (partsToDisplay.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "এই গল্পে এখনো কোনো অধ্যায় যুক্ত করা হয়নি।",
                                fontSize = 14.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                } else {
                    items(partsToDisplay) { part ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            if (isWriterOrAdmin) {
                                // 5. অধ্যায়সমূহ — Writer View Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("writer_chapter_card_${part.id}"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, appColors.border)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Small rounded beige tag, e.g. "অধ্যায় ৩", bold muted-brown text
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = appColors.card,
                                            border = BorderStroke(0.5.dp, appColors.border)
                                        ) {
                                            Text(
                                                text = "অধ্যায় ${toBengaliDigits(part.partNumber)}",
                                                color = appColors.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }

                                        // 2. Chapter title in large, bold, dark text
                                        Text(
                                            text = part.title,
                                            fontFamily = LocalReadingFont.current,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appColors.text
                                        )

                                        // 3. Open-book icon + actual word count, e.g. "২,৭০৫ শব্দ", using muted text
                                        val wordCount = countBengaliWords(part.content)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.MenuBook,
                                                contentDescription = null,
                                                tint = appColors.textSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "${toBengaliDigitsWithCommas(wordCount)} শব্দ",
                                                fontSize = 13.sp,
                                                color = appColors.textSecondary
                                            )
                                        }
                                    }
                                }

                                // Below and outside each card: exactly one row of four text links with icons:
                                // "✏️ সম্পাদনা", "👁 প্রিভিউ", "🚫👁 অপ্রকাশিত", "🗑 মুছুন" (evenly spaced outside the card!)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // ✏️ সম্পাদনা (dark text)
                                    Row(
                                        modifier = Modifier
                                            .clickable { editingPart = part }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("✏️", fontSize = 13.sp)
                                        Text("সম্পাদনা", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = appColors.text)
                                    }

                                    // 👁 প্রিভিউ (dark text)
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.navigateTo(ScreenDestination.Reading(story.id, part.id))
                                            }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("👁", fontSize = 13.sp)
                                        Text("প্রিভিউ", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = appColors.text)
                                    }

                                    // 🚫👁 অপ্রকাশিত (amber/gold text)
                                    val isPartPublished = part.status == PartStatus.PUBLISHED
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                coroutineScope.launch {
                                                    val newStatus = if (isPartPublished) PartStatus.DRAFT else PartStatus.PUBLISHED
                                                    viewModel.repository.updateStoryPart(part.copy(status = newStatus))
                                                    Toast.makeText(
                                                        context,
                                                        if (newStatus == PartStatus.PUBLISHED) "পর্বটি প্রকাশিত হয়েছে" else "পর্বটি অপ্রকাশিত করা হয়েছে",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("🚫👁", fontSize = 12.sp)
                                        Text(
                                            text = if (isPartPublished) "অপ্রকাশিত" else "প্রকাশ করুন",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFD97706) // Amber/Gold
                                        )
                                    }

                                    // 🗑 মুছুন (red/danger text)
                                    Row(
                                        modifier = Modifier
                                            .clickable { deletingPart = part }
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("🗑", fontSize = 13.sp)
                                        Text("মুছুন", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFDC2626))
                                    }
                                }
                            } else {
                                // 8. অধ্যায়সমূহ — Reader View Card
                                val userProg = readingProgress.find { it.userId == currentUser?.id && it.partId == part.id }
                                val progPercent = userProg?.progressPercent ?: 0
                                val isBookmarked = userBookmarks.any { it.userId == currentUser?.id && it.partId == part.id }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reader_chapter_card_${part.id}")
                                        .clickable {
                                            viewModel.navigateTo(ScreenDestination.Reading(story.id, part.id))
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, appColors.border)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Top row: Left chapter tag, Right outline bookmark icon button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = appColors.card,
                                                border = BorderStroke(0.5.dp, appColors.border)
                                            ) {
                                                Text(
                                                    text = "অধ্যায় ${toBengaliDigits(part.partNumber)}",
                                                    color = appColors.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.toggleBookmark(story.id, part.id) { saved ->
                                                        Toast.makeText(
                                                            context,
                                                            if (saved) "বুকমার্ক সংরক্ষিত হয়েছে" else "বুকমার্ক সরানো হয়েছে",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                                    contentDescription = "বুকমার্ক",
                                                    tint = if (isBookmarked) appColors.primary else appColors.textSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Chapter title in bold, large text
                                        Text(
                                            text = part.title,
                                            fontFamily = LocalReadingFont.current,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appColors.text
                                        )

                                        // Metadata row: Open-book icon + actual word count, Clock icon + actual reading progress
                                        val wordCount = countBengaliWords(part.content)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.MenuBook,
                                                    contentDescription = null,
                                                    tint = appColors.textSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "${toBengaliDigitsWithCommas(wordCount)} শব্দ",
                                                    fontSize = 12.sp,
                                                    color = appColors.textSecondary
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AccessTime,
                                                    contentDescription = null,
                                                    tint = appColors.textSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "${toBengaliDigits(progPercent)}% পড়া হয়েছে",
                                                    fontSize = 12.sp,
                                                    color = appColors.textSecondary
                                                )
                                            }
                                        }

                                        // Progress bar: Thin rounded track (soft beige), filled portion using theme primary
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(appColors.border.copy(alpha = 0.5f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth((progPercent / 100f).coerceIn(0f, 1f))
                                                    .background(appColors.primary)
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

    // --- Dialogs ---

    // 4.1 Edit Story Dialog
    if (showEditStoryDialog && story != null) {
        var editTitle by remember { mutableStateOf(story.title) }
        var editDesc by remember { mutableStateOf(story.description) }

        AlertDialog(
            onDismissRequest = { showEditStoryDialog = false },
            title = {
                Text("গল্প সম্পাদনা", fontWeight = FontWeight.Bold, color = appColors.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("গল্পের নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("গল্পের বিবরণ") },
                        modifier = Modifier.fillMaxWidth().height(110.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isBlank()) {
                            Toast.makeText(context, "গল্পের নাম দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateStoryDetails(
                            storyId = story.id,
                            title = editTitle,
                            description = editDesc,
                            coverResName = story.coverResName,
                            coverUri = story.coverUri
                        ) { result ->
                            result.onSuccess {
                                Toast.makeText(context, "গল্পের তথ্য হালনাগাদ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                showEditStoryDialog = false
                            }.onFailure {
                                Toast.makeText(context, "সংরক্ষণ ব্যর্থ: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStoryDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            }
        )
    }

    // 4.2 New Chapter Dialog
    if (showNewPartDialog && story != null) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var newStatus by remember { mutableStateOf(PartStatus.PUBLISHED) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showNewPartDialog = false },
            title = {
                Text("নতুন অধ্যায় যোগ করুন", fontWeight = FontWeight.Bold, color = appColors.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("অধ্যায়ের শিরোনাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newStatus == PartStatus.PUBLISHED,
                            onClick = { newStatus = PartStatus.PUBLISHED },
                            label = { Text("প্রকাশিত") }
                        )
                        FilterChip(
                            selected = newStatus == PartStatus.DRAFT,
                            onClick = { newStatus = PartStatus.DRAFT },
                            label = { Text("খসড়া") }
                        )
                    }
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("অধ্যায়ের মূল লেখা") },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isBlank() || newContent.isBlank()) {
                            Toast.makeText(context, "শিরোনাম ও মূল লেখা পূরণ করুন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        coroutineScope.launch {
                            viewModel.repository.createPart(
                                storyId = story.id,
                                partTitle = newTitle.trim(),
                                content = newContent.trim(),
                                status = newStatus,
                                scheduledAt = null
                            )
                            isSubmitting = false
                            showNewPartDialog = false
                            Toast.makeText(context, "নতুন অধ্যায় সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("যুক্ত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPartDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            }
        )
    }

    // 4.3 Complete Story Confirmation Dialog
    if (showCompleteConfirmDialog && story != null) {
        val nextStatus = !story.isCompleted
        AlertDialog(
            onDismissRequest = { showCompleteConfirmDialog = false },
            title = {
                Text(
                    text = if (nextStatus) "গল্পটি সম্পূর্ণ হিসেবে চিহ্নিত করবেন?" else "গল্পটি পুনরায় চলমান করবেন?",
                    fontWeight = FontWeight.Bold,
                    color = appColors.text
                )
            },
            text = {
                Text(
                    text = if (nextStatus) "গল্পটি 'সম্পূর্ণ' হিসেবে চিহ্নিত করলে পাঠকেরা জানতে পারবেন যে গল্পের ইতি টানা হয়েছে।"
                    else "গল্পটি পুনরায় 'চলমান' হিসেবে চিহ্নিত করা হবে।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleStoryCompletion(story.id) {
                            showCompleteConfirmDialog = false
                            Toast.makeText(context, if (it) "গল্পটি সম্পূর্ণ করা হয়েছে" else "গল্পটি চলমান করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("হ্যাঁ, নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            }
        )
    }

    // 4.4 Unpublish Story Confirmation Dialog
    if (showUnpublishConfirmDialog && story != null) {
        val nextPub = !story.isPublished
        AlertDialog(
            onDismissRequest = { showUnpublishConfirmDialog = false },
            title = {
                Text(
                    text = if (nextPub) "গল্পটি প্রকাশ করবেন?" else "গল্পটি অপ্রকাশিত করবেন?",
                    fontWeight = FontWeight.Bold,
                    color = appColors.text
                )
            },
            text = {
                Text(
                    text = if (nextPub) "গল্পটি প্রকাশিত হলে সকল পাঠক এটি দেখতে ও পড়তে পারবেন।"
                    else "গল্পটি অপ্রকাশিত করলে পাঠকদের তালিকা থেকে এটি সরিয়ে ফেলা হবে। কোনো তথ্য মুছে যাবে না।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleStoryPublished(story.id) {
                            showUnpublishConfirmDialog = false
                            Toast.makeText(context, if (it) "গল্পটি প্রকাশিত হয়েছে" else "গল্পটি অপ্রকাশিত করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("হ্যাঁ, নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpublishConfirmDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            }
        )
    }

    // 4.5 Delete Story Confirmation Dialog
    if (showDeleteStoryConfirmDialog && story != null) {
        AlertDialog(
            onDismissRequest = { showDeleteStoryConfirmDialog = false },
            title = {
                Text("গল্প মুছে ফেলার নিশ্চয়তা", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে “${story.title}” গল্পটি মুছে ফেলতে চান? গল্পের সকল অধ্যায় ও পর্যালোচনা স্থায়ীভাবে মুছে যাবে।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStoryPermanently(story.id) {
                            showDeleteStoryConfirmDialog = false
                            Toast.makeText(context, "গল্পটি সফলভাবে মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            viewModel.navigateTo(ScreenDestination.Stories)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("স্থায়ীভাবে মুছুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStoryConfirmDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            }
        )
    }

    // 5.1 Edit Chapter Dialog
    if (editingPart != null) {
        val part = editingPart!!
        var editTitle by remember { mutableStateOf(part.title) }
        var editContent by remember { mutableStateOf(part.content) }
        var editStatus by remember { mutableStateOf(part.status) }

        AlertDialog(
            onDismissRequest = { editingPart = null },
            title = {
                Text("অধ্যায় সম্পাদনা: পর্ব ${part.partNumber}", fontWeight = FontWeight.Bold, color = appColors.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("অধ্যায়ের শিরোনাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = editStatus == PartStatus.PUBLISHED,
                            onClick = { editStatus = PartStatus.PUBLISHED },
                            label = { Text("প্রকাশিত") }
                        )
                        FilterChip(
                            selected = editStatus == PartStatus.DRAFT,
                            onClick = { editStatus = PartStatus.DRAFT },
                            label = { Text("খসড়া") }
                        )
                    }
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("মূল লেখা") },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.updateStoryPart(
                                part.copy(
                                    title = editTitle.trim(),
                                    content = editContent.trim(),
                                    status = editStatus
                                )
                            )
                            editingPart = null
                            Toast.makeText(context, "অধ্যায় হালনাগাদ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPart = null }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            }
        )
    }

    // 5.4 Delete Chapter Confirmation Dialog
    if (deletingPart != null) {
        val part = deletingPart!!
        AlertDialog(
            onDismissRequest = { deletingPart = null },
            title = {
                Text("অধ্যায় মুছুন", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            },
            text = {
                Text("আপনি কি নিশ্চিতভাবে “${part.title}” অধ্যায়টি মুছে ফেলতে চান?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.repository.permanentlyDeletePart(part.id)
                            deletingPart = null
                            Toast.makeText(context, "অধ্যায়টি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPart = null }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            }
        )
    }
}
