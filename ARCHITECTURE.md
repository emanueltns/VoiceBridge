# VoiceBridge Architecture

## System Overview

VoiceBridge is a hands-free voice assistant Android app. It uses on-device models for speech recognition and text-to-speech, and connects to a remote VPS running Claude for conversational AI.

```
+------------------+     +------------------+     +------------------+
|   MICROPHONE     | --> | STREAMING ASR    | --> |   VPS (Claude)   |
|   (AudioRecord)  |     | (Zipformer)      |     |   via TCP        |
|   16kHz mono     |     | Real-time decode |     |   host:port      |
+------------------+     +------------------+     +------------------+
                                                         |
+------------------+     +------------------+             |
|   SPEAKER        | <-- | TTS (Kokoro)     | <-----------+
|   (AudioTrack)   |     | On-device synth  |
+------------------+     +------------------+
```

---

## 1. Main Pipeline State Machine

This is the core loop running in `AudioPipelineManager` inside a foreground service.

```
                            User taps "Start Call"
                                    |
                                    v
                          +------------------+
                          |   INITIALIZING   |
                          |  Load ASR + TTS  |
                          |  models into RAM |
                          +------------------+
                                    |
                                    | Models loaded
                                    v
          +=======================================+
          ||          LISTENING (green)          ||
          ||  Mic open, streaming audio to ASR   ||
          ||  Partial text appears in real-time  ||
          +=======================================+
               |                          |
               | Partial text detected    | User taps "End Call"
               v                          v
     +------------------+          +------------------+
     |  TRANSCRIBING    |          |      IDLE        |
     |  (blue)          |          |  Service stopped |
     |  Words appearing |          +------------------+
     |  as user speaks  |
     +------------------+
               |
               | Endpoint detected (1.0s silence after speech)
               v
     +------------------+
     |    SENDING       |
     |    (yellow)      |  ---- TCP ----> VPS (Claude)
     |  "Asking Claude" |
     +------------------+
          |          |
          |          | If VPS takes > 3 seconds
          |          v
          |   +------------------+
          |   |  ENTERTAINING    |
          |   |  (purple)        |
          |   |  TTS speaks fun  |
          |   |  facts in a loop |
          |   +------------------+
          |          |
          |          | VPS response arrives
          |          | Wait for current fact to finish
          |          | "Alright, I have the response now."
          |          v
          +--------->+
                     |
                     v
          +------------------+
          |   SPEAKING       |
          |   (cyan)         |
          |   TTS reads      |
          |   Claude's reply |
          +------------------+
                     |
                     | TTS finished
                     v
          +=======================================+
          ||          LISTENING (green)          ||
          ||       (loop continues)              ||
          +=======================================+
```

### State Transitions Table

| From | To | Trigger | Audio Cue |
|---|---|---|---|
| IDLE | INITIALIZING | User taps Start | - |
| INITIALIZING | LISTENING | Models loaded | Beep |
| LISTENING | TRANSCRIBING | Partial ASR text detected | - |
| TRANSCRIBING | SENDING | Endpoint detected (silence) | Ack tone |
| TRANSCRIBING | LISTENING | Empty transcription | Beep |
| SENDING | ENTERTAINING | VPS response > 3s | - |
| SENDING | SPEAKING | VPS response received | Ack tone |
| ENTERTAINING | SPEAKING | VPS response + current fact finishes | Ack tone |
| SPEAKING | LISTENING | TTS finished | Beep |
| Any | IDLE | User taps Stop | - |

---

## 2. VPS Connection State Machine

Managed by `VpsConnectionManager`. Each message creates a new TCP socket.

```
     +------------------+
     |  DISCONNECTED    | <-------- User taps Stop
     +------------------+           or app closed
            |
            | User taps Start
            v
     +------------------+
     |   CONNECTED      | <---+
     |  (host:port set) |     |
     +------------------+     |
            |                 |
            | sendMessage()   | Success (retryCount = 0)
            v                 |
     +------------------+     |
     |   CONNECTING     |-----+
     |  (TCP in flight) |
     +------------------+
            |
            | TCP failure (IOException, timeout)
            v
     +------------------+
     |     ERROR        |
     |  "Retrying in    |
     |   Xs..."         |
     +------------------+
            |
            | Exponential backoff: 1s, 2s, 4s, 8s... 60s max
            v
     +------------------+
     |   CONNECTED      | (ready for next attempt)
     +------------------+
```

### Retry Schedule

