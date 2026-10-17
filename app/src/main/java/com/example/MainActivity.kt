package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.AiStudioScreen
import com.example.ui.theme.SmartVideoCutterTheme
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AiStudioScreen(
                            metadata = uiState.videoMetadata,
                            chatMessages = uiState.chatMessages,
                            recentJobs = jobs,
                            isAiThinking = uiState.isAiThinking,
                            isExporting = uiState.isExporting,
                            exportProgress = uiState.exportProgress,
                            exportStageMessage = uiState.exportStageMessage,
                            onImportVideo = { uri -> viewModel.importVideo(this@MainActivity, uri) },
                            onSendPrompt = { prompt -> viewModel.sendAiChatPrompt(this@MainActivity, prompt) },
                            onOpenSettings = { viewModel.toggleSettingsModal(true) },
                            onExportProposal = { proposal -> viewModel.exportProposalClip(this@MainActivity, proposal) }
                        )

                        if (uiState.showSettingsModal) {
                            SettingsDialog(
                                keyStorage = viewModel.keyStorage,
                                onDismiss = { viewModel.toggleSettingsModal(false) },
                                onSaveSettings = { openRouterKey, geminiKey, priority, frameAccurate, maxDuration, consent ->
                                    viewModel.saveSettings(
                                        openRouterKey, geminiKey, priority, frameAccurate, maxDuration, consent
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
