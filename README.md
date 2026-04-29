# MJ AI Assistant

> A futuristic personal AI voice assistant for Android — Iron Man–inspired, powered by Google Gemini.

## ✨ Features

- 🎙️ **Voice Commands** — Say "Hey MJ" to activate, then speak your command
- ⌨️ **Text Input** — Type commands when voice isn't convenient
- 🤖 **Gemini AI** — Powered by Google Gemini 2.0 Flash for natural conversations
- 📱 **App Control** — Open YouTube, WhatsApp, Spotify, Maps, Camera, and 10+ more
- 🔍 **Web Search** — Search Google for anything
- 🌤️ **Weather** — Get weather updates for any location
- 📰 **News** — Latest headlines
- 🎨 **Arc Reactor UI** — Dark Iron Man–themed interface with animated orb
- 🔊 **Female Voice** — Sleek female TTS voice (Iron Man–inspired)
- 💬 **Chat History** — Scrollable conversation transcript

## 🏗️ Architecture

```
MJ-V1-main/
├── backend/          # FastAPI Python backend
│   ├── main.py       # API server (chat, health, tools)
│   ├── config.py     # Settings & Gemini config
│   ├── core/         # Router, logger, utils
│   ├── services/     # Gemini, cache, tools
│   └── handlers/     # API endpoint handlers
│
├── mj-native-android/   # Native Android app (Kotlin)
│   └── app/src/main/
│       ├── java/com/mj/assistant/
│       │   ├── MainActivity.kt    # Full UI
│       │   ├── GeminiClient.kt    # Gemini API
│       │   ├── VoiceService.kt    # Background voice
│       │   └── ActionExecutor.kt  # App/device actions
│       └── res/                   # Marvel theme resources
│
└── .github/workflows/build.yml  # CI/CD → APK
```

## 🚀 Quick Start

### Backend
```bash
cd backend
pip install -r requirements.txt
cp .env.example .env  # Add your Gemini API key
python main.py
```

### Android APK
1. Go to **Actions** tab → latest build → download `app-debug.apk`
2. Install on your Android phone
3. Launch **MJ Assistant** → tap the mic → say "Hey MJ"

### Local Development
```bash
# Backend
cd backend && uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Android
cd mj-native-android && ./gradlew assembleDebug
```

## 🎤 Voice Commands

| Command | Action |
|---------|--------|
| "Hey MJ" | Wake word activation |
| "Open YouTube" | Launch YouTube app |
| "Open WhatsApp" | Launch WhatsApp |
| "What's the weather in Mumbai?" | Weather search |
| "Search for restaurants near me" | Google search |
| "Tell me a joke" | AI conversation |
| "Open camera" | Launch camera |
| "Play Arijit Singh songs" | Spotify/YouTube search |

## 📋 Requirements

- Android 8.0+ (API 26)
- Internet connection (for Gemini API)
- Microphone permission
- Google Gemini API key

## 🔑 API Key Setup

1. Get a free Gemini API key from [Google AI Studio](https://aistudio.google.com/)
2. Add to `backend/.env`: `GEMINI_API_KEY=your_key_here`
3. For GitHub Actions: Add as repository secret `GEMINI_API_KEY`

---

Built by Rohit Gangurde 🚀