| Attempt | Delay | Total wait |
|---|---|---|
| 1 | 1s | 1s |
| 2 | 2s | 3s |
| 3 | 4s | 7s |
| 4 | 8s | 15s |
| 5 | 16s | 31s |
| 6 | 32s | 63s |
| 7+ | 60s (cap) | +60s each |

---

## 3. Audio Data Flow

```
+------------------------------------------------------------------+
|                    FOREGROUND SERVICE                              |
|                                                                   |
|  +-------------+    +------------------+    +------------------+  |
|  | AudioRecord |    | OnlineRecognizer |    |    VpsClient     |  |
|  | 16kHz PCM   |--->| (Zipformer)      |--->| TCP Socket       |  |
|  | 16-bit mono |    |                  |    | text -> Claude   |  |
|  | 512 samples |    | feedAudio()      |    | response back    |  |
|  | per chunk   |    | isEndpoint()     |    +------------------+  |
|  +-------------+    | getFinalResult() |             |            |
|                     +------------------+             |            |
|                            |                         v            |
|                            | partialResult     +-----------+      |
|                            | (StateFlow)       | OfflineTts|      |
|                            v                   | (Kokoro)  |      |
|                     +------------------+       | speak()   |      |
|                     |   Room Database  |       +-----------+      |
|                     | conversations    |             |             |
|                     | messages         |             v             |
|                     +------------------+       +-----------+      |
|                            |                   | AudioTrack|      |
|                            v                   | PCM float |      |
|                     +------------------+       | mono      |      |
|                     | Compose UI       |       +-----------+      |
|                     | (observes Flow)  |             |             |
|                     +------------------+             v             |
|                                                  SPEAKER          |
+------------------------------------------------------------------+
```

### Audio Specifications

| Component | Format | Sample Rate | Channels | Buffer |
|---|---|---|---|---|
| Microphone (AudioRecord) | PCM 16-bit signed | 16,000 Hz | Mono | 512 samples (32ms) |
| ASR (Zipformer) | Float32 [-1.0, 1.0] | 16,000 Hz | Mono | Continuous stream |
| TTS (Kokoro) | Float32 | Model native | Mono | Streaming callback |
| Speaker (AudioTrack) | PCM Float | Model native | Mono | Min buffer size |

---

## 4. Entertainment Sub-Flow

When VPS is slow, the pipeline transitions to ENTERTAINING mode.

```
     SENDING state entered
            |
            | Start 3-second timer
            |
            +--- VPS responds within 3s ---> SPEAKING (no entertainment)
            |
            | Timer expires, VPS still pending
            v
     ENTERTAINING state
            |
            +---> Pick random fun fact (50 items, no repeats)
            |          |
            |          v
            |     TTS speaks the fact
            |          |
            |          v
            |     1.5s pause
            |          |
            |     VPS still pending? ----yes----> Loop (next fact)
            |          |
            |          no
            |          v
            |     Let current TTS finish naturally
            |          |
            |          v
            |     TTS: "Alright, I have the response now."
            |          |
            v          v
     SPEAKING (Claude's response)
```

---

## 5. Models Currently Used

### ASR: Streaming Zipformer Transducer (English)

| Property | Value |
|---|---|
| **Model** | `sherpa-onnx-streaming-zipformer-en-2023-06-26` |
| **Architecture** | Zipformer2 Transducer (encoder + decoder + joiner) |
| **Quantization** | INT8 |
| **Size** | ~68 MB total |
| **Language** | English |
| **Processing** | Real-time streaming (incremental decode) |
| **Endpoint detection** | Built-in (configurable silence rules) |

**Files in `assets/sherpa-onnx-streaming-zipformer-en/`:**
- `encoder.int8.onnx` (67 MB) - Acoustic encoder
- `decoder.int8.onnx` (528 KB) - Text context decoder
- `joiner.int8.onnx` (253 KB) - Combines encoder + decoder
- `tokens.txt` (5 KB) - Token vocabulary

**Current endpoint configuration:**
```
Rule 1: 1.8s silence (no speech required) = endpoint
Rule 2: 1.0s silence after speech detected = endpoint
Rule 3: 30s max utterance length = forced endpoint
```

### TTS: Kokoro English v0.19

| Property | Value |
|---|---|
| **Model** | `kokoro-en-v0_19` |
| **Architecture** | Kokoro (neural TTS with eSpeak phonemizer) |
| **Size** | ~336 MB total |
| **Language** | English |
| **Voices** | Multiple (via `voices.bin`, selected by `sid`) |
| **Speed** | 1.05x (configurable via `GenerationConfig.speed`) |

