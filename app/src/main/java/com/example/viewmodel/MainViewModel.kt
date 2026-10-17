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
    val showSettingsModal: Boolean = false
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
        // Welcome message in AI Voice & Chat Assistant
        val welcomeMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.SYSTEM,
            text = "Welcome to AI Voice & Smart Video Cutter! / एआई स्मार्ट वीडियो कटर में आपका स्वागत है!\n\nImport a video and speak or type your request (e.g., \"Cut all funny moments\", \"Extract 30s summary\", \"Remove silent parts\"). AI will cut all clips automatically!"
        )
        _uiState.update { it.copy(chatMessages = listOf(welcomeMsg)) }
    }

    fun toggleSettingsModal(show: Boolean) {
        _uiState.update { it.copy(showSettingsModal = show) }
    }

    fun importVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningScenes = true) }
            val metaResult = AndroidMediaEngine.extractMetadata(context, uri)

            if (metaResult.isSuccess) {
                val metadata = metaResult.getOrThrow()
                val duration = metadata.durationMs
                val defaultEndMs = duration.coerceAtMost(300000L)

                _uiState.update {
                    it.copy(
                        videoMetadata = metadata,
                        selectionStartMs = 0L,
                        selectionEndMs = defaultEndMs,
                        currentPlaybackMs = 0L
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

    fun sendAiChatPrompt(context: Context, userPrompt: String) {
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
                "AI Edit Plan prepared via ${analysis.providerUsed}.\n(Add OpenRouter/Gemini key in Settings for deeper LLM reasoning):"
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

            // Automatically trigger batch cutting if proposed clips exist
            if (analysis.editPlan.suggestedClips.isNotEmpty()) {
                exportAllProposalsInBatch(context, analysis.editPlan.suggestedClips)
            }
        }
    }

    fun exportProposalClip(context: Context, proposal: ClipProposal) {
        exportClipInternal(context, proposal.startMs, proposal.endMs, proposal.title)
    }

    fun exportAllProposalsInBatch(context: Context, proposals: List<ClipProposal>) {
        if (proposals.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            proposals.forEachIndexed { idx, clip ->
                val title = clip.title.ifBlank { "Clip_${idx + 1}" }
                exportClipInternalSync(context, clip.startMs, clip.endMs, title, idx + 1, proposals.size)
            }
        }
    }

    private fun exportClipInternal(context: Context, startMs: Long, endMs: Long, clipTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            exportClipInternalSync(context, startMs, endMs, clipTitle, 1, 1)
        }
    }

    private fun exportClipInternalSync(
        context: Context,
        startMs: Long,
        endMs: Long,
        clipTitle: String,
        currentBatchIndex: Int,
        totalBatchCount: Int
    ) {
        val metadata = _uiState.value.videoMetadata ?: return
        val isReencode = keyStorage.frameAccurateReencode

        if (endMs <= startMs) return

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanTitle = clipTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val outputFileName = "${cleanTitle}_$timeStamp.mp4"

        _uiState.update {
            it.copy(
                isExporting = true,
                exportProgress = 10,
                exportStageMessage = "Processing clip $currentBatchIndex of $totalBatchCount ($clipTitle)...",
                exportSuccessUri = null,
                exportErrorMessage = null,
                showExportDialog = true
            )
        }

        val jobId = kotlinx.coroutines.runBlocking {
            jobRepository.insertJob(
                sourceUri = metadata.uriString,
                sourceFileName = metadata.fileName,
                sourceDurationMs = metadata.durationMs,
                clipStartMs = startMs,
                clipEndMs = endMs,
                outputFileName = outputFileName,
                isReencodeMode = isReencode
            )
        }

        try {
            val sourceUri = Uri.parse(metadata.uriString)
            val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val outputFile = File(exportDir, outputFileName)

            val cutResult = kotlinx.coroutines.runBlocking {
                AndroidMediaEngine.cutVideoLossless(
                    context = context,
                    sourceUri = sourceUri,
                    startMs = startMs,
                    endMs = endMs,
                    outputFile = outputFile,
                    onProgress = { percent ->
                        _uiState.update { s ->
                            s.copy(
                                exportProgress = percent,
                                exportStageMessage = "Trimming $clipTitle ($percent%)..."
                            )
                        }
                    }
                )
            }

            if (cutResult.isSuccess) {
                val finalFile = cutResult.getOrThrow()
                val verifyResult = kotlinx.coroutines.runBlocking {
                    AndroidMediaEngine.verifyOutput(finalFile)
                }

                if (verifyResult.isSuccess) {
                    val outputUriStr = Uri.fromFile(finalFile).toString()
                    val fileSize = finalFile.length()

                    kotlinx.coroutines.runBlocking {
                        jobRepository.markCompleted(jobId, outputUriStr, fileSize)
                    }

                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportProgress = 100,
                            exportStageMessage = "Successfully exported $totalBatchCount clip(s)!",
                            exportSuccessUri = outputUriStr
                        )
                    }
                } else {
                    val verifyErr = verifyResult.exceptionOrNull()?.message ?: "Verification failed"
                    kotlinx.coroutines.runBlocking {
                        jobRepository.markFailed(jobId, verifyErr)
                    }
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportErrorMessage = "Verification Failed: $verifyErr"
                        )
                    }
                }
            } else {
                val cutErr = cutResult.exceptionOrNull()?.message ?: "Cut execution failed"
                kotlinx.coroutines.runBlocking {
                    jobRepository.markFailed(jobId, cutErr)
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportErrorMessage = "Export Failed: $cutErr"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during export job", e)
            kotlinx.coroutines.runBlocking {
                jobRepository.markFailed(jobId, e.message ?: "Unknown error")
            }
            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportErrorMessage = "Export Error: ${e.message}"
                )
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

    fun deleteJob(jobId: Long) {
        viewModelScope.launch {
            jobRepository.deleteJob(jobId)
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
        toggleSettingsModal(false)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
