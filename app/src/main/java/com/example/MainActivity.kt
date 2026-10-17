package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BilingualHeader
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JobHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.NeonCyanPrimary
import com.example.ui.theme.SmartVideoCutterTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartVideoCutterTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val jobs by viewModel.allJobs.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        BilingualHeader(
                            titleEn = when (uiState.activeTab) {
                                0 -> "Home / Import"
                                1 -> "Timeline Trimmer"
                                2 -> "AI Chat Assistant"
                                3 -> "Export History"
                                4 -> "Settings"
                                else -> "About"
                            },
                            titleHi = when (uiState.activeTab) {
                                0 -> "होम"
                                1 -> "समयरेखा"
                                2 -> "एआई चैट"
                                3 -> "इतिहास"
                                4 -> "सेटिंग्स"
                                else -> "जानकारी"
                            },
                            subtitle = uiState.videoMetadata?.fileName ?: "No video selected"
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = CharcoalSurface,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = uiState.activeTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyanPrimary,
                                    selectedTextColor = NeonCyanPrimary,
                                    indicatorColor = CharcoalSurface,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_home")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = { Icon(Icons.Default.Timeline, contentDescription = "Timeline") },
                                label = { Text("Timeline") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyanPrimary,
                                    selectedTextColor = NeonCyanPrimary,
                                    indicatorColor = CharcoalSurface,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_timeline")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Chat") },
                                label = { Text("AI Chat") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyanPrimary,
                                    selectedTextColor = NeonCyanPrimary,
                                    indicatorColor = CharcoalSurface,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_ai_chat")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == 3,
                                onClick = { viewModel.selectTab(3) },
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("History") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyanPrimary,
                                    selectedTextColor = NeonCyanPrimary,
                                    indicatorColor = CharcoalSurface,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_history")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == 4,
                                onClick = { viewModel.selectTab(4) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyanPrimary,
                                    selectedTextColor = NeonCyanPrimary,
                                    indicatorColor = CharcoalSurface,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_settings")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (uiState.activeTab) {
                            0 -> HomeScreen(
                                metadata = uiState.videoMetadata,
                                recentJobs = jobs,
                                onVideoImported = { uri ->
                                    viewModel.importVideo(this@MainActivity, uri)
                                },
                                onNavigateToTimeline = { viewModel.selectTab(1) },
                                onNavigateToChat = { viewModel.selectTab(2) }
                            )

                            1 -> TimelineScreen(
                                metadata = uiState.videoMetadata,
                                selectionStartMs = uiState.selectionStartMs,
                                selectionEndMs = uiState.selectionEndMs,
                                currentPlaybackMs = uiState.currentPlaybackMs,
                                isPlaying = uiState.isPlaying,
                                scenes = uiState.sceneCandidates,
                                isExporting = uiState.isExporting,
                                exportProgress = uiState.exportProgress,
                                exportStageMessage = uiState.exportStageMessage,
                                exportSuccessUri = uiState.exportSuccessUri,
                                exportErrorMessage = uiState.exportErrorMessage,
                                showExportDialog = uiState.showExportDialog,
                                onTogglePlay = { viewModel.togglePlayback() },
                                onSeekTo = { ms -> viewModel.updatePlaybackPosition(ms) },
                                onSelectionChange = { start, end ->
                                    viewModel.updateSelection(start, end)
                                },
                                onExportClip = { viewModel.exportCurrentClip(this@MainActivity) },
                                onDismissStatus = { viewModel.dismissExportDialog() }
                            )

                            2 -> AiChatScreen(
                                messages = uiState.chatMessages,
                                isAiThinking = uiState.isAiThinking,
                                onSendMessage = { prompt -> viewModel.sendAiChatPrompt(prompt) },
                                onApplyProposal = { proposal -> viewModel.applyProposalToTimeline(proposal) }
                            )

                            3 -> JobHistoryScreen(
                                jobs = jobs,
                                onDeleteJob = { jobId ->
                                    // Managed via JobRepository
                                }
                            )

                            4 -> SettingsScreen(
                                keyStorage = viewModel.keyStorage,
                                onSaveSettings = { openRouterKey, geminiKey, priority, frameAccurate, maxDuration, consent ->
                                    viewModel.saveSettings(
                                        openRouterKey, geminiKey, priority, frameAccurate, maxDuration, consent
                                    )
                                }
                            )

                            else -> AboutScreen()
                        }
                    }
                }
            }
        }
    }
}