**Files in `assets/kokoro-en-v0_19/`:**
- `model.onnx` (330 MB) - Main synthesis model
- `voices.bin` (5.5 MB) - Voice embeddings
- `tokens.txt` (1 KB) - Text tokenization
- `espeak-ng-data/` - Phonetic/linguistic data for 122 languages

**Current TTS configuration:**
```
maxNumSentences = 4     (process 4 sentences at once, fewer pauses)
silenceScale = 0.05     (minimal inter-sentence silence)
speed = 1.05            (slightly faster than normal)
sid = 0                 (default voice)
numThreads = 4          (parallel processing)
```

---

## 6. Alternative Models You Can Use

### Alternative ASR Models (Streaming)

All models below use the same `OnlineRecognizer` API. Just swap the model files in `assets/` and update `SherpaAsrAdapter.initialize()`.

| Model | Type | Language | Size | Notes |
|---|---|---|---|---|
| **zipformer-en-2023-06-26** (current) | Transducer | English | 68 MB | Good accuracy, int8 |
| **zipformer-en-20M-2023-02-17** | Transducer | English | ~20 MB | Smaller, faster, less accurate |
| **lstm-en-2023-02-17** | Transducer | English | ~80 MB | LSTM alternative |
| **nemo-fast-conformer-ctc-en-80ms** | CTC | English | ~100 MB | NeMo, 80ms latency |
| **nemo-fast-conformer-ctc-en-480ms** | CTC | English | ~100 MB | NeMo, better accuracy |
| **nemotron-speech-streaming-en-0.6b** | Transducer | English | ~600 MB | Best accuracy, largest |
| **zipformer-en-kroko-2025-08-06** | Transducer | English | ~100 MB | Newest English model |
| **zipformer-bilingual-zh-en** | Transducer | Chinese+English | ~100 MB | Bilingual |
| **streaming-paraformer-bilingual-zh-en** | Paraformer | Chinese+English | ~100 MB | Different architecture |
| **zipformer-fr-2023-04-14** | Transducer | French | ~100 MB | French |
| **zipformer-korean-2024-06-16** | Transducer | Korean | ~100 MB | Korean |
| **zipformer-es-kroko-2025-08-06** | Transducer | Spanish | ~100 MB | Spanish |
| **zipformer-de-kroko-2025-08-06** | Transducer | German | ~100 MB | German |
| **zipformer-small-ru-vosk** | Transducer | Russian | ~50 MB | Russian |
| **zipformer-bn-vosk-2026-02-09** | Transducer | Bengali | ~100 MB | Bengali |

**Download from:** `https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/`

### Alternative TTS Models

All models below use the same `OfflineTts` API. Swap model files and update `SherpaTtsAdapter.initialize()`.

| Model | Architecture | Languages | Size | Quality | Notes |
|---|---|---|---|---|---|
| **Kokoro** (current) | Kokoro | English | 336 MB | High | Natural, expressive |
| **VITS** | VITS | Multi | 30-100 MB | Medium | Smaller, faster, many languages |
| **Matcha** | Matcha + Vocoder | Multi | 50-150 MB | High | Needs separate vocoder |
| **Kitten** | Kitten | English | ~200 MB | High | Alternative to Kokoro |
| **Pocket** | Pocket | English | ~300 MB | High | LM-based, more natural |
| **Supertonic** | Supertonic | Multi | ~200 MB | High | Newest architecture |
| **ZipVoice** | ZipVoice | Multi | ~150 MB | High | Encoder/decoder/vocoder |

**Download from:** `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/`

---

## 7. Configurable Parameters

### ASR Tuning (`SherpaAsrAdapter.kt`)

```kotlin
// Endpoint detection — when to consider speech "done"
EndpointConfig(
    rule1 = EndpointRule(
        mustContainNonSilence = false,
        minTrailingSilence = 1.8f,    // Seconds of silence to trigger endpoint
        minUtteranceLength = 0.0f,     // Min speech length (0 = any)
    ),
    rule2 = EndpointRule(
        mustContainNonSilence = true,  // Only trigger after actual speech
        minTrailingSilence = 1.0f,     // Shorter silence threshold
        minUtteranceLength = 0.0f,
    ),
    rule3 = EndpointRule(
        mustContainNonSilence = false,
        minTrailingSilence = 0.0f,
        minUtteranceLength = 30.0f,    // Force endpoint after 30 seconds
    ),
)

// Model threading
numThreads = 2        // CPU threads for inference (2-4 recommended)

// Decoding strategy
decodingMethod = "greedy_search"   // or "modified_beam_search"
maxActivePaths = 4                  // For beam search only

// Hotwords boost (bias toward specific words)
hotwordsFile = ""      // Path to hotwords file
hotwordsScore = 1.5f   // Boost factor
```

