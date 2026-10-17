# AI Smart Video Cutter

A native, production-grade Android application that analyzes videos (up to 5 minutes default), automatically detects scene candidates, accepts natural language editing instructions via AI chat, and executes verified, local, lossless video cuts with zero mock data.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed through Phase 1 requirements:

- **Confirmed Decision 1 (Provider Routing)**: OpenRouter is configured as the primary AI provider with automatic, quota-aware fallback to the Gemini API (targeting `models/gemini-flash-latest` and supporting `gemini-4-argon` high thinking where applicable).
- **Confirmed Decision 2 (Cutting Engine)**: Fast, lossless stream-copy trimming (via native Android `MediaExtractor` + `MediaMuxer`) is the default export mode, with a user toggle for frame-accurate re-encoding when exact micro-boundary accuracy is demanded.
- **Confirmed Decision 3 (Bilingual UI Style)**: Bilingual technical labels combining English and Hindi (e.g., "Export Video / वीडियो निर्यात", "Scene Timeline / दृश्य समयरेखा", "AI Analysis / एआई विश्लेषण") to provide intuitive comprehension for multilingual creators.
- **Confirmed Decision 4 (Zero-Leak Security & Consent)**: API keys are securely entered in an in-app settings screen, stored encrypted in Android Keystore / EncryptedSharedPreferences, redacted from all logs and chat prompts, and videos are never uploaded to the cloud without explicit per-job user consent.

---

## 1. Overview & Core Concept

- **What It Does**: The app allows users to import any video from device storage via Android's Photo Picker / Storage Access Framework (SAF), inspect its technical metadata (resolution, frame rate, audio streams, codecs), perform a low-memory local scene-change scan, discuss clip selections via natural language in an AI chat panel, fine-tune cut boundaries on an interactive visual timeline, and export validated, playable video clips directly to local storage.
- **Target Audience / Persona**: Content creators, vloggers, educators, and social media managers who want to extract highlights, comedy bits, or topic sections from raw footage without manual timeline scrubbing on desktop software.
- **Key Value**: Bridges high-level creative intent ("Find the most interesting 30 seconds" or "Cut from the moment the speaker mentions AI") with guaranteed local processing and verifiable playback. Works 100% offline in manual mode even if no API key is provided or when cloud quotas are exceeded.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Import & Preflight**:
   - User launches the app to the **Home Screen**.
   - Tapping "Import Video / वीडियो चुनें" launches the system picker.
   - Preflight inspection reads duration, codecs, dimensions, rotation, and checks available device storage.
   - If duration exceeds 5 minutes, a friendly warning is presented with an option to analyze the first 5 minutes or adjust the duration cap in Settings.

2. **Analysis & Scene Discovery**:
   - The app runs a lightweight background pass using `MediaMetadataRetriever` to sample frames at regular intervals and calculate visual histogram variance.
   - Scene candidate markers appear dynamically along the visual timeline with thumbnails.
   - If an AI provider (OpenRouter or Gemini) is active and user consent is granted, keyframe thumbnails and timestamps are sent to the AI model to generate structured scene titles, confidence ratings, and rationale.

3. **AI Chat & Natural Language Edit Plan**:
   - User switches to or slides up the **AI Chat Sheet**.
   - User types instructions (e.g., "Cut the segment where the demo begins", "Shorten this clip by 5 seconds", "Separate all distinct scenes").
   - AI generates a structured `EditPlan` containing clip boundaries, suggested title, confidence score, and explanation.
   - Ambiguous queries trigger clarifying questions before any cutting starts.

4. **Timeline Trimming & Boundary Refinement**:
   - The interactive dual-thumb range slider lets users scrub through the video and preview exact start and end frames with millisecond precision.
   - Scene candidate pills allow 1-tap snap-to-scene.

5. **Cutting & Verified Export**:
   - User taps "Export Clip / क्लिप काटें".
   - The background worker executes stream-copy extraction via `MediaExtractor` + `MediaMuxer` to an app-private cache before copying to the public Movies/Video directory.
   - Export verification verifies file size (>0 bytes), readable duration, and media playability.
   - A success dialog displays playback preview, output statistics, and a Share action.

6. **Settings & Job History**:
   - Dedicated screens for managing OpenRouter and Gemini API keys with validation tests, setting provider priority, toggling frame-accurate re-encode, and viewing past jobs with recovery/retry capabilities.

### Visual Identity & Theme

