package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.data.repository.GolpoGhorRepository
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.BengaliEditorToolbar
import com.example.ui.components.TopNavBar
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriterDashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val currentUser by viewModel.currentUser.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val stories by viewModel.allStories.collectAsState()
    val allParts by viewModel.allParts.collectAsState()
    val readers by viewModel.allReaders.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val readingProgress by viewModel.allReadingProgress.collectAsState()
    val trashStories by viewModel.repository.trashStories.collectAsState(initial = emptyList())
    val trashParts by viewModel.repository.trashParts.collectAsState(initial = emptyList())
    val userOverrides by viewModel.userOverrides.collectAsState()
    val allAuditLogs by viewModel.allAuditLogs.collectAsState()

    val isAdmin = currentUser?.role == UserRole.ADMIN &&
            currentUser?.username.equals(GolpoGhorRepository.ADMIN_EMAIL, ignoreCase = true) &&
            (currentUser?.isEmailVerified == true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = if (isAdmin) listOf("প্রকাশনা", "ধারাবাহিক ও আনলক", "সম্পাদনা ও ট্র্যাশ", "রোল ও পাঠক") else listOf("প্রকাশনা", "ধারাবাহিক ও আনলক", "সম্পাদনা ও ট্র্যাশ", "পাঠক ট্র্যাকিং")

    // State for Publishing
    var publishMode by remember { mutableStateOf("STORY") } // "STORY" or "PART"
    var newStoryTitle by remember { mutableStateOf("") }
    var newStoryDesc by remember { mutableStateOf("") }
    var selectedCoverRes by remember { mutableStateOf<String?>("cover_nilkantha") }
    var customCoverUri by remember { mutableStateOf<String?>(null) }
    var newPartTitle by remember { mutableStateOf("") }
    var newPartContent by remember { mutableStateOf("") }
    var targetStoryId by remember { mutableStateOf("") }
    var publishOption by remember { mutableStateOf("PUBLISH") } // "PUBLISH", "DRAFT", "SCHEDULE"
    var schedulePreset by remember { mutableStateOf("10_MIN") } // "10_MIN", "30_MIN", "1_HOUR", "TOMORROW", "CUSTOM"
    val defaultScheduleCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 10)
        }
    }
    var scheduledYear by remember { mutableIntStateOf(defaultScheduleCal.get(Calendar.YEAR)) }
    var scheduledMonth by remember { mutableIntStateOf(defaultScheduleCal.get(Calendar.MONTH)) }
    var scheduledDay by remember { mutableIntStateOf(defaultScheduleCal.get(Calendar.DAY_OF_MONTH)) }
    var scheduledHour by remember { mutableIntStateOf(defaultScheduleCal.get(Calendar.HOUR_OF_DAY)) }
    var scheduledMinute by remember { mutableIntStateOf(defaultScheduleCal.get(Calendar.MINUTE)) }

    val photoPickerStoryCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customCoverUri = uri.toString()
        }
    }

    // State for Editing
    var editingStory by remember { mutableStateOf<Story?>(null) }
    var editStoryTitle by remember { mutableStateOf("") }
    var editStoryDesc by remember { mutableStateOf("") }
    var editStoryCoverRes by remember { mutableStateOf<String?>("cover_nilkantha") }
    var editStoryCoverUri by remember { mutableStateOf<String?>(null) }

    val photoPickerEditStoryCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editStoryCoverUri = uri.toString()
        }
    }

    var editingPart by remember { mutableStateOf<StoryPart?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair(Type, Id)

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "লেখক ড্যাশবোর্ড",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = appColors.surface,
                contentColor = appColors.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // TAB 1: Publication Creator (Story vs Part)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "আপনি কি প্রকাশ করতে চান?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.text
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = publishMode == "STORY",
                                    onClick = { publishMode = "STORY" },
                                    label = { Text("১. নতুন গল্প তৈরি করুন", fontSize = 13.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("publish_new_story_tab_button")
                                )
                                FilterChip(
                                    selected = publishMode == "PART",
                                    onClick = {
                                        publishMode = "PART"
                                        if (targetStoryId.isEmpty() && stories.isNotEmpty()) {
                                            targetStoryId = stories.first().id
                                        }
                                    },
                                    label = { Text("২. বিদ্যমান গল্পে নতুন পর্ব", fontSize = 13.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("publish_new_part_tab_button")
                                )
                            }
                        }

                        if (publishMode == "STORY") {
                            // New Story Fields
                            item {
                                OutlinedTextField(
                                    value = newStoryTitle,
                                    onValueChange = { newStoryTitle = it },
                                    label = { Text("গল্পের নাম / শিরোনাম") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_story_title_input"),
                                    singleLine = true
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = newStoryDesc,
                                    onValueChange = { newStoryDesc = it },
                                    label = { Text("সংক্ষিপ্ত বিবরণ / ভূমিকা") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .testTag("new_story_desc_input")
                                )
                            }
                            item {
                                Text("কভার ছবি নির্বাচন ও আপলোড:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Cover Preview
                                    Box(
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(98.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, appColors.border, RoundedCornerShape(8.dp))
                                            .background(appColors.card),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!customCoverUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = customCoverUri,
                                                contentDescription = "নির্বাচিত কভার ছবি",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            val resId = when (selectedCoverRes) {
                                                "cover_meghmallar" -> R.drawable.cover_meghmallar
                                                else -> R.drawable.cover_nilkantha
                                            }
                                            Image(
                                                painter = painterResource(id = resId),
                                                contentDescription = "প্রিসেট কভার ছবি",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerStoryCoverLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("গ্যালারি থেকে ছবি আপলোড", fontSize = 12.sp)
                                        }

                                        if (customCoverUri != null) {
                                            TextButton(
                                                onClick = {
                                                    customCoverUri = null
                                                    selectedCoverRes = "cover_nilkantha"
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ছবি সরান (ডিফল্ট রাখুন)", fontSize = 11.sp)
                                            }
                                        } else {
                                            Text("অথবা প্রিসেট কভার নির্বাচন করুন:", fontSize = 11.sp, color = appColors.textSecondary)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilterChip(
                                                    selected = selectedCoverRes == "cover_nilkantha",
                                                    onClick = {
                                                        selectedCoverRes = "cover_nilkantha"
                                                        customCoverUri = null
                                                    },
                                                    label = { Text("নীল নির্জনতা", fontSize = 11.sp) }
                                                )
                                                FilterChip(
                                                    selected = selectedCoverRes == "cover_meghmallar",
                                                    onClick = {
                                                        selectedCoverRes = "cover_meghmallar"
                                                        customCoverUri = null
                                                    },
                                                    label = { Text("বৃষ্টিভেজা নদী", fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // New Part: Target Story Selection
                            item {
                                Text("গল্প নির্বাচন করুন:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    stories.forEach { s ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { targetStoryId = s.id },
                                            color = if (targetStoryId == s.id) appColors.primary.copy(alpha = 0.15f) else appColors.card,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (targetStoryId == s.id) appColors.primary else appColors.border
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(s.title, fontWeight = FontWeight.Bold, color = appColors.text)
                                                if (targetStoryId == s.id) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = appColors.primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Part Title
                        item {
                            OutlinedTextField(
                                value = newPartTitle,
                                onValueChange = { newPartTitle = it },
                                label = { Text(if (publishMode == "STORY") "প্রথম পর্বের শিরোনাম" else "নতুন পর্বের শিরোনাম") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("part_title_input"),
                                singleLine = true
                            )
                        }

                        // Bengali Editor Helper Toolbar
                        item {
                            BengaliEditorToolbar(
                                onInsertSymbol = { sym ->
                                    newPartContent += sym
                                }
                            )
                        }

                        // Bengali Part Content Editor (Unicode safe)
                        item {
                            OutlinedTextField(
                                value = newPartContent,
                                onValueChange = { newPartContent = it },
                                label = { Text("পর্বের মূল লেখা (বাংলা ইউনিকোড)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .testTag("part_content_editor"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = appColors.primary,
                                    unfocusedBorderColor = appColors.border
                                )
                            )
                        }

                        // Publication Status Options
                        item {
                            Text("প্রকাশের ধরন:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = publishOption == "PUBLISH",
                                    onClick = { publishOption = "PUBLISH" },
                                    label = { Text("তাৎক্ষণিক প্রকাশ") },
                                    modifier = Modifier.testTag("publish_option_now")
                                )
                                FilterChip(
                                    selected = publishOption == "SCHEDULE",
                                    onClick = { publishOption = "SCHEDULE" },
                                    label = { Text("শিডিউল প্রকাশনা") },
                                    modifier = Modifier.testTag("publish_option_schedule")
                                )
                                FilterChip(
                                    selected = publishOption == "DRAFT",
                                    onClick = { publishOption = "DRAFT" },
                                    label = { Text("খসড়া (Draft)") },
                                    modifier = Modifier.testTag("publish_option_draft")
                                )
                            }
                        }

                        // Flexible Scheduled Date-Time Picker (Calendar & Clock / Time Table)
                        if (publishOption == "SCHEDULE") {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = appColors.card),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "শিডিউল প্রকাশনা নির্ধারণ (ক্যালেন্ডার ও টাইম টেবিল)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = appColors.text
                                            )
                                        }

                                        Text(
                                            text = "যেকোনো নির্দিষ্ট দিন বা সময়ে স্বয়ংক্রিয়ভাবে পর্ব প্রকাশের জন্য ক্যালেন্ডার ও ঘড়ি থেকে সময় ঠিক করুন:",
                                            fontSize = 12.sp,
                                            color = appColors.textSecondary
                                        )

                                        // Two interactive cards: Calendar Picker & Clock/Time Picker
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Calendar Date Picker Card
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        android.app.DatePickerDialog(
                                                            context,
                                                            { _, year, month, dayOfMonth ->
                                                                scheduledYear = year
                                                                scheduledMonth = month
                                                                scheduledDay = dayOfMonth
                                                                schedulePreset = "CUSTOM"
                                                            },
                                                            scheduledYear,
                                                            scheduledMonth,
                                                            scheduledDay
                                                        ).apply {
                                                            datePicker.minDate = System.currentTimeMillis() - 1000L
                                                        }.show()
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                                                border = androidx.compose.foundation.BorderStroke(1.5.dp, appColors.primary)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("তারিখ (ক্যালেন্ডার)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = appColors.primary)
                                                    }
                                                    val dateCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, scheduledYear)
                                                        set(Calendar.MONTH, scheduledMonth)
                                                        set(Calendar.DAY_OF_MONTH, scheduledDay)
                                                    }
                                                    val dateFmt = SimpleDateFormat("d MMMM yyyy", Locale("bn"))
                                                    Text(
                                                        text = dateFmt.format(dateCal.time),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = appColors.text
                                                    )
                                                    Text(
                                                        text = "ক্যালেন্ডার খুলুন ➜",
                                                        fontSize = 10.sp,
                                                        color = appColors.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            // Clock Time Picker Card
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        android.app.TimePickerDialog(
                                                            context,
                                                            { _, hourOfDay, minute ->
                                                                scheduledHour = hourOfDay
                                                                scheduledMinute = minute
                                                                schedulePreset = "CUSTOM"
                                                            },
                                                            scheduledHour,
                                                            scheduledMinute,
                                                            false
                                                        ).show()
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                                                border = androidx.compose.foundation.BorderStroke(1.5.dp, appColors.primary)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("সময় (ঘড়ি / টাইম)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = appColors.primary)
                                                    }
                                                    val timeCal = Calendar.getInstance().apply {
                                                        set(Calendar.HOUR_OF_DAY, scheduledHour)
                                                        set(Calendar.MINUTE, scheduledMinute)
                                                    }
                                                    val timeFmt = SimpleDateFormat("hh:mm a", Locale("bn"))
                                                    Text(
                                                        text = timeFmt.format(timeCal.time),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = appColors.text
                                                    )
                                                    Text(
                                                        text = "টাইম টেবিল খুলুন ➜",
                                                        fontSize = 10.sp,
                                                        color = appColors.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // Quick Presets
                                        Text("সহজ অপশন:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = appColors.textSecondary)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = schedulePreset == "10_MIN",
                                                onClick = {
                                                    schedulePreset = "10_MIN"
                                                    val c = Calendar.getInstance().apply { add(Calendar.MINUTE, 10) }
                                                    scheduledYear = c.get(Calendar.YEAR)
                                                    scheduledMonth = c.get(Calendar.MONTH)
                                                    scheduledDay = c.get(Calendar.DAY_OF_MONTH)
                                                    scheduledHour = c.get(Calendar.HOUR_OF_DAY)
                                                    scheduledMinute = c.get(Calendar.MINUTE)
                                                },
                                                label = { Text("১০ মিনিট পর", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = schedulePreset == "30_MIN",
                                                onClick = {
                                                    schedulePreset = "30_MIN"
                                                    val c = Calendar.getInstance().apply { add(Calendar.MINUTE, 30) }
                                                    scheduledYear = c.get(Calendar.YEAR)
                                                    scheduledMonth = c.get(Calendar.MONTH)
                                                    scheduledDay = c.get(Calendar.DAY_OF_MONTH)
                                                    scheduledHour = c.get(Calendar.HOUR_OF_DAY)
                                                    scheduledMinute = c.get(Calendar.MINUTE)
                                                },
                                                label = { Text("৩০ মিনিট পর", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = schedulePreset == "1_HOUR",
                                                onClick = {
                                                    schedulePreset = "1_HOUR"
                                                    val c = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                                                    scheduledYear = c.get(Calendar.YEAR)
                                                    scheduledMonth = c.get(Calendar.MONTH)
                                                    scheduledDay = c.get(Calendar.DAY_OF_MONTH)
                                                    scheduledHour = c.get(Calendar.HOUR_OF_DAY)
                                                    scheduledMinute = c.get(Calendar.MINUTE)
                                                },
                                                label = { Text("১ ঘণ্টা পর", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = schedulePreset == "TOMORROW",
                                                onClick = {
                                                    schedulePreset = "TOMORROW"
                                                    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                                                    scheduledYear = c.get(Calendar.YEAR)
                                                    scheduledMonth = c.get(Calendar.MONTH)
                                                    scheduledDay = c.get(Calendar.DAY_OF_MONTH)
                                                },
                                                label = { Text("আগামীকাল", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        val previewCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, scheduledYear)
                                            set(Calendar.MONTH, scheduledMonth)
                                            set(Calendar.DAY_OF_MONTH, scheduledDay)
                                            set(Calendar.HOUR_OF_DAY, scheduledHour)
                                            set(Calendar.MINUTE, scheduledMinute)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        val displayFormat = SimpleDateFormat("EEEE, d MMMM yyyy, hh:mm a", Locale("bn"))
                                        Surface(
                                            color = appColors.primary.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.primary.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    val targetStoryName = if (publishMode == "STORY") newStoryTitle.ifBlank { "নতুন গল্প" }
                                                                          else stories.find { it.id == targetStoryId }?.title ?: "বিদ্যমান গল্প"
                                                    val targetPartName = newPartTitle.ifBlank { "নতুন পর্ব" }
                                                    Text(
                                                        text = "গল্প: $targetStoryName • পর্ব: $targetPartName",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = appColors.text
                                                    )
                                                    Text(
                                                        text = "নির্ধারিত সময়: ${displayFormat.format(Date(previewCal.timeInMillis))}",
                                                        fontSize = 12.sp,
                                                        color = appColors.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "এই সময়ে পর্বটি স্বয়ংক্রিয়ভাবে গল্পঘরে পাঠকদের জন্য প্রকাশিত হবে।",
                                                        fontSize = 11.sp,
                                                        color = appColors.textSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Submit Button
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (newPartTitle.isBlank() || newPartContent.isBlank()) {
                                        Toast.makeText(context, "অনুগ্রহ করে শিরোনাম ও মূল লেখা লিখুন", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val status = when (publishOption) {
                                        "PUBLISH" -> PartStatus.PUBLISHED
                                        "SCHEDULE" -> PartStatus.SCHEDULED
                                        else -> PartStatus.DRAFT
                                    }

                                    val scheduledTime = if (status == PartStatus.SCHEDULED) {
                                        val cal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, scheduledYear)
                                            set(Calendar.MONTH, scheduledMonth)
                                            set(Calendar.DAY_OF_MONTH, scheduledDay)
                                            set(Calendar.HOUR_OF_DAY, scheduledHour)
                                            set(Calendar.MINUTE, scheduledMinute)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        if (cal.timeInMillis <= System.currentTimeMillis()) {
                                            Toast.makeText(context, "শিডিউলের সময় বর্তমান সময়ের পরবর্তী হতে হবে", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        cal.timeInMillis
                                    } else null

                                    coroutineScope.launch {
                                        if (publishMode == "STORY") {
                                            if (newStoryTitle.isBlank()) {
                                                Toast.makeText(context, "গল্পের নাম দিন", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                            viewModel.repository.createStory(
                                                title = newStoryTitle,
                                                description = newStoryDesc,
                                                coverResName = if (customCoverUri != null) null else selectedCoverRes,
                                                coverUri = customCoverUri,
                                                firstPartTitle = newPartTitle,
                                                firstPartContent = newPartContent,
                                                status = status,
                                                scheduledAt = scheduledTime,
                                                authorId = currentUser?.id ?: "author",
                                                authorName = currentUser?.displayName ?: "লেখক"
                                            )
                                            Toast.makeText(context, "নতুন গল্প সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_LONG).show()
                                            newStoryTitle = ""
                                            newStoryDesc = ""
                                            customCoverUri = null
                                            newPartTitle = ""
                                            newPartContent = ""
                                            viewModel.navigateTo(ScreenDestination.Home)
                                        } else {
                                            val tId = if (targetStoryId.isNotEmpty()) targetStoryId else stories.firstOrNull()?.id ?: ""
                                            if (tId.isEmpty()) {
                                                Toast.makeText(context, "বিদ্যমান গল্প পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                            viewModel.repository.createPart(
                                                storyId = tId,
                                                partTitle = newPartTitle,
                                                content = newPartContent,
                                                status = status,
                                                scheduledAt = scheduledTime
                                            )
                                            Toast.makeText(context, "নতুন পর্ব সফলভাবে যোগ করা হয়েছে!", Toast.LENGTH_LONG).show()
                                            newPartTitle = ""
                                            newPartContent = ""
                                            viewModel.navigateTo(ScreenDestination.Home)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_story_part_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = appColors.primary,
                                    contentColor = appColors.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (publishOption) {
                                        "PUBLISH" -> "অবিলম্বে প্রকাশ করুন"
                                        "SCHEDULE" -> "শিডিউল প্রকাশনা নিশ্চিত করুন"
                                        else -> "খসড়া হিসেবে সংরক্ষণ করুন"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: Sequential Reading Settings & Reader Manual Unlock
                    SequentialAndUnlockTab(
                        stories = stories,
                        allParts = allParts,
                        readers = readers,
                        readingProgress = readingProgress,
                        userOverrides = userOverrides,
                        auditLogs = allAuditLogs,
                        onSaveStorySettings = { storyId, seqEnabled, minPct, reqActive, minSecs, reqEnd, autoNext ->
                            coroutineScope.launch {
                                viewModel.updateStorySettings(
                                    storyId = storyId,
                                    sequentialReadingEnabled = seqEnabled,
                                    minReadingPercent = minPct,
                                    requireActiveReadingTime = reqActive,
                                    minActiveReadingSeconds = minSecs,
                                    requireEndOfPart = reqEnd,
                                    autoUnlockNextPart = autoNext
                                )
                            }
                        },
                        onSetManualUnlock = { userId, storyId, partId, partNumber, unlock, notes ->
                            coroutineScope.launch {
                                viewModel.setManualUnlock(
                                    userId = userId,
                                    storyId = storyId,
                                    partId = partId,
                                    partNumber = partNumber,
                                    unlock = unlock,
                                    notes = notes
                                )
                            }
                        }
                    )
                }

                2 -> {
                    // TAB 3: Manage Existing Stories & Parts, Reorder, Soft Delete & Trash
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "বিদ্যমান গল্প ও পর্ব সম্পাদনা",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.text
                            )
                        }

                        // Active Stories
                        items(stories) { story ->
                            val parts = allParts.filter { it.storyId == story.id }.sortedBy { it.partNumber }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Story Cover Thumbnail
                                            Box(
                                                modifier = Modifier
                                                    .width(42.dp)
                                                    .height(58.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .border(1.dp, appColors.border, RoundedCornerShape(6.dp))
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
                                                    val resId = when (story.coverResName) {
                                                        "cover_meghmallar" -> R.drawable.cover_meghmallar
                                                        else -> R.drawable.cover_nilkantha
                                                    }
                                                    Image(
                                                        painter = painterResource(id = resId),
                                                        contentDescription = story.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(story.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = appColors.text)
                                                Text("মোট পর্ব: ${parts.size}", fontSize = 12.sp, color = appColors.textSecondary)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Story Edit button (title, desc, cover photo)
                                            IconButton(
                                                onClick = {
                                                    editingStory = story
                                                    editStoryTitle = story.title
                                                    editStoryDesc = story.description
                                                    editStoryCoverRes = story.coverResName
                                                    editStoryCoverUri = story.coverUri
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "গল্প ও কভার সম্পাদনা", tint = appColors.primary)
                                            }

                                            // Story soft delete button
                                            IconButton(
                                                onClick = {
                                                    showDeleteConfirmDialog = Pair("STORY", story.id)
                                                }
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "ট্র্যাশে পাঠান", tint = Color.Red)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = appColors.border)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Story Parts list with Reorder & Edit
                                    Text("পর্বসমূহ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = appColors.textSecondary)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        parts.forEachIndexed { idx, part ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = appColors.card,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("পর্ব ${part.partNumber}: ${part.title}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = appColors.text)
                                                        val statusLabel = when (part.status) {
                                                            PartStatus.PUBLISHED -> "প্রকাশিত"
                                                            PartStatus.SCHEDULED -> "শিডিউল"
                                                            PartStatus.DRAFT -> "খসড়া"
                                                        }
                                                        Text("স্ট্যাটাস: $statusLabel", fontSize = 10.sp, color = appColors.textSecondary)
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        // Move Up
                                                        if (idx > 0) {
                                                            IconButton(
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        val prev = parts[idx - 1]
                                                                        viewModel.repository.updateStoryPart(part.copy(partNumber = prev.partNumber))
                                                                        viewModel.repository.updateStoryPart(prev.copy(partNumber = part.partNumber))
                                                                    }
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.ArrowUpward, contentDescription = "উপরে নিন", modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        // Move Down
                                                        if (idx < parts.size - 1) {
                                                            IconButton(
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        val next = parts[idx + 1]
                                                                        viewModel.repository.updateStoryPart(part.copy(partNumber = next.partNumber))
                                                                        viewModel.repository.updateStoryPart(next.copy(partNumber = part.partNumber))
                                                                    }
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.ArrowDownward, contentDescription = "নিচে নিন", modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        // Edit Part
                                                        IconButton(
                                                            onClick = { editingPart = part },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", modifier = Modifier.size(16.dp), tint = appColors.primary)
                                                        }
                                                        // Soft Delete Part
                                                        IconButton(
                                                            onClick = {
                                                                showDeleteConfirmDialog = Pair("PART", part.id)
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "পর্ব মুছুন", modifier = Modifier.size(16.dp), tint = Color.Red)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Trash / Recovery Section
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = appColors.card),
                                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.RestoreFromTrash, contentDescription = null, tint = appColors.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ট্র্যাশ ও রিকভারি বিন (মুছে ফেলা গল্প ও পর্ব)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = appColors.text)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (trashStories.isEmpty() && trashParts.isEmpty()) {
                                        Text("ট্র্যাশে কোনো আইটেম নেই।", fontSize = 12.sp, color = appColors.textSecondary)
                                    } else {
                                        trashStories.forEach { ts ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("গল্প: ${ts.title}", fontSize = 13.sp, color = appColors.text)
                                                Row {
                                                    TextButton(onClick = { coroutineScope.launch { viewModel.repository.restoreStory(ts.id) } }) {
                                                        Text("পুনরুদ্ধার", fontSize = 12.sp)
                                                    }
                                                    TextButton(onClick = { coroutineScope.launch { viewModel.repository.permanentlyDeleteStory(ts.id) } }) {
                                                        Text("স্থায়ীভাবে মুছুন", fontSize = 12.sp, color = Color.Red)
                                                    }
                                                }
                                            }
                                        }

                                        trashParts.forEach { tp ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("পর্ব: ${tp.title}", fontSize = 13.sp, color = appColors.text)
                                                Row {
                                                    TextButton(onClick = { coroutineScope.launch { viewModel.repository.restorePart(tp.id) } }) {
                                                        Text("পুনরুদ্ধার", fontSize = 12.sp)
                                                    }
                                                    TextButton(onClick = { coroutineScope.launch { viewModel.repository.permanentlyDeletePart(tp.id) } }) {
                                                        Text("স্থায়ীভাবে মুছুন", fontSize = 12.sp, color = Color.Red)
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

                3 -> {
                    // TAB 4: User & Role Management (Admin) / Reader Tracking
                    val managedUsers = allUsers.filter { it.username != GolpoGhorRepository.ADMIN_EMAIL }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = appColors.card),
                                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = appColors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isAdmin) "ব্যবহারকারী ও রোল নিয়ন্ত্রণ (অ্যাডমিন মোড)" else "পাঠক ট্র্যাকিং ড্যাশবোর্ড",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = appColors.text
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAdmin)
                                            "আপনি পাঠকদের 'লেখক' পদে উন্নীত করতে পারেন এবং লেখকদের পুনরায় 'পাঠক' পদে ফিরিয়ে আনতে পারেন।"
                                        else
                                            "শুধুমাত্র অ্যাডমিন (Mdnajrulislam771@gmail.com) ব্যবহারকারীদের ভূমিকা পরিবর্তনের ক্ষমতা রাখেন।",
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                        }

                        if (managedUsers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("এখনো কোনো অতিরিক্ত ব্যবহারকারী নিবন্ধিত হননি।", color = appColors.textSecondary)
                                }
                            }
                        } else {
                            items(managedUsers) { user ->
                                val userReadItems = readingProgress.filter { it.userId == user.id }
                                val isSuspended = user.status == UserStatus.SUSPENDED

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSuspended) Color.Gray else appColors.primary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(user.avatarLetter, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = appColors.text)
                                                        // Role Badge
                                                        val roleColor = when (user.role) {
                                                            UserRole.ADMIN -> Color(0xFF047857)
                                                            UserRole.WRITER -> appColors.primary
                                                            UserRole.READER -> Color(0xFF6B7280)
                                                        }
                                                        val roleName = when (user.role) {
                                                            UserRole.ADMIN -> "অ্যাডমিন"
                                                            UserRole.WRITER -> "লেখক"
                                                            UserRole.READER -> "পাঠক"
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = roleColor.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = roleName,
                                                                fontSize = 10.sp,
                                                                color = roleColor,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(user.username, fontSize = 11.sp, color = appColors.textSecondary)
                                                }
                                            }

                                            // Status Badge
                                            if (isSuspended) {
                                                Text("স্থগিত", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Admin Role Management Controls (Promote Reader to Writer, Writer to Reader)
                                        if (isAdmin) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (user.role == UserRole.READER) {
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                val res = viewModel.updateUserRole(user.id, UserRole.WRITER)
                                                                res.onSuccess {
                                                                    Toast.makeText(context, "${user.displayName}-কে লেখক হিসেবে উন্নীত করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                                }.onFailure {
                                                                    Toast.makeText(context, it.message ?: "ত্রুটি", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("promote_to_writer_button_${user.id}"),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = appColors.primary,
                                                            contentColor = appColors.onPrimary
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("লেখক বানান", fontSize = 12.sp)
                                                    }
                                                } else if (user.role == UserRole.WRITER) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                val res = viewModel.updateUserRole(user.id, UserRole.READER)
                                                                res.onSuccess {
                                                                    Toast.makeText(context, "${user.displayName}-কে পাঠক পদে পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                                }.onFailure {
                                                                    Toast.makeText(context, it.message ?: "ত্রুটি", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("demote_to_reader_button_${user.id}"),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp), tint = appColors.text)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("পাঠক বানান", fontSize = 12.sp, color = appColors.text)
                                                    }
                                                }

                                                // Status toggle (Active vs Suspended)
                                                OutlinedButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val newStatus = if (isSuspended) UserStatus.ACTIVE else UserStatus.SUSPENDED
                                                            val res = viewModel.updateUserStatus(user.id, newStatus)
                                                            res.onSuccess {
                                                                Toast.makeText(context, "${user.displayName}-এর অ্যাকাউন্ট ${if (newStatus == UserStatus.ACTIVE) "সচল" else "স্থগিত"} করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                            }.onFailure {
                                                                Toast.makeText(context, it.message ?: "ত্রুটি", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = if (isSuspended) Color(0xFF10B981) else Color(0xFFEF4444)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSuspended) Color(0xFF10B981) else Color(0xFFEF4444)
                                                    )
                                                ) {
                                                    Text(if (isSuspended) "সচল করুন" else "স্থগিত করুন", fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = appColors.border)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Reading Activity
                                        Text("পড়ার বিবরণ ও অগ্রগতি:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = appColors.textSecondary)
                                        if (userReadItems.isEmpty()) {
                                            Text("এখনো কোনো পর্ব পড়া শুরু করেননি।", fontSize = 12.sp, color = appColors.textSecondary)
                                        } else {
                                            userReadItems.forEach { prog ->
                                                val story = stories.find { it.id == prog.storyId }
                                                val part = allParts.find { it.id == prog.partId }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("${story?.title ?: "গল্প"} • ${part?.title ?: "পর্ব"}", fontSize = 12.sp, color = appColors.text)
                                                        Text("সময় ব্যয়: ${prog.activeSeconds} সেকেন্ড • সমাপ্তি: ${if (prog.isCompleted) "সম্পূর্ণ" else "চলমান"}", fontSize = 10.sp, color = appColors.textSecondary)
                                                    }
                                                    Text("${prog.progressPercent}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = appColors.primary)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Special Part Unlock Override Button
                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    allParts.forEach { p ->
                                                        viewModel.repository.setAccessOverride(user.id, p.id, true)
                                                    }
                                                    Toast.makeText(context, "${user.displayName}-এর জন্য সকল পর্ব উন্মুক্ত করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                                        ) {
                                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = appColors.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("বিশেষ অ্যাক্সেস উন্মুক্ত করুন (অ্যাক্সেস ওভাররাইড)", fontSize = 12.sp, color = appColors.text)
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

    // Modal: Edit Part Title, Content & Schedule
    if (editingPart != null) {
        var editTitle by remember { mutableStateOf(editingPart!!.title) }
        var editContent by remember { mutableStateOf(editingPart!!.content) }
        var editStatus by remember { mutableStateOf(editingPart!!.status) }
        val editPartCal = remember {
            Calendar.getInstance().apply {
                if (editingPart!!.scheduledAt != null && editingPart!!.scheduledAt!! > 0) {
                    timeInMillis = editingPart!!.scheduledAt!!
                } else {
                    add(Calendar.MINUTE, 10)
                }
            }
        }
        var editYear by remember { mutableIntStateOf(editPartCal.get(Calendar.YEAR)) }
        var editMonth by remember { mutableIntStateOf(editPartCal.get(Calendar.MONTH)) }
        var editDay by remember { mutableIntStateOf(editPartCal.get(Calendar.DAY_OF_MONTH)) }
        var editHour by remember { mutableIntStateOf(editPartCal.get(Calendar.HOUR_OF_DAY)) }
        var editMinute by remember { mutableIntStateOf(editPartCal.get(Calendar.MINUTE)) }

        AlertDialog(
            onDismissRequest = { editingPart = null },
            title = { Text("পর্ব সম্পাদনা ও প্রকাশনা", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appColors.text) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("পর্বের শিরোনাম") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Publication Status Selector
                    Text("স্ট্যাটাস:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = appColors.text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = editStatus == PartStatus.PUBLISHED,
                            onClick = { editStatus = PartStatus.PUBLISHED },
                            label = { Text("প্রকাশিত", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = editStatus == PartStatus.SCHEDULED,
                            onClick = { editStatus = PartStatus.SCHEDULED },
                            label = { Text("শিডিউল", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = editStatus == PartStatus.DRAFT,
                            onClick = { editStatus = PartStatus.DRAFT },
                            label = { Text("খসড়া", fontSize = 11.sp) }
                        )
                    }

                    // Calendar & Clock Picker for Scheduled status
                    if (editStatus == PartStatus.SCHEDULED) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            editYear = y
                                            editMonth = m
                                            editDay = d
                                        },
                                        editYear,
                                        editMonth,
                                        editDay
                                    ).apply {
                                        datePicker.minDate = System.currentTimeMillis() - 1000L
                                    }.show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val c = Calendar.getInstance().apply { set(Calendar.YEAR, editYear); set(Calendar.MONTH, editMonth); set(Calendar.DAY_OF_MONTH, editDay) }
                                Text(SimpleDateFormat("d MMM yyyy", Locale("bn")).format(c.time), fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            editHour = h
                                            editMinute = min
                                        },
                                        editHour,
                                        editMinute,
                                        false
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, editHour); set(Calendar.MINUTE, editMinute) }
                                Text(SimpleDateFormat("hh:mm a", Locale("bn")).format(c.time), fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("মূল লেখা") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newScheduledAt = if (editStatus == PartStatus.SCHEDULED) {
                                val c = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, editYear)
                                    set(Calendar.MONTH, editMonth)
                                    set(Calendar.DAY_OF_MONTH, editDay)
                                    set(Calendar.HOUR_OF_DAY, editHour)
                                    set(Calendar.MINUTE, editMinute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                if (c.timeInMillis <= System.currentTimeMillis()) {
                                    Toast.makeText(context, "শিডিউলের সময় বর্তমান সময়ের পরবর্তী হতে হবে", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                c.timeInMillis
                            } else if (editStatus == PartStatus.PUBLISHED) {
                                null
                            } else editingPart!!.scheduledAt

                            viewModel.repository.updateStoryPart(
                                editingPart!!.copy(
                                    title = editTitle,
                                    content = editContent,
                                    status = editStatus,
                                    scheduledAt = newScheduledAt
                                )
                            )
                            editingPart = null
                            Toast.makeText(context, "পর্ব হালনাগাদ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPart = null }) {
                    Text("বাতিল")
                }
            },
            containerColor = appColors.surface
        )
    }

    // Modal: Edit Story Details & Cover Photo
    if (editingStory != null) {
        AlertDialog(
            onDismissRequest = { editingStory = null },
            title = {
                Text(
                    text = "গল্প ও কভার ছবি সম্পাদনা",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editStoryTitle,
                        onValueChange = { editStoryTitle = it },
                        label = { Text("গল্পের নাম / শিরোনাম") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editStoryDesc,
                        onValueChange = { editStoryDesc = it },
                        label = { Text("সংক্ষিপ্ত বিবরণ / ভূমিকা") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    )

                    Text(
                        text = "কভার ছবি পরিবর্তন ও আপলোড:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.text
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, appColors.border, RoundedCornerShape(8.dp))
                                .background(appColors.card),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!editStoryCoverUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = editStoryCoverUri,
                                    contentDescription = "কভার",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val resId = when (editStoryCoverRes) {
                                    "cover_meghmallar" -> R.drawable.cover_meghmallar
                                    else -> R.drawable.cover_nilkantha
                                }
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "কভার",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerEditStoryCoverLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ছবি আপলোড করুন", fontSize = 11.sp)
                            }

                            if (!editStoryCoverUri.isNullOrEmpty()) {
                                TextButton(
                                    onClick = {
                                        editStoryCoverUri = null
                                        editStoryCoverRes = "cover_nilkantha"
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ছবি সরান", fontSize = 11.sp)
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = editStoryCoverRes == "cover_nilkantha",
                                        onClick = {
                                            editStoryCoverRes = "cover_nilkantha"
                                            editStoryCoverUri = null
                                        },
                                        label = { Text("নীল নির্জনতা", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = editStoryCoverRes == "cover_meghmallar",
                                        onClick = {
                                            editStoryCoverRes = "cover_meghmallar"
                                            editStoryCoverUri = null
                                        },
                                        label = { Text("বৃষ্টিভেজা নদী", fontSize = 10.sp) }
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
                        if (editStoryTitle.trim().isEmpty()) {
                            Toast.makeText(context, "গল্পের নাম খালি রাখা যাবে না", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateStoryDetails(
                            storyId = editingStory!!.id,
                            title = editStoryTitle,
                            description = editStoryDesc,
                            coverResName = if (editStoryCoverUri != null) null else editStoryCoverRes,
                            coverUri = editStoryCoverUri
                        ) { res ->
                            res.onSuccess {
                                Toast.makeText(context, "গল্প ও কভার সফলভাবে হালনাগাদ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                editingStory = null
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "ত্রুটি ঘটেছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStory = null }) {
                    Text("বাতিল")
                }
            },
            containerColor = appColors.surface
        )
    }

    // Modal: Delete Confirmation (Soft Delete to Trash)
    if (showDeleteConfirmDialog != null) {
        val (type, id) = showDeleteConfirmDialog!!

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("মুছে ফেলার নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text("আপনি কি নিশ্চিতভাবে এই ${if (type == "STORY") "গল্পটি" else "পর্বটি"} ট্র্যাশে পাঠাতে চান? পরবর্তীতে রিকভারি বিন থেকে তা পুনরুদ্ধার করা যাবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (type == "STORY") {
                                viewModel.repository.softDeleteStory(id)
                            } else {
                                viewModel.repository.softDeletePart(id)
                            }
                            showDeleteConfirmDialog = null
                            Toast.makeText(context, "ট্র্যাশে পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("ট্র্যাশে পাঠান", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("বাতিল")
                }
            },
            containerColor = appColors.surface
        )
    }
}

@Composable
fun SequentialAndUnlockTab(
    stories: List<Story>,
    allParts: List<StoryPart>,
    readers: List<User>,
    readingProgress: List<ReadingProgress>,
    userOverrides: List<UserAccessOverride>,
    auditLogs: List<UnlockAuditLog>,
    onSaveStorySettings: (String, Boolean, Int, Boolean, Long, Boolean, Boolean) -> Unit,
    onSetManualUnlock: (userId: String, storyId: String, partId: String, partNumber: Int, unlock: Boolean, notes: String?) -> Unit
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current

    var selectedStoryId by remember(stories) { mutableStateOf(stories.firstOrNull()?.id ?: "") }
    val currentStory = stories.find { it.id == selectedStoryId } ?: stories.firstOrNull()
    val partsForStory = remember(allParts, currentStory?.id) {
        allParts.filter { it.storyId == currentStory?.id }.sortedBy { it.partNumber }
    }

    // Configurable state
    var seqEnabled by remember(currentStory?.id) { mutableStateOf(currentStory?.sequentialReadingEnabled ?: true) }
    var minReadingPct by remember(currentStory?.id) { mutableIntStateOf(currentStory?.minReadingPercent ?: 90) }
    var reqActiveTime by remember(currentStory?.id) { mutableStateOf(currentStory?.requireActiveReadingTime ?: true) }
    var minActiveSecs by remember(currentStory?.id) { mutableLongStateOf(currentStory?.minActiveReadingSeconds ?: 30L) }
    var reqEndOfPart by remember(currentStory?.id) { mutableStateOf(currentStory?.requireEndOfPart ?: true) }
    var autoUnlockNext by remember(currentStory?.id) { mutableStateOf(currentStory?.autoUnlockNextPart ?: true) }

    // Manual unlock reader selection
    var selectedReaderId by remember(readers) { mutableStateOf(readers.firstOrNull()?.id ?: "") }
    var readerSearchText by remember { mutableStateOf("") }
    val filteredReaders = remember(readers, readerSearchText) {
        if (readerSearchText.isBlank()) readers
        else readers.filter {
            it.displayName.contains(readerSearchText, ignoreCase = true) ||
                    it.username.contains(readerSearchText, ignoreCase = true)
        }
    }
    val selectedReader = readers.find { it.id == selectedReaderId } ?: readers.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: Story Selection & Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = appColors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ধারাবাহিক পঠন ও আনলক সেটিংস",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text
                        )
                    }
                    Text(
                        text = "গল্প অনুযায়ী পাঠকদের পর্ব উন্মোচন শর্তাবলী ও গোপনীয়তা নিয়ন্ত্রণ করুন।",
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Story Selection Chips
                    Text("গল্প নির্বাচন করুন:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.text)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stories.forEach { story ->
                            FilterChip(
                                selected = selectedStoryId == story.id,
                                onClick = {
                                    selectedStoryId = story.id
                                    seqEnabled = story.sequentialReadingEnabled
                                    minReadingPct = story.minReadingPercent
                                    reqActiveTime = story.requireActiveReadingTime
                                    minActiveSecs = story.minActiveReadingSeconds
                                    reqEndOfPart = story.requireEndOfPart
                                    autoUnlockNext = story.autoUnlockNextPart
                                },
                                label = { Text(story.title, maxLines = 1, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = appColors.border)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentStory != null) {
                        // 1. Sequential Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ধারাবাহিক পঠন মোড", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                Text("সক্রিয় থাকলে পাঠকরা এক পর্ব সম্পন্ন না করে পরের পর্ব দেখতে পাবেন না।", fontSize = 11.sp, color = appColors.textSecondary)
                            }
                            Switch(
                                checked = seqEnabled,
                                onCheckedChange = { seqEnabled = it },
                                modifier = Modifier.testTag("sequential_mode_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Auto Unlock Next Part
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("স্বয়ংক্রিয় পরবর্তী পর্ব আনলক", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                Text("শর্তাবলী পূরণ হলে স্বয়ংক্রিয়ভাবে পরের পর্ব উন্মুক্ত হবে।", fontSize = 11.sp, color = appColors.textSecondary)
                            }
                            Switch(
                                checked = autoUnlockNext,
                                onCheckedChange = { autoUnlockNext = it },
                                modifier = Modifier.testTag("auto_unlock_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Minimum Reading Percentage
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("সর্বনিম্ন পঠন শতাংশ:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = appColors.text)
                                Text("${minReadingPct}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.primary)
                            }
                            Slider(
                                value = minReadingPct.toFloat(),
                                onValueChange = { minReadingPct = it.toInt() },
                                valueRange = 10f..100f,
                                steps = 17,
                                modifier = Modifier.testTag("min_reading_pct_slider")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Active Reading Time Verification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("সক্রিয় পঠন সময় যাচাইকরণ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                Text("পাঠক নির্দিষ্ট সময় ব্যয় করলেই কেবল পর্ব সম্পন্ন গণ্য হবে।", fontSize = 11.sp, color = appColors.textSecondary)
                            }
                            Switch(
                                checked = reqActiveTime,
                                onCheckedChange = { reqActiveTime = it },
                                modifier = Modifier.testTag("require_active_time_switch")
                            )
                        }

                        if (reqActiveTime) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("ন্যূনতম সময় (সেকেন্ড):", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = appColors.text)
                                    Text("${minActiveSecs} সেকেন্ড", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.primary)
                                }
                                Slider(
                                    value = minActiveSecs.toFloat(),
                                    onValueChange = { minActiveSecs = it.toLong() },
                                    valueRange = 5f..180f,
                                    steps = 34,
                                    modifier = Modifier.testTag("min_active_seconds_slider")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 5. End-of-Part Requirement
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("পর্বের শেষপ্রান্তে পৌঁছানো বাধ্যতামূলক", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.text)
                                Text("পাঠককে অবশ্যই স্ক্রল করে পর্বের একদম শেষে যেতে হবে।", fontSize = 11.sp, color = appColors.textSecondary)
                            }
                            Switch(
                                checked = reqEndOfPart,
                                onCheckedChange = { reqEndOfPart = it },
                                modifier = Modifier.testTag("require_end_of_part_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onSaveStorySettings(
                                    currentStory.id,
                                    seqEnabled,
                                    minReadingPct,
                                    reqActiveTime,
                                    minActiveSecs,
                                    reqEndOfPart,
                                    autoUnlockNext
                                )
                                Toast.makeText(context, "'${currentStory.title}' সেটিংস সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_story_settings_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = appColors.primary,
                                contentColor = appColors.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সেটিংস সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // SECTION 2: Reader Access & Manual Unlock
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = appColors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পাঠক অ্যাক্সেস ও ম্যানুয়াল আনলক",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text
                        )
                    }
                    Text(
                        text = "যেকোনো পাঠকের জন্য নির্দিষ্ট পর্ব ম্যানুয়ালি উন্মুক্ত বা প্রত্যাহার করুন।",
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Reader Search Input
                    OutlinedTextField(
                        value = readerSearchText,
                        onValueChange = { readerSearchText = it },
                        label = { Text("পাঠক খুঁজুন (নাম বা ইমেইল)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_reader_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Readers Horizontal List
                    if (filteredReaders.isEmpty()) {
                        Text("কোনো পাঠক পাওয়া যায়নি।", fontSize = 12.sp, color = appColors.textSecondary)
                    } else {
                        Text("পাঠক নির্বাচন করুন:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredReaders.take(5).forEach { rdr ->
                                FilterChip(
                                    selected = selectedReaderId == rdr.id,
                                    onClick = { selectedReaderId = rdr.id },
                                    label = { Text(rdr.displayName, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    if (selectedReader != null && currentStory != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = appColors.border)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Reader Progress Snapshot
                        val readerStoryProgress = readingProgress.filter { it.userId == selectedReader.id && it.storyId == currentStory.id }
                        val completedCount = readerStoryProgress.count { it.isCompleted }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = appColors.card,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${selectedReader.displayName} (${selectedReader.username})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = appColors.text
                                    )
                                    Text(
                                        text = "গল্প: ${currentStory.title} • সম্পন্ন: ${completedCount}/${partsForStory.size} পর্ব",
                                        fontSize = 11.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("পর্বভিত্তিক ম্যানুয়াল অ্যাক্সেস নিয়ন্ত্রণ:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.text)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            partsForStory.forEach { part ->
                                val prog = readerStoryProgress.find { it.partId == part.id }
                                val override = userOverrides.find { it.userId == selectedReader.id && it.partId == part.id && it.isUnlocked }
                                val isCompleted = prog?.isCompleted == true
                                val isManuallyUnlocked = override != null

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = appColors.card,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, appColors.border)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "পর্ব ${part.partNumber}: ${part.title}",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = appColors.text
                                            )
                                            Row(
                                                modifier = Modifier.padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isCompleted) {
                                                    Text("✓ সম্পন্ন", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                                                } else if (isManuallyUnlocked) {
                                                    Text("★ ম্যানুয়ালি উন্মুক্ত", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                                } else {
                                                    Text("লক করা", fontSize = 11.sp, color = appColors.textSecondary)
                                                }

                                                if (prog != null && prog.progressPercent > 0) {
                                                    Text("• পড়া হয়েছে: ${prog.progressPercent}%", fontSize = 10.sp, color = appColors.textSecondary)
                                                }

                                                if (part.status != PartStatus.PUBLISHED) {
                                                    Text("(শিডিউল/অপ্রকাশিত)", fontSize = 10.sp, color = Color(0xFFDC2626))
                                                }
                                            }
                                        }

                                        // Action Button: Unlock or Revoke
                                        if (isManuallyUnlocked) {
                                            OutlinedButton(
                                                onClick = {
                                                    onSetManualUnlock(
                                                        selectedReader.id,
                                                        currentStory.id,
                                                        part.id,
                                                        part.partNumber,
                                                        false,
                                                        "অ্যাক্সেস প্রত্যাহার করা হয়েছে"
                                                    )
                                                    Toast.makeText(context, "পর্ব ${part.partNumber} এর অ্যাক্সেস প্রত্যাহার করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("প্রত্যাহার", fontSize = 11.sp)
                                            }
                                        } else if (!isCompleted) {
                                            Button(
                                                onClick = {
                                                    onSetManualUnlock(
                                                        selectedReader.id,
                                                        currentStory.id,
                                                        part.id,
                                                        part.partNumber,
                                                        true,
                                                        "লেখক কর্তৃক ম্যানুয়াল আনলক"
                                                    )
                                                    Toast.makeText(context, "পর্ব ${part.partNumber} ম্যানুয়ালি আনলক করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = appColors.primary,
                                                    contentColor = appColors.onPrimary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("আনলক", fontSize = 11.sp)
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

        // SECTION 3: Persistent Audit Logs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = appColors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ম্যানুয়াল আনলক অডিট হিস্টোরি (${auditLogs.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.text
                        )
                    }
                    Text(
                        text = "লেখক ও অ্যাডমিন কর্তৃক প্রদত্ত বা প্রত্যাহারকৃত সকল আনলকের স্থায়ী লগ।",
                        fontSize = 11.sp,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    if (auditLogs.isEmpty()) {
                        Text(
                            text = "এখনো কোনো ম্যানুয়াল আনলক রেকর্ড নথিভুক্ত হয়নি।",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            auditLogs.take(10).forEach { log ->
                                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale("bn")).format(Date(log.timestamp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = appColors.card,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val actionLabel = if (log.action == "MANUAL_UNLOCK") "ম্যানুয়াল আনলক" else "অ্যাক্সেস প্রত্যাহার"
                                            val actionColor = if (log.action == "MANUAL_UNLOCK") Color(0xFF047857) else Color.Red
                                            Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = actionColor)
                                            Text(dateStr, fontSize = 10.sp, color = appColors.textSecondary)
                                        }
                                        Text(
                                            text = "কর্তা: ${log.actorName} • পাঠক আইডি: ${log.userId} • পর্ব #${log.partNumber}",
                                            fontSize = 11.sp,
                                            color = appColors.text,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        if (!log.notes.isNullOrBlank()) {
                                            Text("মন্তব্য: ${log.notes}", fontSize = 10.sp, color = appColors.textSecondary)
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
}
