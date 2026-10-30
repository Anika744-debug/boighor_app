package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.GolpoGhorBottomBar
import com.example.ui.components.MoreBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.GolpoGhorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val readingMood by viewModel.readingMood.collectAsState()
            val readingFont by viewModel.readingFont.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()

            var showMoreSheet by remember { mutableStateOf(false) }

            // Bottom bar is visible everywhere EXCEPT during focused Reading
            val shouldShowBottomBar = currentScreen !is ScreenDestination.Reading

            GolpoGhorTheme(
                appTheme = appTheme,
                isDarkMode = isDarkMode,
                readingMood = readingMood,
                readingFont = readingFont
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            if (shouldShowBottomBar) {
                                GolpoGhorBottomBar(
                                    currentDestination = currentScreen,
                                    onNavigate = { destination ->
                                        viewModel.navigateTo(destination)
                                    },
                                    onOpenMore = {
                                        showMoreSheet = true
                                    }
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (shouldShowBottomBar) innerPadding.calculateBottomPadding() else 0.dp
                                )
                        ) {
                            Crossfade(targetState = currentScreen, label = "ScreenTransition") { destination ->
                                when (destination) {
                                    is ScreenDestination.Home -> HomeScreen(viewModel = viewModel)
                                    is ScreenDestination.Library -> LibraryScreen(viewModel = viewModel)
                                    is ScreenDestination.Stories -> StoriesBrowseScreen(viewModel = viewModel)
                                    is ScreenDestination.Reviews -> ReaderReviewsScreen(viewModel = viewModel)
                                    is ScreenDestination.StoryDetail -> StoryDetailScreen(
                                        storyId = destination.storyId,
                                        viewModel = viewModel
                                    )
                                    is ScreenDestination.Reading -> ReadingScreen(
                                        storyId = destination.storyId,
                                        partId = destination.partId,
                                        highlightPassage = destination.highlightPassage,
                                        highlightAnchor = destination.highlightAnchor,
                                        viewModel = viewModel
                                    )
                                    is ScreenDestination.WriterDashboard -> WriterDashboardScreen(viewModel = viewModel)
                                    is ScreenDestination.ReviewsManagement -> ReviewsManagementScreen(viewModel = viewModel)
                                    is ScreenDestination.Notifications -> NotificationsScreen(viewModel = viewModel)
                                    is ScreenDestination.Settings -> SettingsScreen(viewModel = viewModel)
                                    is ScreenDestination.Login -> LoginScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }

                    if (showMoreSheet) {
                        MoreBottomSheet(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onDismiss = { showMoreSheet = false },
                            onNavigate = { destination ->
                                viewModel.navigateTo(destination)
                            }
                        )
                    }
                }
            }
        }
    }
}
