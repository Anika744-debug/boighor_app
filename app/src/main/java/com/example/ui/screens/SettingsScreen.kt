package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.data.model.AppTheme
import com.example.data.model.ReadingFont
import com.example.data.model.ReadingMood
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.TopNavBar
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalReadingFont
import com.example.ui.theme.getColorsForTheme
import com.example.ui.theme.getFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current
    val currentUser by viewModel.currentUser.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val readingMood by viewModel.readingMood.collectAsState()
    val readingFont by viewModel.readingFont.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val lineSpacing by viewModel.lineSpacing.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editDisplayName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var editAvatarUri by remember(currentUser) { mutableStateOf(currentUser?.avatarUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editAvatarUri = uri.toString()
        }
    }

    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    if (showEditProfileDialog && currentUser != null) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "প্রোফাইল তথ্য সম্পাদনা",
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalReadingFont.current,
                    color = appColors.text
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar Preview & Change Action
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
                                text = editDisplayName.take(1).ifEmpty { "গ" },
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ছবি নির্বাচন", fontSize = 12.sp)
                        }

                        if (!editAvatarUri.isNullOrEmpty()) {
                            TextButton(
                                onClick = { editAvatarUri = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ছবি সরান", fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("আপনার নাম (Display Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editDisplayName.trim().isEmpty()) {
                            Toast.makeText(context, "নাম খালি রাখা যাবে না", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateUserProfile(
                            displayName = editDisplayName,
                            avatarUri = editAvatarUri
                        ) { result ->
                            result.onSuccess {
                                Toast.makeText(context, "প্রোফাইল সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                                showEditProfileDialog = false
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "ত্রুটি ঘটেছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primary)
                ) {
                    Text("সংরক্ষণ", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("বাতিল", color = appColors.textSecondary)
                }
            },
            containerColor = appColors.surface
        )
    }

    Scaffold(
        topBar = {
            TopNavBar(
                title = "সেটিংস",
                currentUser = currentUser,
                showBackButton = true,
                onBackClick = { viewModel.navigateTo(ScreenDestination.Home) },
                currentMood = readingMood,
                onMoodSelected = { viewModel.updateMood(it) },
                showMoodSelector = true,
                onNotificationClick = { viewModel.navigateTo(ScreenDestination.Notifications) },
                onSettingsClick = {},
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Account Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(appColors.primary)
                                    .border(1.dp, appColors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!currentUser?.avatarUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = currentUser?.avatarUri,
                                        contentDescription = currentUser?.displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = currentUser?.avatarLetter ?: "গ",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName ?: "অতিথি",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    fontFamily = LocalReadingFont.current,
                                    color = appColors.text
                                )
                                val roleLabel = when (currentUser?.role) {
                                    UserRole.ADMIN -> "অ্যাডমিন অ্যাকাউন্ট"
                                    UserRole.WRITER -> "লেখক অ্যাকাউন্ট"
                                    UserRole.READER -> "পাঠক অ্যাকাউন্ট"
                                    null -> "পাঠক অ্যাকাউন্ট"
                                }
                                val verifiedLabel = if (currentUser?.isEmailVerified == true) " • যাচাইকৃত" else ""
                                Text(
                                    text = "$roleLabel • ${currentUser?.username ?: "লগইন করা নেই"}$verifiedLabel",
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (currentUser != null) {
                                FilledTonalButton(
                                    onClick = {
                                        editDisplayName = currentUser?.displayName ?: ""
                                        editAvatarUri = currentUser?.avatarUri
                                        showEditProfileDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_profile_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("এডিট", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateTo(ScreenDestination.Login) },
                                modifier = Modifier.testTag("switch_account_button"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(if (currentUser != null) "বদলান" else "লগইন", fontSize = 12.sp, color = appColors.primary)
                            }
                        }
                    }
                }
            }

            // SECTION 1: Website Color Themes (Separate Section as mandated)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = appColors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "অ্যাপ ও ওয়েবসাইট কালার থিম (Website Color Theme)",
                                fontFamily = LocalReadingFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = appColors.text
                            )
                        }
                        Text(
                            text = "এটি সামগ্রিক অ্যাপ্লিকেশনের রঙ পরিবর্তন করে এবং রিডিং মুড থেকে সম্পূর্ণ স্বতন্ত্র।",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Dark Mode Toggle Row
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("dark_mode_toggle_surface"),
                            color = appColors.card,
                            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = appColors.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ওয়েবসাইট ডার্ক মোড (Dark Mode)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = appColors.text
                                        )
                                        Text(
                                            text = if (isDarkMode) "গাঢ় অন্ধকার মুড সক্রিয় (প্রতিটি থিমে আলাদা রূপ)" else "উজ্জ্বল মোড সক্রিয়",
                                            fontSize = 11.sp,
                                            color = appColors.textSecondary
                                        )
                                    }
                                }
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { viewModel.setDarkMode(it) },
                                    modifier = Modifier.testTag("dark_mode_switch")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 7 themes list
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTheme.entries.forEach { theme ->
                                val tColors = getColorsForTheme(theme, isDarkMode)
                                val isSelected = appTheme == theme

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.updateTheme(theme) }
                                        .testTag("theme_option_${theme.name}"),
                                    color = if (isSelected) tColors.primary.copy(alpha = 0.12f) else tColors.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) tColors.primary else tColors.border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Theme Preview Swatch
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(tColors.background)
                                                    .border(2.dp, tColors.primary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = theme.displayName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = tColors.text
                                                )
                                                Text(
                                                    text = theme.description,
                                                    fontSize = 11.sp,
                                                    color = tColors.textSecondary
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "নির্বাচিত",
                                                tint = tColors.primary,
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

            // SECTION 2: Reading Moods (Separate from Website theme)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ColorLens, contentDescription = null, tint = appColors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ডিফল্ট পড়ার মুড (Reading Mood)",
                                fontFamily = LocalReadingFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = appColors.text
                            )
                        }
                        Text(
                            text = "গল্প পড়ার পেজের আবহাওয়া: লাইট, ডার্ক, সেপিয়া (#F4ECD8), বা ওয়ালনাট (#8B6248)।",
                            fontSize = 12.sp,
                            color = appColors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReadingMood.entries.forEach { mood ->
                                val isSelected = readingMood == mood
                                OutlinedButton(
                                    onClick = { viewModel.updateMood(mood) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("mood_option_${mood.name}"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) appColors.primary.copy(alpha = 0.12f) else Color.Transparent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) appColors.primary else appColors.border
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = mood.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) appColors.primary else appColors.text
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Bengali Typography & Reading Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = appColors.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, appColors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FontDownload, contentDescription = null, tint = appColors.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "বাংলা ফন্ট ও টাইপোগ্রাফি",
                                fontFamily = LocalReadingFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = appColors.text
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ReadingFont.entries.forEach { fontOption ->
                            val isSelected = readingFont == fontOption
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.updateFont(fontOption) }
                                    .testTag("font_option_${fontOption.name}"),
                                color = if (isSelected) appColors.primary.copy(alpha = 0.12f) else appColors.card,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) appColors.primary else appColors.border
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fontOption.displayName,
                                            fontFamily = getFontFamily(fontOption),
                                            fontSize = 15.sp,
                                            color = appColors.text,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "“মেঘমল্লার সুরে বৃষ্টিভেজা নিঃশব্দ রাত।”",
                                            fontFamily = getFontFamily(fontOption),
                                            fontSize = 13.sp,
                                            color = appColors.textSecondary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = appColors.primary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Font Size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ডিফল্ট অক্ষরের মাপ:", fontSize = 13.sp, color = appColors.text)
                            Text("${fontSize.toInt()} sp", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.primary)
                        }
                        Slider(
                            value = fontSize,
                            onValueChange = { viewModel.updateFontSize(it) },
                            valueRange = 14f..28f,
                            steps = 14
                        )
                    }
                }
            }

            // Logout Button
            item {
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logout_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("লগ আউট করুন")
                }
            }
        }
    }
}
