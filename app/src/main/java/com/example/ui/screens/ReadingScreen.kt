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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.FloatingReadingToolbar
import com.example.ui.components.ReadingProgressBar
import com.example.ui.components.TopNavBar
import com.example.ui.components.toBengaliDigits
import com.example.ui.theme.*
import com.example.util.PdfExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    storyId: String,
    partId: String,
    highlightPassage: String? = null,
    highlightAnchor: String? = null,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val allReviews by viewModel.allReviews.collectAsState()
    val allReplies by viewModel.allReplies.collectAsState()
    val allReactions by viewModel.allReactions.collectAsState()
    val userBookmarks by viewModel.userBookmarks.collectAsState()
    val userOverrides by viewModel.userOverrides.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val allProgress by viewModel.allReadingProgress.collectAsState()

    val readingMood by viewModel.readingMood.collectAsState()
    val readingFont by viewModel.readingFont.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()
    val isPageTurnMode by viewModel.isPageTurnMode.collectAsState()

    var isFullscreen by remember { mutableStateOf(false) }
    var showReadingSettingsDialog by remember { mutableStateOf(false) }
    var showReviewsDrawer by remember { mutableStateOf(false) }
    var showPartsDrawer by remember { mutableStateOf(false) }

    // Selected passage, anchor and contextual action states
    val density = LocalDensity.current
    val clipboardManager = LocalClipboardManager.current
    val defaultTextToolbar = LocalTextToolbar.current

    var selectedPassageText by remember { mutableStateOf<String?>(highlightPassage) }
    var selectedPassageAnchor by remember { mutableStateOf<String?>(highlightAnchor) }
    var selectedPassageParaIndex by remember { mutableStateOf<Int?>(null) }
    var activeSelectionText by remember { mutableStateOf<String?>(null) }
    var activeSelectionRect by remember { mutableStateOf<Rect?>(null) }
    var showContextualMenu by remember { mutableStateOf(false) }

    var pendingCopyAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val customTextToolbar = remember(defaultTextToolbar, clipboardManager) {
        object : TextToolbar {
            override val status: TextToolbarStatus
                get() = if (showContextualMenu) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

            override fun hide() {
                showContextualMenu = false
                activeSelectionText = null
                activeSelectionRect = null
                pendingCopyAction = null
                defaultTextToolbar.hide()
            }

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                // Do NOT automatically copy to system clipboard!
                // Only save callback and rect; copy will only happen if user explicitly taps "Copy".
                pendingCopyAction = onCopyRequested
                activeSelectionRect = rect
                showContextualMenu = true
            }
        }
    }

    var showNewReviewDialog by remember { mutableStateOf(false) }
    var newReviewType by remember { mutableStateOf(ReviewType.PASSAGE) }

    // Whole part review dialog
    var showWholePartReviewDialog by remember { mutableStateOf(false) }

    val currentStory = stories.find { it.id == storyId }
    val partsForStory = allParts.filter { it.storyId == storyId }.sortedBy { it.partNumber }
    val currentPart = partsForStory.find { it.id == partId } ?: partsForStory.firstOrNull()

    // Accessible parts filtered for the reader (future locked parts strictly hidden)
    val accessibleParts = remember(currentUser, currentStory, partsForStory, allProgress, userOverrides) {
        com.example.util.SequentialReadingHelper.getVisiblePartsForUser(
            story = currentStory,
            allPartsForStory = partsForStory,
            currentUser = currentUser,
            userReadingProgress = allProgress,
            userOverrides = userOverrides
        )
    }

    // Access authorization check via SequentialReadingHelper
    val isAuthorized = currentPart != null && com.example.util.SequentialReadingHelper.isPartAccessible(
        story = currentStory,
        part = currentPart,
        currentUser = currentUser,
        userReadingProgress = allProgress,
        userOverrides = userOverrides,
        allPartsForStory = partsForStory
    )

    val currentPartProgress = remember(allProgress, currentUser?.id, currentPart?.id) {
        allProgress.find { it.userId == currentUser?.id && it.partId == currentPart?.id }
    }

    // Paragraphs splitting
    val paragraphs = remember(currentPart?.content) {
        currentPart?.content?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    // Scroll state & progress tracking
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { maxOf(1, paragraphs.size) })

    // Restore exact reading position when opened
    var hasRestoredPosition by remember(currentPart?.id) { mutableStateOf(false) }
    LaunchedEffect(currentPart?.id, currentPartProgress?.lastScrollIndex) {
        if (!hasRestoredPosition && currentPartProgress != null && currentPartProgress.lastScrollIndex > 0) {
            try {
                if (isPageTurnMode) {
                    pagerState.scrollToPage(currentPartProgress.lastScrollIndex.coerceIn(0, maxOf(0, paragraphs.size - 1)))
                } else {
                    listState.scrollToItem(
                        index = currentPartProgress.lastScrollIndex,
                        scrollOffset = currentPartProgress.lastScrollOffset
                    )
                }
                hasRestoredPosition = true
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Auto-scroll to highlighted passage if arriving from "Go to Text" in reviews
    LaunchedEffect(highlightPassage, highlightAnchor, paragraphs) {
        if (paragraphs.isNotEmpty()) {
            val targetIdx: Int = if (!highlightAnchor.isNullOrBlank()) {
                val pIdx = highlightAnchor.substringAfter("p:").substringBefore(":").toIntOrNull()
                pIdx?.takeIf { it in paragraphs.indices }
                    ?: if (!highlightPassage.isNullOrBlank()) paragraphs.indexOfFirst { it.contains(highlightPassage) } else -1
            } else if (!highlightPassage.isNullOrBlank()) {
                paragraphs.indexOfFirst { it.contains(highlightPassage) }
            } else -1

            if (targetIdx in paragraphs.indices) {
                try {
                    if (isPageTurnMode) {
                        pagerState.scrollToPage(targetIdx)
                    } else {
                        // Index 0 in LazyColumn is the Story & Part Header, so paragraph targetIdx is at targetIdx + 1
                        listState.animateScrollToItem(targetIdx + 1)
                    }
                    selectedPassageText = highlightPassage
                    selectedPassageAnchor = highlightAnchor
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    // End-of-part detection
    val isAtEndOfPart by remember {
        derivedStateOf {
            if (isPageTurnMode) {
                pagerState.currentPage >= maxOf(0, paragraphs.size - 1)
            } else {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems <= 1) false
                else {
                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisible >= totalItems - 1
                }
            }
        }
    }

    // Real-time Reading Progress Fraction (0.0f to 1.0f) for the visible progress bar
    val currentReadingFraction by remember {
        derivedStateOf {
            if (isPageTurnMode) {
                val total = maxOf(1, paragraphs.size)
                ((pagerState.currentPage + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
            } else {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems <= 1) {
                    0f
                } else {
                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    (lastVisible.toFloat() / (totalItems - 1).toFloat()).coerceIn(0f, 1f)
                }
            }
        }
    }

    // Active reading time tracking
    var activeReadingSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(currentPart?.id) {
        activeReadingSeconds = 0L
        while (true) {
            delay(1000L)
            activeReadingSeconds++
            // Save reading progress every 5 seconds
            if (activeReadingSeconds % 5L == 0L && currentStory != null && currentPart != null) {
                val effectiveUserId = currentUser?.id ?: "guest"
                val progressPercent = if (isPageTurnMode) {
                    ((pagerState.currentPage + 1) * 100) / maxOf(1, paragraphs.size)
                } else {
                    val totalItems = paragraphs.size
                    if (totalItems > 0) {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: listState.firstVisibleItemIndex
                        ((lastVisible + 1) * 100) / totalItems
                    } else 0
                }
                val scrollIndex = if (isPageTurnMode) pagerState.currentPage else listState.firstVisibleItemIndex
                val scrollOffset = if (isPageTurnMode) 0 else listState.firstVisibleItemScrollOffset
                viewModel.repository.updateReadingProgress(
                    userId = effectiveUserId,
                    storyId = currentStory.id,
                    partId = currentPart.id,
                    progressPercent = progressPercent.coerceIn(0, 100),
                    activeDurationDeltaSeconds = 5L,
                    lastScrollIndex = scrollIndex,
                    lastScrollOffset = scrollOffset,
                    reachedEndOfPart = isAtEndOfPart || (currentPartProgress?.reachedEndOfPart == true)
                )
            }
        }
    }

    // Auto-complete immediately when 90%+ or end of part is reached
    LaunchedEffect(isAtEndOfPart, currentReadingFraction) {
        if ((isAtEndOfPart || currentReadingFraction >= 0.85f) && currentStory != null && currentPart != null) {
            val effectiveUserId = currentUser?.id ?: "guest"
            viewModel.repository.updateReadingProgress(
                userId = effectiveUserId,
                storyId = currentStory.id,
                partId = currentPart.id,
                progressPercent = 100,
                activeDurationDeltaSeconds = 1L,
                reachedEndOfPart = true
            )
        }
    }

    // Save exact position on dispose/exit
    DisposableEffect(currentPart?.id) {
        onDispose {
            if (currentStory != null && currentPart != null) {
                val effectiveUserId = currentUser?.id ?: "guest"
                val progressPercent = if (isPageTurnMode) {
                    ((pagerState.currentPage + 1) * 100) / maxOf(1, paragraphs.size)
                } else {
                    val totalItems = paragraphs.size
                    if (totalItems > 0) {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: listState.firstVisibleItemIndex
                        ((lastVisible + 1) * 100) / totalItems
                    } else 0
                }
                val scrollIndex = if (isPageTurnMode) pagerState.currentPage else listState.firstVisibleItemIndex
                val scrollOffset = if (isPageTurnMode) 0 else listState.firstVisibleItemScrollOffset
                coroutineScope.launch {
                    viewModel.repository.updateReadingProgress(
                        userId = effectiveUserId,
                        storyId = currentStory.id,
                        partId = currentPart.id,
                        progressPercent = progressPercent.coerceIn(0, 100),
                        activeDurationDeltaSeconds = activeReadingSeconds % 5L,
                        lastScrollIndex = scrollIndex,
                        lastScrollOffset = scrollOffset,
                        reachedEndOfPart = isAtEndOfPart || (currentPartProgress?.reachedEndOfPart == true)
                    )
                }
            }
        }
    }

    // Colors for the active reading mood (strictly separate from app theme!)
    val moodColors = getColorsForMood(readingMood)
    val fontChoice = getFontFamily(readingFont)
    val appColors = LocalAppColors.current

    // Bookmarking status
    val isCurrentPartBookmarked = userBookmarks.any { it.partId == currentPart?.id && it.passageText == null }
    val currentPartReviews = allReviews.filter { it.partId == currentPart?.id }
    val unreadNotifs = notifications.count { !it.isRead }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopNavBar(
                    title = currentStory?.title ?: "গল্পঘর",
                    currentUser = currentUser,
                    unreadNotificationCount = unreadNotifs,
                    currentMood = readingMood,
                    onMoodSelected = { viewModel.updateMood(it) },
                    showMoodSelector = true,
                    showBackButton = true,
                    onBackClick = { viewModel.navigateTo(ScreenDestination.Home) },
                    onNotificationClick = { viewModel.navigateTo(ScreenDestination.Notifications) },
                    onSettingsClick = { viewModel.navigateTo(ScreenDestination.Settings) },
                    onProfileClick = { viewModel.navigateTo(ScreenDestination.Login) }
                )
            }
        },
        bottomBar = {
            FloatingReadingToolbar(
                isVisible = true,
                fontSize = fontSize,
                onIncreaseFontSize = { viewModel.updateFontSize(fontSize + 2f) },
                onDecreaseFontSize = { viewModel.updateFontSize(fontSize - 2f) },
                isPageTurnMode = isPageTurnMode,
                onTogglePageTurnMode = { viewModel.togglePageTurnMode() },
                isBookmarked = isCurrentPartBookmarked,
                onToggleBookmark = {
                    if (currentUser != null && currentStory != null && currentPart != null) {
                        coroutineScope.launch {
                            val existing = userBookmarks.find { it.partId == currentPart.id && it.passageText == null }
                            if (existing != null) {
                                viewModel.repository.deleteBookmark(existing.id)
                                Toast.makeText(context, "বুকমার্ক সরানো হয়েছে", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.repository.addBookmark(currentUser!!.id, currentStory.id, currentPart.id, null, null)
                                Toast.makeText(context, "পর্বটি বুকমার্ক করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onOpenReadingSettings = { showReadingSettingsDialog = true },
                onExportPdf = {
                    if (currentStory != null && currentPart != null) {
                        PdfExporter.exportPartToPdf(context, currentStory.title, currentPart)
                    }
                },
                isFullscreen = isFullscreen,
                onToggleFullscreen = { isFullscreen = !isFullscreen },
                onOpenReviews = { showReviewsDrawer = true },
                reviewCount = currentPartReviews.size
            )
        },
        containerColor = moodColors.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(moodColors.background)
        ) {
            if (!isAuthorized) {
                // Restricted / Locked Content Notice (Strictly hide metadata)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = moodColors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, moodColors.border),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = moodColors.textSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "পর্বটি বর্তমানে লক করা আছে",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = moodColors.text,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "ধারাবাহিক পঠন নিয়মানুযায়ী পূর্ববর্তী পর্বসমূহ সফলভাবে সম্পন্ন করার পর এই পর্বটি উন্মুক্ত হবে। অথবা লেখকের অনুমোদন পেলে আপনি সরাসরি পড়তে পারবেন।",
                                fontSize = 14.sp,
                                color = moodColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.navigateTo(ScreenDestination.Home) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appColors.primary,
                                    contentColor = appColors.onPrimary
                                )
                            ) {
                                Text("হোমপেজে ফিরে যান")
                            }
                        }
                    }
                }
            } else if (currentPart == null) {
                EmptyReadingState(moodColors = moodColors, onBack = { viewModel.navigateTo(ScreenDestination.Home) })
            } else {
                // Reading Progress Line Indicator (3dp, non-intrusive) + Reading Content
                Column(modifier = Modifier.fillMaxSize()) {
                    LinearProgressIndicator(
                        progress = { currentReadingFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = appColors.primary,
                        trackColor = moodColors.border.copy(alpha = 0.2f)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Native Text Selection Container
                        CompositionLocalProvider(LocalTextToolbar provides customTextToolbar) {
                            SelectionContainer {
                                // Content Rendering: Either Continuous Scroll or Page-Turn Pager
                        if (isPageTurnMode) {
                    // Page-Turn Mode (Horizontal Pager simulating book pages)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Page indicator header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "পৃষ্ঠা ${pagerState.currentPage + 1} / ${paragraphs.size}",
                                fontSize = 12.sp,
                                color = moodColors.textSecondary
                            )
                            Text(
                                text = "পর্ব ${currentPart.partNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = moodColors.textSecondary
                            )
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) { pageIndex ->
                            val para = paragraphs.getOrNull(pageIndex) ?: ""
                            val isHighlighted = selectedPassageText != null && para.contains(selectedPassageText!!)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val annotatedPara = remember(para, selectedPassageText, selectedPassageAnchor, pageIndex) {
                                    buildHighlightedParagraph(para, selectedPassageText, selectedPassageAnchor, pageIndex)
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = moodColors.surface
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, moodColors.border)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = annotatedPara,
                                            fontFamily = fontChoice,
                                            fontSize = fontSize.sp,
                                            lineHeight = (fontSize * lineSpacing).sp,
                                            color = moodColors.text,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Continuous Vertical Reading Scroll
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Minimal Part Title Header (Unobtrusive & Scrolls naturally)
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentPart.title,
                                    fontFamily = fontChoice,
                                    fontSize = (fontSize + 2f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = moodColors.text,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "পর্ব ${toBengaliDigits(currentPart.partNumber)}",
                                        fontSize = 11.sp,
                                        color = moodColors.textSecondary
                                    )
                                    Text("•", fontSize = 10.sp, color = moodColors.textSecondary)
                                    Text(
                                        text = "পর্ব তালিকা",
                                        fontSize = 11.sp,
                                        color = appColors.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { showPartsDrawer = true }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = moodColors.border.copy(alpha = 0.5f))
                            }
                        }

                        // Paragraphs with Native Selection & Passage Highlights
                        itemsIndexed(paragraphs) { index, para ->
                            val passageReviews = currentPartReviews.filter { rev ->
                                if (!rev.passageAnchor.isNullOrBlank()) {
                                    val anchorPara = rev.passageAnchor.substringAfter("p:").substringBefore(":").toIntOrNull()
                                    anchorPara == index
                                } else if (!rev.passageText.isNullOrBlank()) {
                                    paragraphs.indexOfFirst { it.contains(rev.passageText) } == index
                                } else false
                            }
                            val passageReactions = allReactions.filter {
                                it.passageText != null && para.contains(it.passageText)
                            }
                            val annotatedPara = remember(para, selectedPassageText, selectedPassageAnchor, index) {
                                buildHighlightedParagraph(para, selectedPassageText, selectedPassageAnchor, index)
                            }

                            val paraToolbar = remember(customTextToolbar, index) {
                                object : TextToolbar {
                                    override val status: TextToolbarStatus
                                        get() = customTextToolbar.status

                                    override fun hide() {
                                        customTextToolbar.hide()
                                    }

                                    override fun showMenu(
                                        rect: Rect,
                                        onCopyRequested: (() -> Unit)?,
                                        onPasteRequested: (() -> Unit)?,
                                        onCutRequested: (() -> Unit)?,
                                        onSelectAllRequested: (() -> Unit)?
                                    ) {
                                        selectedPassageParaIndex = index
                                        customTextToolbar.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                CompositionLocalProvider(LocalTextToolbar provides paraToolbar) {
                                    SelectionContainer {
                                        Text(
                                            text = annotatedPara,
                                            fontFamily = fontChoice,
                                            fontSize = fontSize.sp,
                                            lineHeight = (fontSize * lineSpacing).sp,
                                            color = moodColors.text,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }

                                // Subtle indicator row for existing reviews & reactions on this paragraph
                                if (passageReactions.isNotEmpty() || passageReviews.isNotEmpty()) {
                                    DisableSelection {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (passageReactions.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(moodColors.surface)
                                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = passageReactions.joinToString(" ") { it.emojiType.symbol } + " ${passageReactions.size}",
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            if (passageReviews.isNotEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = moodColors.surface,
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, moodColors.border),
                                                    modifier = Modifier.clickable {
                                                        selectedPassageText = para
                                                        showReviewsDrawer = true
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = moodColors.textSecondary
                                                        )
                                                        Text(
                                                            text = "${toBengaliDigits(passageReviews.size)}টি মন্তব্য",
                                                            fontSize = 11.sp,
                                                            color = moodColors.text
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Reading Completion & Whole Part Review Card
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = moodColors.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, moodColors.border)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "পর্ব সমাপ্তি ও মূল্যায়ন",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = moodColors.text
                                    )
                                    Text(
                                        text = "এই পর্বটি আপনার কেমন লাগল? সমগ্র পর্বের জন্য রেটিং ও মতামত দিন।",
                                        fontSize = 13.sp,
                                        color = moodColors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Button(
                                        onClick = { showWholePartReviewDialog = true },
                                        modifier = Modifier.testTag("submit_whole_part_review_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = appColors.primary,
                                            contentColor = appColors.onPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("সমগ্র পর্বের পর্যালোচনা লিখুন")
                                    }
                                }
                            }
                        }

                        // After-Reading Experience & Reveal Animation
                        item {
                            val isPartCompleted = currentPartProgress?.isCompleted == true ||
                                    (currentStory != null && com.example.util.SequentialReadingHelper.isPartCompletionSatisfied(
                                        story = currentStory,
                                        progressPercent = (currentReadingFraction * 100f).toInt(),
                                        activeDurationSeconds = (currentPartProgress?.activeSeconds ?: 0L) + activeReadingSeconds,
                                        reachedEndOfPart = isAtEndOfPart || (currentPartProgress?.reachedEndOfPart == true)
                                    ))

                            val currentIndex = accessibleParts.indexOfFirst { it.id == currentPart.id }
                            val prevPart = if (currentIndex > 0) accessibleParts[currentIndex - 1] else null
                            val nextPart = if (currentIndex >= 0 && currentIndex < accessibleParts.size - 1) accessibleParts[currentIndex + 1] else null

                            val allStoryPartsSorted = partsForStory
                            val rawIndex = allStoryPartsSorted.indexOfFirst { it.id == currentPart.id }
                            val nextEligiblePart = if (rawIndex >= 0 && rawIndex < allStoryPartsSorted.size - 1) allStoryPartsSorted[rawIndex + 1] else null

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (isPartCompleted) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("part_completion_celebration_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = appColors.primary.copy(alpha = 0.1f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.primary.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = appColors.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "অভিনন্দন! পর্ব ${toBengaliDigits(currentPart.partNumber)} সফলভাবে সমাপ্ত হয়েছে",
                                                fontFamily = fontChoice,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = moodColors.text,
                                                textAlign = TextAlign.Center
                                            )

                                             if (nextPart != null || (nextEligiblePart != null && currentStory?.sequentialReadingEnabled == true)) {
                                                val targetPart = nextPart ?: nextEligiblePart!!
                                                Text(
                                                    text = "আপনার জন্য অপেক্ষা করছে পরের অধ্যায়…",
                                                    fontFamily = fontChoice,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = appColors.primary,
                                                    textAlign = TextAlign.Center
                                                )
                                                Button(
                                                    onClick = {
                                                        val effectiveUserId = currentUser?.id ?: "guest"
                                                        coroutineScope.launch {
                                                            if (currentStory != null) {
                                                                viewModel.repository.updateReadingProgress(
                                                                    userId = effectiveUserId,
                                                                    storyId = currentStory.id,
                                                                    partId = currentPart.id,
                                                                    progressPercent = 100,
                                                                    activeDurationDeltaSeconds = 1L,
                                                                    reachedEndOfPart = true
                                                                )
                                                                viewModel.repository.setManualAccessOverride(
                                                                    userId = effectiveUserId,
                                                                    storyId = currentStory.id,
                                                                    partId = targetPart.id,
                                                                    partNumber = targetPart.partNumber,
                                                                    unlock = true,
                                                                    actorId = "SYSTEM",
                                                                    actorName = "ধারাবাহিক পঠন আনলক"
                                                                )
                                                                viewModel.navigateTo(ScreenDestination.Reading(storyId, targetPart.id))
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("reveal_next_chapter_button"),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = appColors.primary,
                                                        contentColor = appColors.onPrimary
                                                    ),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("পরবর্তী অধ্যায় শুরু করুন ➜", fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Text(
                                                    text = "আপনি গল্পের এযাবৎ প্রকাশিত সবকটি পর্ব সম্পন্ন করেছেন! নতুন পর্ব প্রকাশের সাথে সাথে আপনাকে জানানো হবে।",
                                                    fontSize = 13.sp,
                                                    color = moodColors.textSecondary,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (prevPart != null) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.navigateTo(ScreenDestination.Reading(storyId, prevPart.id))
                                            },
                                            modifier = Modifier.testTag("prev_part_button"),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, moodColors.border)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = moodColors.text)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("পূর্ববর্তী পর্ব", color = moodColors.text, fontSize = 13.sp)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    val finalNextPart = nextPart ?: if (isPartCompleted) nextEligiblePart else null
                                    if (finalNextPart != null) {
                                        Button(
                                            onClick = {
                                                val effectiveUserId = currentUser?.id ?: "guest"
                                                coroutineScope.launch {
                                                    if (currentStory != null) {
                                                        viewModel.repository.updateReadingProgress(
                                                            userId = effectiveUserId,
                                                            storyId = currentStory.id,
                                                            partId = currentPart.id,
                                                            progressPercent = 100,
                                                            activeDurationDeltaSeconds = 1L,
                                                            reachedEndOfPart = true
                                                        )
                                                        viewModel.repository.setManualAccessOverride(
                                                            userId = effectiveUserId,
                                                            storyId = currentStory.id,
                                                            partId = finalNextPart.id,
                                                            partNumber = finalNextPart.partNumber,
                                                            unlock = true,
                                                            actorId = "SYSTEM",
                                                            actorName = "ধারাবাহিক পঠন আনলক"
                                                        )
                                                        viewModel.navigateTo(ScreenDestination.Reading(storyId, finalNextPart.id))
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("next_part_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = appColors.primary,
                                                contentColor = appColors.onPrimary
                                            )
                                        ) {
                                            Text("পরবর্তী পর্ব", fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    } else if (nextEligiblePart != null && currentStory?.sequentialReadingEnabled == true && !isPartCompleted) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = moodColors.surface,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, moodColors.border)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = moodColors.textSecondary)
                                                Text("পরবর্তী পর্ব লক করা", fontSize = 12.sp, color = moodColors.textSecondary)
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

                        // Floating Contextual Action Menu on Native Text Selection
                        if (showContextualMenu) {
                            DisableSelection {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    val topOffset = with(density) {
                                        val t = activeSelectionRect?.top ?: 140f
                                        maxOf(12.dp, (t - 55f).toDp())
                                    }
                                    Card(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .offset(y = topOffset)
                                            .testTag("selection_contextual_action_menu"),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = appColors.card),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                        border = BorderStroke(1.dp, appColors.border)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    val prev = clipboardManager.getText()
                                                    pendingCopyAction?.invoke()
                                                    val txt = clipboardManager.getText()?.text?.toString() ?: ""
                                                    // Restore clipboard so selecting for review does NOT auto-copy
                                                    clipboardManager.setText(prev ?: AnnotatedString(""))
                                                    val pIdx = selectedPassageParaIndex ?: 0
                                                    val para = paragraphs.getOrNull(pIdx) ?: ""
                                                    val charOffsetInPara = if (para.contains(txt)) para.indexOf(txt) else 0
                                                    var globalOffset = 0
                                                    for (i in 0 until pIdx.coerceAtMost(paragraphs.size)) {
                                                        globalOffset += paragraphs[i].length + 2
                                                    }
                                                    val globalCharOffset = globalOffset + charOffsetInPara
                                                    val anchor = "p:${pIdx}:offset:${charOffsetInPara}:len:${txt.length}:global:${globalCharOffset}"
                                                    selectedPassageText = txt
                                                    selectedPassageAnchor = anchor
                                                    activeSelectionText = txt
                                                    newReviewType = ReviewType.PASSAGE
                                                    showNewReviewDialog = true
                                                    showContextualMenu = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .testTag("action_write_review"),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = appColors.primary,
                                                    contentColor = appColors.onPrimary
                                                )
                                            ) {
                                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("রিভিউ ও রিঅ্যাক্ট", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            VerticalDivider(modifier = Modifier.height(20.dp), color = appColors.border)

                                            listOf(
                                                ReactionEmoji.LOVE,
                                                ReactionEmoji.HAHA,
                                                ReactionEmoji.SAD,
                                                ReactionEmoji.ANGRY
                                            ).forEach { emoji ->
                                                IconButton(
                                                    onClick = {
                                                        val prev = clipboardManager.getText()
                                                        pendingCopyAction?.invoke()
                                                        val passage = clipboardManager.getText()?.text?.toString() ?: ""
                                                        clipboardManager.setText(prev ?: AnnotatedString(""))
                                                        val effectiveUserId = currentUser?.id ?: "guest"
                                                        if (currentPart != null && passage.isNotBlank()) {
                                                            val pIdx = selectedPassageParaIndex ?: 0
                                                            val para = paragraphs.getOrNull(pIdx) ?: ""
                                                            val charOffsetInPara = if (para.contains(passage)) para.indexOf(passage) else 0
                                                            var globalOffset = 0
                                                            for (i in 0 until pIdx.coerceAtMost(paragraphs.size)) {
                                                                globalOffset += paragraphs[i].length + 2
                                                            }
                                                            val globalCharOffset = globalOffset + charOffsetInPara
                                                            val anchor = "p:${pIdx}:offset:${charOffsetInPara}:len:${passage.length}:global:${globalCharOffset}"
                                                            selectedPassageText = passage
                                                            selectedPassageAnchor = anchor

                                                            coroutineScope.launch {
                                                                viewModel.repository.toggleReaction(
                                                                    userId = effectiveUserId,
                                                                    targetType = "PASSAGE",
                                                                    targetId = currentPart.id,
                                                                    passageText = passage,
                                                                    emojiType = emoji
                                                                )
                                                                Toast.makeText(context, "${emoji.symbol} প্রতিক্রিয়া সম্পন্ন", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                        showContextualMenu = false
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("action_emoji_${emoji.name}")
                                                ) {
                                                    Text(emoji.symbol, fontSize = 16.sp)
                                                }
                                            }

                                            VerticalDivider(modifier = Modifier.height(20.dp), color = appColors.border)

                                            // Explicit Copy button - ONLY copies when user taps this!
                                            IconButton(
                                                onClick = {
                                                    pendingCopyAction?.invoke()
                                                    Toast.makeText(context, "উদ্ধৃতি ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                    showContextualMenu = false
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "কপি করুন", modifier = Modifier.size(16.dp), tint = appColors.text)
                                            }

                                            IconButton(
                                                onClick = { showContextualMenu = false },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", modifier = Modifier.size(14.dp), tint = appColors.textSecondary)
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

    // Dialog: Write Review & Emoji Reaction Together
    if (showNewReviewDialog) {
        var reviewText by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf<ReactionEmoji?>(ReactionEmoji.LOVE) }
        var rating by remember { mutableIntStateOf(5) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showNewReviewDialog = false },
            title = {
                Text("রিভিউ ও প্রতিক্রিয়া দিন", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appColors.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!selectedPassageText.isNullOrBlank()) {
                        val displayPassage = if (selectedPassageText!!.length > 100) {
                            selectedPassageText!!.take(100) + "..."
                        } else {
                            selectedPassageText!!
                        }
                        Text(
                            text = "নির্বাচিত উদ্ধৃতি: “$displayPassage”",
                            fontSize = 11.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(appColors.card)
                                .padding(8.dp)
                        )
                    }

                    // Emoji Selection Row
                    Text(
                        text = "প্রতিক্রিয়া (Emoji Reaction):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = appColors.text
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            ReactionEmoji.LOVE,
                            ReactionEmoji.HAHA,
                            ReactionEmoji.SAD,
                            ReactionEmoji.ANGRY
                        ).forEach { emoji ->
                            val isSelected = selectedEmoji == emoji
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedEmoji = if (isSelected) null else emoji
                                },
                                label = { Text(emoji.symbol, fontSize = 16.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Rating Stars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { rating = i },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$i তারা",
                                    tint = if (i <= rating) Color(0xFFF59E0B) else appColors.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("আপনার মূল্যবান মন্তব্য ও পর্যালোচনা...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("new_review_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.border
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val effectiveUserId = currentUser?.id ?: "guest"
                        val effectiveUserName = currentUser?.displayName ?: "সম্মানিত পাঠক"
                        if (currentStory != null && currentPart != null) {
                            isSubmitting = true
                            coroutineScope.launch {
                                // Save reaction if selected
                                if (selectedEmoji != null) {
                                    viewModel.repository.toggleReaction(
                                        userId = effectiveUserId,
                                        targetType = "PASSAGE",
                                        targetId = currentPart.id,
                                        passageText = selectedPassageText,
                                        emojiType = selectedEmoji!!
                                    )
                                }
                                // Save review
                                val commentToSave = if (reviewText.isNotBlank()) reviewText.trim()
                                                   else "${selectedEmoji?.symbol ?: "⭐"} পর্যালোচনা"
                                viewModel.repository.addReview(
                                    storyId = currentStory.id,
                                    partId = currentPart.id,
                                    userId = effectiveUserId,
                                    userName = effectiveUserName,
                                    passageText = selectedPassageText,
                                    passageAnchor = selectedPassageAnchor,
                                    reviewType = ReviewType.PASSAGE,
                                    rating = rating,
                                    comment = commentToSave
                                )
                                isSubmitting = false
                                Toast.makeText(context, "রিভিউ ও প্রতিক্রিয়া সফলভাবে জমা হয়েছে!", Toast.LENGTH_SHORT).show()
                                showNewReviewDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("submit_review_dialog_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.primary,
                        contentColor = appColors.onPrimary
                    ),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = appColors.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("জমা দিন", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewReviewDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            },
            containerColor = appColors.surface
        )
    }

    // Dialog: Whole Part Review (1-5 Star Rating + Comment)
    if (showWholePartReviewDialog) {
        var rating by remember { mutableIntStateOf(5) }
        var reviewText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWholePartReviewDialog = false },
            title = {
                Text("সমগ্র পর্বের পর্যালোচনা ও রেটিং", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appColors.text)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "রেটিং নির্বাচন করুন (১ - ৫ তারা):",
                        fontSize = 13.sp,
                        color = appColors.text,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (i in 1..5) {
                            IconButton(
                                onClick = { rating = i },
                                modifier = Modifier.testTag("rating_star_$i")
                            ) {
                                Icon(
                                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$i তারা",
                                    tint = if (i <= rating) Color(0xFFF59E0B) else appColors.textSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("পর্বটি সম্পর্কে আপনার মতামত...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("whole_part_review_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appColors.primary,
                            unfocusedBorderColor = appColors.border
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reviewText.isNotBlank() && currentUser != null && currentStory != null && currentPart != null) {
                            coroutineScope.launch {
                                viewModel.repository.addReview(
                                    storyId = currentStory.id,
                                    partId = currentPart.id,
                                    userId = currentUser!!.id,
                                    userName = currentUser!!.displayName,
                                    passageText = null,
                                    passageAnchor = null,
                                    reviewType = ReviewType.WHOLE_PART,
                                    rating = rating,
                                    comment = reviewText
                                )
                                Toast.makeText(context, "পর্ব পর্যালোচনা জমা হয়েছে", Toast.LENGTH_SHORT).show()
                                showWholePartReviewDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("submit_whole_part_dialog_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appColors.primary,
                        contentColor = appColors.onPrimary
                    )
                ) {
                    Text("জমা দিন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWholePartReviewDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            },
            containerColor = appColors.surface
        )
    }

    // Modal Bottom Sheet: Reviews & Replies List
    if (showReviewsDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showReviewsDrawer = false },
            containerColor = appColors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "পাঠক পর্যালোচনা ও আলোচনা (${currentPartReviews.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )
                    IconButton(onClick = { showReviewsDrawer = false }) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = appColors.text)
                    }
                }

                HorizontalDivider(color = appColors.border, modifier = Modifier.padding(vertical = 8.dp))

                if (currentPartReviews.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("এই পর্বে এখনো কোনো মন্তব্য নেই।", color = appColors.textSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentPartReviews) { review ->
                            val replies = allReplies.filter { it.reviewId == review.id }

                            ReviewItemCard(
                                review = review,
                                replies = replies,
                                currentUser = currentUser,
                                onSendReply = { replyText ->
                                    if (currentUser != null) {
                                        coroutineScope.launch {
                                            viewModel.repository.addReply(
                                                reviewId = review.id,
                                                userId = currentUser!!.id,
                                                userName = currentUser!!.displayName,
                                                userRole = currentUser!!.role,
                                                replyText = replyText
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Part List Selector
    if (showPartsDrawer) {
        ModalBottomSheet(
            onDismissRequest = { showPartsDrawer = false },
            containerColor = appColors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                val isPrivilegedUser = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.WRITER
                Text(
                    text = if (currentStory?.sequentialReadingEnabled == true && !isPrivilegedUser) 
                        "আপনার উন্মুক্ত পর্বসমূহ (${accessibleParts.size})" 
                    else 
                        "গল্পের সকল পর্ব (${accessibleParts.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.text,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accessibleParts) { part ->
                        val isCurrent = part.id == currentPart?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPartsDrawer = false
                                    viewModel.navigateTo(ScreenDestination.Reading(storyId, part.id))
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) appColors.primary.copy(alpha = 0.12f) else appColors.card
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) appColors.primary else appColors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "পর্ব ${part.partNumber}: ${part.title}",
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) appColors.primary else appColors.text
                                    )
                                    if (part.status == PartStatus.SCHEDULED) {
                                        Text(text = "আসন্ন প্রকাশনা", fontSize = 11.sp, color = Color(0xFFD97706))
                                    }
                                }
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = appColors.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Reading Preferences (Mood, Font, Spacing, Size)
    if (showReadingSettingsDialog) {
        ReadingSettingsDialog(
            readingMood = readingMood,
            onMoodChange = { viewModel.updateMood(it) },
            readingFont = readingFont,
            onFontChange = { viewModel.updateFont(it) },
            fontSize = fontSize,
            onFontSizeChange = { viewModel.updateFontSize(it) },
            lineSpacing = lineSpacing,
            onLineSpacingChange = { viewModel.updateLineSpacing(it) },
            onDismiss = { showReadingSettingsDialog = false }
        )
    }
}
}

@Composable
fun ReviewItemCard(
    review: PassageReview,
    replies: List<ReviewReply>,
    currentUser: User?,
    onSendReply: (String) -> Unit
) {
    val appColors = LocalAppColors.current
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.card),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, appColors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Reader Name, Date, Review Type & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(appColors.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = review.userName.take(1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = review.userName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text
                        )
                        val dateStr = SimpleDateFormat("d MMM, hh:mm a", Locale("bn")).format(Date(review.createdAt))
                        Text(text = dateStr, fontSize = 10.sp, color = appColors.textSecondary)
                    }
                }

                if (review.rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = "${review.rating}/৫", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = appColors.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "প্যারাগ্রাফ মন্তব্য",
                            fontSize = 10.sp,
                            color = appColors.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quoted excerpt if passage review
            if (!review.passageText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "“${review.passageText.take(100)}...”",
                    fontSize = 11.sp,
                    color = appColors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(appColors.surface)
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.comment,
                fontSize = 13.sp,
                color = appColors.text,
                lineHeight = 18.sp
            )

            // Threaded Replies
            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    replies.forEach { reply ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = appColors.surface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, appColors.border)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = reply.userName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.text
                                    )
                                    if (reply.userRole == UserRole.ADMIN || reply.userRole == UserRole.WRITER) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (reply.userRole == UserRole.ADMIN) Color(0xFF047857) else appColors.primary
                                        ) {
                                            Text(
                                                text = if (reply.userRole == UserRole.ADMIN) "অ্যাডমিন" else "লেখক",
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reply.replyText,
                                    fontSize = 12.sp,
                                    color = appColors.text
                                )
                            }
                        }
                    }
                }
            }

            // Reply trigger button
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { showReplyInput = !showReplyInput },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = appColors.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showReplyInput) "উত্তর বাতিল" else "উত্তর দিন", fontSize = 12.sp, color = appColors.primary)
                }
            }

            if (showReplyInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("উত্তর লিখুন...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onSendReply(replyText)
                                replyText = ""
                                showReplyInput = false
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

@Composable
fun ReadingSettingsDialog(
    readingMood: ReadingMood,
    onMoodChange: (ReadingMood) -> Unit,
    readingFont: ReadingFont,
    onFontChange: (ReadingFont) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    lineSpacing: Float,
    onLineSpacingChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val appColors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("পড়ার হরফ ও বিন্যাস সেটিংস", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appColors.text)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Reading Mood
                Text("পড়ার আবহাওয়া (Reading Mood):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReadingMood.entries.forEach { mood ->
                        val isSelected = readingMood == mood
                        OutlinedButton(
                            onClick = { onMoodChange(mood) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) appColors.primary.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) appColors.primary else appColors.border
                            ),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = mood.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) appColors.primary else appColors.text
                            )
                        }
                    }
                }

                HorizontalDivider(color = appColors.border)

                // Section 2: Font Selector
                Text("বাংলা ফন্ট নির্বাচন:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ReadingFont.entries.forEach { fontOption ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onFontChange(fontOption) },
                            color = if (readingFont == fontOption) appColors.primary.copy(alpha = 0.15f) else appColors.card,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (readingFont == fontOption) appColors.primary else appColors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fontOption.displayName,
                                    fontFamily = getFontFamily(fontOption),
                                    fontSize = 14.sp,
                                    color = appColors.text,
                                    fontWeight = if (readingFont == fontOption) FontWeight.Bold else FontWeight.Normal
                                )
                                if (readingFont == fontOption) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = appColors.border)

                // Section 3: Font Size with +/- step buttons & Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("অক্ষরের আকার:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onFontSizeChange((fontSize - 2f).coerceAtLeast(14f)) },
                            modifier = Modifier.size(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${fontSize.toInt()} sp",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        OutlinedButton(
                            onClick = { onFontSizeChange((fontSize + 2f).coerceAtMost(36f)) },
                            modifier = Modifier.size(34.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 14f..36f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = appColors.primary, activeTrackColor = appColors.primary)
                )

                // Section 4: Line spacing options
                Text("প্যারাগ্রাফের লাইনের ফাঁক:", fontSize = 13.sp, color = appColors.text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        1.3f to "কম (1.3x)",
                        1.6f to "মাঝারি (1.6x)",
                        2.0f to "বেশি (2.0x)"
                    ).forEach { (mult, label) ->
                        FilterChip(
                            selected = lineSpacing == mult,
                            onClick = { onLineSpacingChange(mult) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
            ) {
                Text("সম্পন্ন")
            }
        },
        containerColor = appColors.surface
    )
}

@Composable
fun EmptyReadingState(
    moodColors: ReadingMoodColors,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "কোনো পর্ব পাওয়া যায়নি",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = moodColors.text
            )
            Button(onClick = onBack) {
                Text("ফিরে যান")
            }
        }
    }
}


/**
 * Builds an AnnotatedString with persistent highlighting on the matching passage.
 * If highlightAnchor is present, highlights ONLY the specific paragraph and exact character range.
 */
private fun buildHighlightedParagraph(
    para: String,
    highlightPassage: String?,
    highlightAnchor: String? = null,
    paraIndex: Int = -1
): AnnotatedString {
    if (highlightPassage.isNullOrBlank() || !para.contains(highlightPassage)) {
        return AnnotatedString(para)
    }

    // Precise Anchor check: only highlight if this paragraph matches the anchor's paragraph index
    if (!highlightAnchor.isNullOrBlank()) {
        val anchorPara = highlightAnchor.substringAfter("p:").substringBefore(":").toIntOrNull()
        if (anchorPara != null && anchorPara != paraIndex) {
            // Anchor is for another paragraph — do NOT highlight occurrences in this paragraph!
            return AnnotatedString(para)
        }

        val anchorOffset = highlightAnchor.substringAfter("offset:").substringBefore(":").toIntOrNull()
        val anchorLen = highlightAnchor.substringAfter("len:").substringBefore(":").toIntOrNull() ?: highlightPassage.length
        if (anchorOffset != null && anchorOffset >= 0 && anchorOffset + anchorLen <= para.length) {
            val sub = para.substring(anchorOffset, anchorOffset + anchorLen)
            if (sub == highlightPassage) {
                return buildAnnotatedString {
                    if (anchorOffset > 0) {
                        append(para.substring(0, anchorOffset))
                    }
                    pushStyle(
                        SpanStyle(
                            background = Color(0xFFC4A77D).copy(alpha = 0.42f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(sub)
                    pop()
                    if (anchorOffset + anchorLen < para.length) {
                        append(para.substring(anchorOffset + anchorLen))
                    }
                }
            }
        }
    }

    // Fallback for general or legacy highlights:
    return buildAnnotatedString {
        var startIndex = 0
        while (startIndex < para.length) {
            val idx = para.indexOf(highlightPassage, startIndex)
            if (idx == -1) {
                append(para.substring(startIndex))
                break
            }
            if (idx > startIndex) {
                append(para.substring(startIndex, idx))
            }
            pushStyle(
                SpanStyle(
                    background = Color(0xFFC4A77D).copy(alpha = 0.42f),
                    fontWeight = FontWeight.SemiBold
                )
            )
            append(highlightPassage)
            pop()
            startIndex = idx + highlightPassage.length
        }
    }
}
