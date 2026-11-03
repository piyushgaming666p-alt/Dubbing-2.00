# Bhasha Studio (भाषा स्टूडियो)

**Bhasha Studio** is a native Android video editing workstation built using **Kotlin**, **Jetpack Compose**, and **Material 3**. Inspired by CapCut, it integrates multi-track video editing with **multilingual AI speech translation**, **real Hindi voice dubbing**, and **automatic synchronized subtitles** on Android devices with 4–6 GB RAM.

---

## 📱 App Highlights & Architecture

- **App Name:** Bhasha Studio
- **Package / Application ID:** `com.bhashastudio.app`
- **UI Design:** Dark Obsidian & Violet Studio Theme (`#0D0B14`, `#161224`, `#A855F7`)
- **Target OS:** Android 7.0+ (API 24 to 36)
- **Memory Footprint:** Tailored for devices with 4–6 GB RAM with optimized frame extraction caching and zero memory leaks.

---

## 🎬 Core Features Breakdown

### 1. Video Editing Engine
- **System File Picker:** Import videos and audio directly from Android device storage using system pickers (`ActivityResultContracts.GetContent`) with zero sensitive permissions needed.
- **Hardware-Accelerated Video Player:** Real-time preview with play, pause, seek, loop, volume, and rotation.
- **Multi-Track Timeline:**
  - **Video Track:** Clip thumbnails, duration badges, trim handles, and split points.
  - **Subtitle Track:** Visual cue blocks with speaker badges and text snippets.
  - **Dubbed Voice Track:** Synchronized Hindi synthesized speech markers.
  - **BGM & SFX Track:** Background audio and sound effects.
  - **Time Ruler & Playhead:** High-precision millisecond scrubber.
- **Editing Tools:** Trim in/out, split at playhead, rotate (90° steps), speed control (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x), aspect ratio switching (16:9 Landscape, 9:16 Shorts/Reels, 1:1 Square, 4:5 Portrait, 21:9 Cinema).
- **Project Persistence:** Full offline Room Database (`bhasha_studio.db`) saving projects, clips, tracks, and subtitles.

### 2. AI Translation & Real Hindi Voice Dubbing
- **Supported Languages:**
  - 🇮🇳 Hindi (हिन्दी)
  - 🌐 English
  - 🇮🇳 Punjabi (ਪੰਜਾਬੀ)
  - 🇮🇳 Bengali (বাংলা)
  - 🇮🇳 Marathi (मराठी)
  - 🇮🇳 Gujarati (ગુજરાતી)
  - 🇮🇳 Tamil (தமிழ்)
  - 🇮🇳 Telugu (తెలుగు)
  - 🇮🇳 Urdu (اردو)
  - 🇮🇳 Kannada (ಕನ್ನಡ)
  - 🇮🇳 Malayalam (മലയാളം)
- **Dialogue Transcription:** Audio dialogue cue detection with silence detection and phrase segmentation.
- **Multilingual Speech Translation:** Contextual translation between English, Hindi, and regional languages.
- **Real Hindi Voice Dubbing (`TextToSpeechDubbingService`):**
  - Powered by Android's native `android.speech.tts.TextToSpeech` engine.
  - Generates **real `.wav` audio files** directly on device via `tts.synthesizeToFile()`.
  - Audition voices instantly with one-tap live preview.
  - Voice profiles (Female Studio, Male Studio, Conversational Host) with fine-grained Pitch and Speed adjustments.
  - Synchronizes synthesized dubbed audio cues with video timeline positions.

### 3. Automatic Subtitles & SRT Studio
- **Auto-Subtitles Generation:** Dialogue extraction with millisecond timestamps.
- **Multilingual Subtitle Translation:** One-tap translation of all subtitle cards into Hindi, Punjabi, Bengali, Tamil, etc.
- **Subtitle Card Editor:** Adjust start/end timestamps and edit Devanagari / English text in real time.
- **Customizable Styling:** Font size (14–32sp), vibrant text colors (Yellow, White, Cyan, Emerald, Pink), background box styles, and screen positions (Top, Center, Bottom).
- **SRT File Import & Export:** Copy SRT, share `.srt` files, or paste external subtitle files.
- **Burned-in Subtitles:** Render subtitles directly into the video frames during MP4 export.

### 4. Audio Studio & Sound Effects
- **Independent Track Mixer:** Sliders for Original Video, Hindi Dubbing, BGM, and Voice-over (0% to 200%).
- **Original Video Mute Switch:** Mute original dialogue to let Hindi dubbing shine.
- **Royalty-Free Sound FX Library:** Pre-synthesized clean 44.1kHz audio presets (Lo-Fi Chill, Cinematic Swell, Upbeat Beat, Whoosh, Notification Chime, Audience Applause).
- **Live Voice-over Recorder:** Integrated microphone recorder with live waveform amplitude meter and elapsed timer.

### 5. High-Performance MP4 Video Export
- **Presets:** 720p HD (fast and memory-safe for 4–6 GB RAM) and 1080p Full HD.
- **Frame Rates:** 24 FPS, 30 FPS, 60 FPS.
- **Hardware Encoder:** Android `MediaCodec` (H.264 / AVC) + `MediaMuxer`.
- **Options:** Burn-in subtitles, include Hindi dubbing, include BGM, mute original audio.
- **Real Progress Indicator:** 0% to 100% progress tracking with status stages.
- **Post-Export Actions:** Direct video playback and system share sheet.

---

## 🛠️ Implemented Features vs. Optional Cloud Extensions

| Feature | Implementation Status | Technical Mechanism |
| :--- | :--- | :--- |
| **Video Import & Preview** | ✅ Implemented (100% Native) | System File Picker (`GetContent`) + `TextureView` / `MediaPlayer` |
| **Trim, Split, Rotate, Aspect Ratio** | ✅ Implemented (100% Native) | Timeline math, `VideoClip` state engine, letterbox canvas |
| **Project Save / Reopen** | ✅ Implemented (100% Native) | Local Room Database (`AppDatabase`, `ProjectDao`) + Moshi |
| **Audio Dubbing Voice Synthesis** | ✅ Implemented (100% Real) | Android `TextToSpeech.synthesizeToFile()` generating real WAVs |
| **SRT Import & Export** | ✅ Implemented (100% Native) | `SrtParser` compliant with standard `.srt` format |
| **BGM & Sound Effects Library** | ✅ Implemented (100% Native) | Synthesized 44.1kHz PCM WAV generator (`SoundEffectsLibrary`) |
| **Voice-over Microphone Recorder** | ✅ Implemented (100% Native) | `MediaRecorder` with AAC encoding and live amplitude |
| **MP4 Video Export** | ✅ Implemented (100% Native) | Hardware `MediaCodec` + `MediaMuxer` with burned-in subtitles |
| **Cloud Speech Recognition (ASR)** | ⚡ Ready for Backend API | Local parser active; backend API key (Gemini / Whisper) optional |
| **Cloud Neural AI Voices** | ⚡ Ready for Backend API | Native TTS active; backend ElevenLabs / Google Cloud TTS optional |

---

## 🔨 How to Build the Installable APK

### Option 1: AI Studio Export & Build
1. Open the project in Google AI Studio.
2. In the top-right menu, select **Export Project** or **Build APK**.
3. Download the generated APK directly to your Android device or run in the streaming emulator.

### Option 2: Command Line (Gradle)
Run the following command from the root directory:
```bash
gradle assembleDebug
```
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To install on a connected USB device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Running Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```
Tests verify app naming, SRT parsing/export roundtrip, and multilingual language detection.