- **Aesthetic Direction**: High-tech cinematic dark theme ("Obsidian & Cyan Neon") designed for creators working in dim studio environments.
- **Color Palette**:
  - Background: Deep Obsidian `#0D1117` and Charcoal `#161B22`
  - Primary Accent: Electric Cyan `#00F0FF` for playback cursors, active markers, and primary buttons
  - Secondary Accent: Neon Purple `#A855F7` for AI insights and thinking indicators
  - Success/Status: Emerald Green `#10B981` for verified exports
  - Warning/Error: Amber `#F59E0B` and Crimson `#EF4444`
  - Surfaces: Elevated cards with subtle 1dp border highlights (`#30363D`) and soft gradient glows
- **Typography & Hierarchy**:
  - Modern sans-serif typography with distinct weights: bold tracking for technical metrics (00:01:23.450), medium weights for bilingual labels, and readable body text for AI explanations.
- **Component Styling & Layout**:
  - Full-bleed edge-to-edge support with safe area insets.
  - Ergonomic bottom navigation bar with active cyan pill indicators.
  - Video preview container that maintains aspect ratio without letterbox distortions.
  - Minimum touch targets of 48dp on all interactive scrubbers and buttons.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Native Android Media Engine vs. Embedded FFmpeg/Python
- **Chosen Approach**: Native Android `android.media` (`MediaExtractor`, `MediaMuxer`, `MediaMetadataRetriever`, and `MediaPlayer`).
- **Why**: Zero native library crashes, 100% compatibility across all Android architectures (x86_64 emulator and ARM64 physical devices), zero MB APK bloat, instantaneous lossless stream extraction, and zero external licensing conflicts.
- **Trade-Off**: Stream-copy trimming snaps cuts to the nearest preceding I-frame (keyframe). To satisfy users demanding exact frame cuts, a secondary frame-accurate re-encoding mode is provided via `MediaCodec`/Canvas rendering.

### Decision 2: Dual AI Provider Strategy (OpenRouter + Gemini)
- **Chosen Approach**: Unified `AiOrchestrator` interface with `OpenRouterClient` (primary) and `GeminiClient` (fallback), using strict JSON schema responses.
- **Why**: Protects users against rate limits (HTTP 429), regional outages, or model deprecations. If OpenRouter returns quota limits or network errors, the orchestrator automatically retries with Gemini. If both fail or no key exists, local scene scan continues seamlessly.
- **Alternatives Considered**: Direct single SDK integration was rejected because it creates single-point-of-failure dependencies.

### Decision 3: Local Frame Sampling vs. Full Video Upload
- **Chosen Approach**: Local downsampling of candidate keyframes (e.g. 1-2 frames per 5-10 seconds or detected visual peaks), converted to lightweight compressed JPEG representations for API multimodal analysis.
- **Why**: Uploading raw 500MB MP4 files over mobile cellular data is slow, expensive, and fails on rate-limited free tiers. Sampling reduces payload size by 98% while preserving 100% of the visual context needed for clip boundary decisions.

### Decision 4: Durable State Persistence via Room
- **Chosen Approach**: Room Database tracking every job state (`QUEUED`, `SCANNING`, `ANALYZING`, `AWAITING_CONFIRMATION`, `CUTTING`, `VERIFYING`, `COMPLETED`, `FAILED`, `CANCELLED`).
- **Why**: Guarantees crash recovery. If the OS kills the process on a 4 GB RAM device during analysis, the job and user-entered boundaries are safely restored upon reopening.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (Jetpack Compose)            │
│  ┌───────────────┐  ┌───────────────┐  ┌──────────────┐  ┌──────────┐  │
│  │  HomeScreen   │  │ TimelineScreen│  │  AiChatSheet │  │SettingsUI│  │
│  └───────┬───────┘  └───────┬───────┘  └──────┬───────┘  └────┬─────┘  │
│          └──────────────────┼─────────────────┴───────────────┘        │
│                             ▼                                          │
│                    MainViewModel & JobViewModel                        │
│                 (StateFlow<JobUiState>, Coroutines)                    │
└─────────────────────────────┬──────────────────────────────────────────┘
                              │
┌─────────────────────────────┼──────────────────────────────────────────┐
│                             ▼                                          │
│                        DOMAIN LAYER                                    │
│  ┌───────────────────────┐  ┌─────────────────┐  ┌──────────────────┐  │
│  │   ScanScenesUseCase   │  │ CutVideoUseCase │  │ AnalyzeAiUseCase │  │
│  └───────────┬───────────┘  └────────┬────────┘  └────────┬─────────┘  │
└──────────────┼───────────────────────┼────────────────────┼────────────┘
               │                       │                    │
