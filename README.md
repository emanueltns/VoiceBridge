# VoiceBridge

Hands-free voice assistant for Android. Talk to Claude using on-device speech recognition and text-to-speech, connected to a VPS running Claude via TCP.

## How It Works

```
Microphone -> Streaming ASR (Zipformer) -> VPS (Claude) -> TTS (Kokoro) -> Speaker
```

Everything runs on-device except the Claude conversation, which goes through your VPS over Tailscale.

## Setup

### 1. Build & Install

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Configure VPS

1. Open the app
2. Tap the gear icon (Settings)
3. Enter your Tailscale IP and port
4. Tap Save

### 3. Start Talking

Tap the green call button. The mesh orb lights up green when listening. Just talk naturally -- VoiceBridge detects when you stop speaking, sends your words to Claude, and speaks the response back.

## Features

- **3D Mesh Sphere Orb** -- flowing particle wireframe that reacts to your voice
- **State Colors** -- orb changes color per state (green=listening, amber=thinking, purple=speaking)
- **Voice Announcements** -- hear "Let me think about that" and "I'm listening" so you know what's happening while driving
- **Mute Microphone** -- tap the center mic button to mute/unmute (red ring = muted)
- **53 TTS Voices** -- Kokoro v1.0 with 9 languages (American/British English, Spanish, French, Hindi, Italian, Japanese, Portuguese, Chinese)
- **Conversation History** -- tap any past conversation to read the full chat
- **Entertainment Mode** -- fun facts spoken while waiting for slow responses

## Voice Selection

Open Settings and scroll to the Voice section. Pick any of the 53 built-in voices grouped by language and gender.

### Custom Voice Blending

You can create custom blended voices by mixing speaker embeddings:

```bash
python3 scripts/blend_voices.py
```

#### How to use

1. Edit `scripts/blend_voices.py` and modify the `RECIPES` list:

```python
RECIPES = [
    Recipe(
        name="My Custom Voice",
        blend={
            "af_nicole": 1.0,
            "af_sarah": 0.58,
            "am_adam": 0.33,
        },
    ),
]
```

2. Run the script:

```bash
cd /path/to/VoiceBridge
python3 scripts/blend_voices.py
```

3. The script will:
   - Back up your original `voices.bin` (first run only)
   - Blend the voices by weighted average of their embeddings
   - Append new speakers to `voices.bin` (sid 53, 54, 55...)
   - Print the code to add to the Android settings UI

4. Update `SettingsBottomSheet.kt` with the new voice entries (the script prints exactly what to paste)

5. Rebuild the app: `./gradlew assembleDebug`

#### Available voices for blending

| sid | Name | Language |
|-----|------|----------|
| 0 | af_alloy | American English Female |
| 1 | af_aoede | American English Female |
| 2 | af_bella | American English Female |
| 3 | af_heart | American English Female |
| 4 | af_jessica | American English Female |
| 5 | af_kore | American English Female |
| 6 | af_nicole | American English Female |
| 7 | af_nova | American English Female |
| 8 | af_river | American English Female |
| 9 | af_sarah | American English Female |
| 10 | af_sky | American English Female |
| 11 | am_adam | American English Male |
| 12 | am_echo | American English Male |
| 13 | am_eric | American English Male |
| 14 | am_fenrir | American English Male |
| 15 | am_liam | American English Male |
| 16 | am_michael | American English Male |
| 17 | am_onyx | American English Male |
| 18 | am_puck | American English Male |
| 19 | am_santa | American English Male |
| 20 | bf_alice | British English Female |
| 21 | bf_emma | British English Female |
| 22 | bf_isabella | British English Female |
| 23 | bf_lily | British English Female |
| 24 | bm_daniel | British English Male |
| 25 | bm_fable | British English Male |
| 26 | bm_george | British English Male |
| 27 | bm_lewis | British English Male |
| 28 | ef_dora | Spanish Female |
| 29 | em_alex | Spanish Male |
| 30 | em_santa | Spanish Male |
| 31 | ff_siwis | French Female |
| 32 | hf_alpha | Hindi Female |
| 33 | hf_beta | Hindi Female |
| 34 | hm_omega | Hindi Male |
| 35 | hm_psi | Hindi Male |
| 36 | if_sara | Italian Female |
| 37 | im_nicola | Italian Male |
| 38 | jf_alpha | Japanese Female |
| 39 | jf_gongitsune | Japanese Female |
| 40 | jf_nezumi | Japanese Female |
| 41 | jf_tebukuro | Japanese Female |
| 42 | jm_kumo | Japanese Male |
| 43 | pf_dora | Portuguese Female |
| 44 | pm_alex | Portuguese Male |
| 45 | pm_santa | Portuguese Male |
| 46 | zf_xiaobei | Chinese Female |
| 47 | zf_xiaoni | Chinese Female |
| 48 | zf_xiaoxiao | Chinese Female |
| 49 | zf_xiaoyi | Chinese Female |
| 50 | zm_yunjian | Chinese Male |
| 51 | zm_yunxi | Chinese Male |
| 52 | zm_yunyang | Chinese Male |

Weights are auto-normalized -- just set relative proportions. For example `{"af_nicole": 1.0, "am_adam": 0.5}` becomes 67% Nicole + 33% Adam.

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full system design including state machines, audio pipeline, and model configurations.

## Models

| Component | Model | Size |
|-----------|-------|------|
| ASR | Zipformer Transducer (streaming, English) | ~68 MB |
| TTS | Kokoro multi-lang v1.0 (53 speakers, 9 languages) | ~393 MB |
| AI | Claude (via VPS + TCP) | Remote |
