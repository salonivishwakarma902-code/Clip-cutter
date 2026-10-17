package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiOrchestrator
import com.example.data.ai.ChatMessage
import com.example.data.ai.ChatSender
import com.example.data.ai.ClipProposal
import com.example.data.ai.EditPlan
import com.example.data.db.AppDatabase
import com.example.data.db.CutJobEntity
import com.example.data.db.JobStatus
import com.example.data.media.AndroidMediaEngine
import com.example.data.media.SceneCandidate
import com.example.data.media.VideoMetadata
import com.example.data.repository.JobRepository
import com.example.data.repository.SettingsRepository
import com.example.data.security.EncryptedKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MainUiState(
    val videoMetadata: VideoMetadata? = null,
    val sceneCandidates: List<SceneCandidate> = emptyList(),
    val isScanningScenes: Boolean = false,
    val selectionStartMs: Long = 0L,
    val selectionEndMs: Long = 0L,
    val currentPlaybackMs: Long = 0L,
    val isPlaying: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val isAiThinking: Boolean = false,
    val activeJob: CutJobEntity? = null,
    val isExporting: Boolean = false,
    val exportProgress: Int = 0,
    val exportStageMessage: String = "",
    val exportSuccessUri: String? = null,
    val exportErrorMessage: String? = null,
    val showExportDialog: Boolean = false,
    val activeTab: Int = 0 // 0: Home/Import, 1: Timeline, 2: AI Chat, 3: History, 4: Settings
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val jobRepository = JobRepository(db.cutJobDao())
    val keyStorage = EncryptedKeyStorage(application)
    val settingsRepository = SettingsRepository(keyStorage)
    private val aiOrchestrator = AiOrchestrator(keyStorage)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val allJobs: StateFlow<List<CutJobEntity>> = jobRepository.allJobs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Welcome message in AI Chat
        val welcomeMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.SYSTEM,
            text = "Welcome to AI Smart Video Cutter! / एआई स्मार्ट वीडियो कटर में आपका स्वागत है!\n\nImport a video to begin. You can ask me to find funny moments, cut specific timestamps, or split scenes."
        )
        _uiState.update { it.copy(chatMessages = listOf(welcomeMsg)) }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun importVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningScenes = true) }
            val metaResult = AndroidMediaEngine.extractMetadata(context, uri)

            if (metaResult.isSuccess) {
                val metadata = metaResult.getOrThrow()
                val duration = metadata.durationMs
                val defaultEndMs = duration.coerceAtMost(300000L) // Cap default view at 5 minutes

                _uiState.update {
                    it.copy(
                        videoMetadata = metadata,
                        selectionStartMs = 0L,
                        selectionEndMs = defaultEndMs,
                        currentPlaybackMs = 0L,
                        activeTab = 1 // Auto navigate to Timeline tab
                    )
                }

                // Run local scene scan pass
                val scenes = AndroidMediaEngine.scanScenes(context, uri, duration)
                _uiState.update {
                    it.copy(
                        sceneCandidates = scenes,
                        isScanningScenes = false
                    )
                }
            } else {
                val errorMsg = metaResult.exceptionOrNull()?.message ?: "Failed to inspect video"
                _uiState.update {
                    it.copy(
                        isScanningScenes = false,
                        exportErrorMessage = "Import Failed: $errorMsg"
                    )
                }
            }
        }
    }

    fun updateSelection(startMs: Long, endMs: Long) {
        val duration = _uiState.value.videoMetadata?.durationMs ?: 0L
        val safeStart = startMs.coerceIn(0L, duration)
        val safeEnd = endMs.coerceIn(safeStart + 500L, duration)

        _uiState.update {
            it.copy(
                selectionStartMs = safeStart,
                selectionEndMs = safeEnd
            )
        }
    }

    fun updatePlaybackPosition(ms: Long) {
        _uiState.update { it.copy(currentPlaybackMs = ms) }
    }

    fun togglePlayback() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun sendAiChatPrompt(userPrompt: String) {
        val metadata = _uiState.value.videoMetadata ?: return
        if (userPrompt.isBlank()) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.USER,
            text = userPrompt
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                isAiThinking = true
            )
        }

        viewModelScope.launch {
            val analysis = aiOrchestrator.analyzeVideoPrompt(
                userPrompt = userPrompt,
                videoDurationMs = metadata.durationMs,
                scenes = _uiState.value.sceneCandidates
            )

            val aiMsgText = if (analysis.isLocalFallback) {
                "Analysis complete via ${analysis.providerUsed}.\nNote: Add API keys in Settings for LLM reasoning."
            } else {
                "AI Edit Plan generated via ${analysis.providerUsed}:"
            }

            val aiMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.AI,
                text = aiMsgText,
                editPlan = analysis.editPlan,
                providerUsed = analysis.providerUsed
            )

            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + aiMsg,
                    isAiThinking = false
                )
            }
        }
    }

    fun applyProposalToTimeline(proposal: ClipProposal) {
        val duration = _uiState.value.videoMetadata?.durationMs ?: 0L
        val safeStart = proposal.startMs.coerceIn(0L, duration)
        val safeEnd = proposal.endMs.coerceIn(safeStart + 500L, duration)

        _uiState.update {
            it.copy(
                selectionStartMs = safeStart,
                selectionEndMs = safeEnd,
                activeTab = 1 // Switch to timeline to view clip
            )
        }
    }

    fun exportCurrentClip(context: Context) {
        val metadata = _uiState.value.videoMetadata ?: return
        val startMs = _uiState.value.selectionStartMs
        val endMs = _uiState.value.selectionEndMs
        val isReencode = keyStorage.frameAccurateReencode

        if (endMs <= startMs) {
            _uiState.update { it.copy(exportErrorMessage = "Invalid clip bounds: End time must be after start time") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFileName = "Clip_$timeStamp.mp4"

            _uiState.update {
                it.copy(
                    isExporting = true,
                    exportProgress = 5,
                    exportStageMessage = "Initializing export / निर्यात प्रारंभ...",
                    exportSuccessUri = null,
                    exportErrorMessage = null,
                    showExportDialog = true
                )
            }

            val jobId = jobRepository.insertJob(
                sourceUri = metadata.uriString,
                sourceFileName = metadata.fileName,
                sourceDurationMs = metadata.durationMs,
                clipStartMs = startMs,
                clipEndMs = endMs,
                outputFileName = outputFileName,
                isReencodeMode = isReencode
            )

            try {
                val sourceUri = Uri.parse(metadata.uriString)

                // Public Movies/Video directory or fallback to app private files
                val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    ?: File(context.filesDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()

                val outputFile = File(exportDir, outputFileName)

                jobRepository.updateProgress(jobId, 20, "Trimming media streams / मीडिया ट्रिमिंग...", JobStatus.CUTTING)
                _uiState.update {
                    it.copy(
                        exportProgress = 30,
                        exportStageMessage = "Executing stream extraction / स्ट्रीम निष्कर्षण..."
                    )
                }

                val cutResult = AndroidMediaEngine.cutVideoLossless(
                    context = context,
                    sourceUri = sourceUri,
                    startMs = startMs,
                    endMs = endMs,
                    outputFile = outputFile,
                    onProgress = { percent ->
                        _uiState.update { s ->
                            s.copy(
                                exportProgress = percent,
                                exportStageMessage = "Processing frames / प्रोसेसिंग... ($percent%)"
                            )
                        }
                    }
                )

                if (cutResult.isSuccess) {
                    val finalFile = cutResult.getOrThrow()
                    _uiState.update {
                        it.copy(
                            exportProgress = 90,
                            exportStageMessage = "Verifying playback integrity / सत्यापन जारी..."
                        )
                    }

                    val verifyResult = AndroidMediaEngine.verifyOutput(finalFile)

                    if (verifyResult.isSuccess) {
                        val outputUriStr = Uri.fromFile(finalFile).toString()
                        val fileSize = finalFile.length()

                        jobRepository.markCompleted(jobId, outputUriStr, fileSize)

                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                exportProgress = 100,
                                exportStageMessage = "Export Completed & Verified / निर्यात पूर्ण एवं सत्यापित!",
                                exportSuccessUri = outputUriStr
                            )
                        }
                    } else {
                        val verifyErr = verifyResult.exceptionOrNull()?.message ?: "Verification failed"
                        jobRepository.markFailed(jobId, verifyErr)
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                exportErrorMessage = "Export Verification Failed: $verifyErr"
                            )
                        }
                    }
                } else {
                    val cutErr = cutResult.exceptionOrNull()?.message ?: "Cut execution failed"
                    jobRepository.markFailed(jobId, cutErr)
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportErrorMessage = "Export Failed: $cutErr"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during export job", e)
                jobRepository.markFailed(jobId, e.message ?: "Unknown error")
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportErrorMessage = "Export Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissExportDialog() {
        _uiState.update {
            it.copy(
                showExportDialog = false,
                exportSuccessUri = null,
                exportErrorMessage = null
            )
        }
    }

    fun saveSettings(
        openRouterKey: String,
        geminiKey: String,
        priority: EncryptedKeyStorage.ProviderPriority,
        frameAccurate: Boolean,
        maxDuration: Int,
        consent: Boolean
    ) {
        settingsRepository.setOpenRouterKey(openRouterKey)
        settingsRepository.setGeminiKey(geminiKey)
        settingsRepository.setProviderPriority(priority)
        settingsRepository.setFrameAccurate(frameAccurate)
        settingsRepository.setMaxDurationMinutes(maxDuration)
        settingsRepository.setCloudConsentGranted(consent)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