┌──────────────┴───────────────────────┴────────────────────┴────────────┐
│                        DATA & INFRASTRUCTURE LAYER                     │
│                                                                        │
│  ┌────────────────────────┐  ┌──────────────────────────────────────┐  │
│  │   AndroidMediaEngine   │  │          AiOrchestrator              │  │
│  │  - MediaMetadataRetr.  │  │   ┌───────────────────────────────┐  │  │
│  │  - MediaExtractor      │  │   │ OpenRouter Provider (Primary) │  │  │
│  │  - MediaMuxer          │  │   └───────────────┬───────────────┘  │  │
│  │  - OutputVerifier      │  │                   ▼ (Fallback on 429)│  │
│  └───────────┬────────────┘  │   ┌───────────────────────────────┐  │  │
│              │               │   │ Gemini API Provider (Fallback)│  │  │
│              │               │   └───────────────────────────────┘  │  │
│              │               └───────────────────┬──────────────────┘  │
│              ▼                                   ▼                     │
│  ┌────────────────────────┐  ┌──────────────────────────────────────┐  │
│  │  AppDatabase (Room)    │  │    EncryptedKeyStorage (Keystore)    │  │
│  │  - JobEntity & Clips   │  │    - Safe secret encryption/redaction│  │
│  └────────────────────────┘  └──────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **`VideoMetadata`**:
  - `uri: String`, `filename: String`, `durationMs: Long`, `width: Int`, `height: Int`, `rotation: Int`, `bitrate: Long`, `mimeType: String`, `sizeBytes: Long`, `hasAudio: Boolean`.
- **`SceneCandidate`**:
  - `id: String`, `startMs: Long`, `endMs: Long`, `thumbnailPath: String?`, `confidence: Float`, `label: String`, `reason: String`.
- **`EditPlan`**:
  - `planId: String`, `taskDescription: String`, `suggestedClips: List<ClipProposal>`, `requiresClarification: Boolean`, `clarificationPrompt: String?`.
- **`ClipProposal`**:
  - `id: String`, `title: String`, `startMs: Long`, `endMs: Long`, `rationale: String`, `confidence: Float`.
- **`CutJobEntity` (Room)**:
  - `id: Long (PK)`, `sourceUri: String`, `sourceDurationMs: Long`, `clipStartMs: Long`, `clipEndMs: Long`, `outputUri: String?`, `outputSizeBytes: Long`, `status: JobStatus`, `createdAt: Long`, `completedAt: Long?`, `errorMessage: String?`.
- **`JobStatus`**:
  - `IDLE`, `VALIDATING`, `SCANNING`, `ANALYZING`, `AWAITING_CONFIRMATION`, `CUTTING`, `VERIFYING`, `COMPLETED`, `FAILED`, `CANCELLED`.

### Interactive Component & State Mapping

- **Video Import Card**: Launches `PickVisualMedia` contract. Validates URI via `ContentResolver` and reads stream tracks. Emits `JobUiState.MetadataLoaded`.
- **Timeline Scrubber**: Real-time two-way binding with `currentPositionMs`, `selectionStartMs`, and `selectionEndMs`. Supports drag gestures, snap-to-scene markers, and pinch-to-zoom granularity.
- **AiChatSheet**: Displays conversation history with state tags. Sends user query + selected candidate keyframes to `AiOrchestrator`. Parses JSON output into actionable `EditPlan` cards that feature a "Load into Timeline" button.
- **Export Action**: Displays non-blocking progress modal with cancel button. Triggers `AndroidMediaEngine.cutVideo()`, then calls `OutputVerifier` to validate stream headers and playback duration.

### Quota-Aware Fallback Flow

```
User AI Prompt
     │
     ▼
Check OpenRouter Key & Model Registry
     │
     ├─► [Key Present & Valid] ──► Call OpenRouter API
     │                                    │
     │                      ┌─────────────┴─────────────┐
     │                      ▼                           ▼
     │                 Success (200)             Rate Limit (429) / Error
     │                      │                           │
     │                      │                           ▼
     │                      │                 Check Gemini API Key
     │                      │                           │
     │                      │                 ┌─────────┴─────────┐
     │                      │                 ▼                   ▼
     │                      │            Key Present          No Key / Failed
     │                      │                 │                   │
     │                      │                 ▼                   ▼
     │                      │            Call Gemini       Local Scene Scan
     │                      │                 │            (Mark partial/manual)
     │                      │                 ▼                   │
     │                      └──────────► Parse JSON               │
     │                                        │                   │
     └─► [No Key Configured] ─────────────────┴───────────────────┘
```
