# MJ Voice Assistant - PRD

## Goal
A futuristic Iron Man–inspired mobile voice assistant named "MJ" that listens to user
voice or text commands, classifies intent via Gemini, executes safe device or
backend tools, and replies with natural female assistant voice. All branding is
original (no Marvel/Iron Man/Siri copying).

## Architecture
- **Frontend**: Expo React Native (SDK 54) + expo-router file-based routes
- **Backend**: FastAPI + MongoDB (motor) + modular services
- **Reasoning**: Google Gemini (gemini-2.5-flash) — structured JSON intent classification
- **Voice**: Sarvam AI primary (STT + TTS) → OpenAI Whisper / TTS fallback (Emergent universal key) → device TTS via expo-speech as last resort
- **Tools**: FastMCP-style registry on backend (time, weather, news, web_search) + local Expo Linking executor (open_app, dialer, sms, browser, maps, calendar)

## State machine (frontend)
IDLE → LISTENING → TRANSCRIBING → THINKING → EXECUTING_TOOL → SPEAKING → IDLE
Plus ERROR and PERMISSION_REQUIRED states. Each shows a distinct orb color and
HUD label. Implicit waits with loaders prevent UI freeze.

## Key endpoints
- GET /api/health
- POST /api/assistant/chat       (text in, JSON+TTS out)
- POST /api/assistant/voice      (audio in → STT → orchestration → TTS)
- POST /api/voice/transcribe
- POST /api/voice/synthesize
- GET /api/tools/list
- POST /api/tools/execute
- GET /api/conversations/{session_id}
- DELETE /api/conversations/{session_id}

## Frontend screens
- `/` Main MJ assistant (orb, waveform, transcript, quick chips, mic FAB, text input)
- `/settings` Voice toggle, wake-word toggle, language picker, status, clear data, about

## Branding
- Original MJ orb: cyan→teal→violet→amber color states with dashed/dotted rings, glow shadow
- Dark theme (#050505), Outfit-style typography, mono HUD labels
- No Marvel/Iron Man/Siri assets

## Permissions
- RECORD_AUDIO requested at first mic press, with rationale
- iOS NSMicrophoneUsageDescription set

## Reasoning fallback chain (intent classification)
1. **Gemini 2.5 Flash** (primary) — if user-provided key is valid and within quota
2. **OpenAI gpt-4o-mini** via Emergent Universal Key — auto-engaged when Gemini fails (quota exhausted, network error, invalid response)
3. **Deterministic regex heuristic** — final fallback for common intents (time/weather/news/open_app/greeting/web_search)

This means MJ keeps working with live data even when Gemini's daily free-tier limit (20 req/day) is hit.

## Smart business enhancement
- Modular FastMCP tool registry: trivial to add new revenue/UX tools (smart-home, calendars, payments)
  without touching the orchestrator — just register in `tools_service.TOOLS`.
