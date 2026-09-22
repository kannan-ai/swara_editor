# Swara Editor - Native Android Video Editing Engine

Swara Editor is a native Android video editing application and multi-track engine built with **Jetpack Compose** and **AndroidX Media3 (ExoPlayer & Transformer 1.11.0)**.

---

## ✨ Features

- **Pinch-to-Zoom Timebase Engine**: Zoomable timeline (`20-300 dp/s`) with `150ms` magnetic snapping and haptic feedback.
- **Canvas Aspect Ratio Framing & Background Blur**: Presets for `9:16` Shorts/Reels, `16:9`, `1:1`, `4:5`, and `Original` with dynamic dual-surface blurred letterbox fill.
- **Audio Waveform Visualization & Mixing**: Asynchronous PCM amplitude decoding, memory caching, volume, mute, fade-in/fade-out envelopes (`0-3s`), and "Extract Audio" tool.
- **Granular Color Grading & Custom OpenGL Shaders**: Real-time Brightness, Contrast, Saturation, Warmth, and custom GLSL Vignette Shaders.
- **Clip Transition Nodes & Hardware Encoder Fallback**: Inter-clip transition nodes (`Crossfade`, `Fade Black`, `Slide Left`, `Wipe Right`) with automatic HEVC to AVC codec fallback on export.
- **Keyframe Animation Engine**: Smooth cubic Hermite curve interpolation for Translation X/Y, Scale, Rotation, and Opacity.
- **Dynamic Speed Ramping**: Piecewise smooth velocity curves (`Hero`, `Bullet`, `Montage Jump`, `Custom`) synchronized with ExoPlayer playback.
- **Chroma Key (Green Screen) & Visual Masking**: YUV/RGB color-distance thresholding and geometric masking (`Rectangle`, `Circle`, `Linear`, `Mirror`) with soft edge feathering (`0.0-0.3`).
- **AI Auto-Captions & Word-Level Karaoke Styling**: Asynchronous speech-to-text transcription with word-by-word karaoke bounce animations and gold/neon highlight styles.
- **Automated In-App Diagnostic Suite**: Headless component verification testing all 9 phases via ADB broadcast or in-app trigger.
- **Studio Gallery & Project Re-Editing**: Local JSON persistence (`ProjectRepository`) allowing saving, renaming, deleting, and re-editing video drafts.
- **Dynamic Themes**: Support for Light Theme, Dark Theme, and Pure OLED Black Theme (`#000000` true dark).

---

## 🛠️ Project Setup & Building

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17 or higher
- Android SDK 35 / 37 (minSdk 24)

### Building via Command Line
```bash
./gradlew :app:assembleDebug
```

---

## 🚀 Pushing to GitHub

To link and push this repository to your GitHub account (`kannan-ai`):

1. Create a new repository on GitHub:
   - Go to [https://github.com/new](https://github.com/new)
   - Repository name: `swara_editor`
   - Owner: `kannan-ai`
   - Description: *Native Android Video Editing Engine using Jetpack Compose and Media3 Transformer*
   - Keep "Initialize this repository with a README" **unchecked** (since local repo is already initialized).

2. Run the following terminal commands to add the remote and push:
   ```bash
   git remote add origin https://github.com/kannan-ai/swara_editor.git
   git branch -M main
   git push -u origin main
   ```

---

## 🧪 Diagnostic Runner (ADB Broadcast)

Execute non-UI component diagnostics directly from terminal:
```bash
adb shell am broadcast -a com.example.swara_editor.RUN_DIAGNOSTIC
```
Check results in Logcat:
```bash
adb logcat -d -s SWARA_DIAGNOSTIC
```

---

## 📜 License
Licensed under the Apache License, Version 2.0.