### TTS Tuning (`SherpaTtsAdapter.kt`)

```kotlin
// Synthesis config
OfflineTtsConfig(
    maxNumSentences = 4,     // 1-10: sentences per batch (higher = fewer pauses)
    silenceScale = 0.05f,    // 0.0-1.0: inter-sentence silence (lower = less)
)

// Per-generation config
GenerationConfig(
    sid = 0,           // Speaker ID (0 to numSpeakers-1)
    speed = 1.05f,     // 0.5-2.0: playback speed
)

// Model threading
numThreads = 4        // CPU threads (4 recommended for TTS)
```

### Pipeline Tuning (`AudioPipelineManager.kt`)

```kotlin
ENTERTAINMENT_DELAY_MS = 3000L   // Wait time before starting fun facts (ms)
// Entertainment loop: 1.5s pause between facts
```

### VPS Connection (`VpsConnectionManager.kt`)

```kotlin
MAX_RETRY_DELAY_MS = 60_000L     // Maximum retry backoff (ms)
// Socket timeout: 180 seconds (3 minutes) per request
```

---

## 8. Clean Architecture Layers

```
+------------------------------------------------------------------+
|                        PRESENTATION                               |
|  Jetpack Compose + MVI                                            |
|  ConversationScreen, HistoryScreen, SettingsBottomSheet           |
|  ViewModels observe StateFlow from service/repositories           |
+------------------------------------------------------------------+
                               |
                          StateFlow
                               |
+------------------------------------------------------------------+
|                          SERVICE                                  |
|  VoiceBridgeForegroundService (Android lifecycle)                 |
|  AudioPipelineManager (orchestrates the streaming loop)           |
|  NotificationHelper (foreground notification)                     |
+------------------------------------------------------------------+
                               |
                      Interface contracts
                               |
+------------------------------------------------------------------+
|                          DOMAIN                                   |
|  Pure Kotlin — no Android dependencies (KMP-ready)                |
|  Models: Conversation, Message, PipelineState, ConnectionState    |
|  Interfaces: SpeechRecognitionService, TextToSpeechService        |
|  Interfaces: ConversationRepository, SettingsRepository, VpsRepo  |
|  Use Cases: SendMessage, DeleteConversation, GetEntertainment     |
+------------------------------------------------------------------+
                               |
                       Implementations
                               |
+------------------------------------------------------------------+
|                           DATA                                    |
|  SherpaAsrAdapter (wraps OnlineRecognizer)                        |
|  SherpaTtsAdapter (wraps OfflineTts + AudioTrack)                 |
|  AudioRecordManager (wraps AudioRecord)                           |
|  VpsClient + VpsConnectionManager (TCP socket + reconnect)        |
|  Room Database (conversations + messages)                         |
|  DataStore Preferences (VPS host/port)                            |
|  AssetCopier (espeak-ng-data extraction)                          |
+------------------------------------------------------------------+
```

---

## 9. How to Swap Models

### Swap ASR Model

1. Download new model from sherpa-onnx releases
2. Place files in `app/src/main/assets/<model-dir>/`
3. Edit `SherpaAsrAdapter.initialize()`:
   ```kotlin
   val modelDir = "your-new-model-dir"
   // Update transducer/paraformer/ctc config as needed
   // Update modelType to match ("zipformer", "zipformer2", "lstm", "paraformer")
   ```
4. Rebuild

### Swap TTS Model

1. Download new model from sherpa-onnx releases
2. Place files in `app/src/main/assets/<model-dir>/`
3. Edit `SherpaTtsAdapter.initialize()`:
   ```kotlin
   // Use the appropriate config class:
   // OfflineTtsKokoroModelConfig for Kokoro
   // OfflineTtsVitsModelConfig for VITS
   // OfflineTtsMatchaModelConfig for Matcha
   // OfflineTtsKittenModelConfig for Kitten
   ```
4. Rebuild

### Add a New Language

1. Download the ASR model for that language (see table above)
2. Download a TTS model for that language (VITS models cover many languages)
3. Follow "Swap ASR/TTS Model" steps above
4. Optionally add a language selector in the Settings UI
