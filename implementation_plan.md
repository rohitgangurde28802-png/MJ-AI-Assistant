# MJ AI Assistant V1 – Implementation Plan

Build a Python voice-first desktop assistant for Windows that listens for the wake phrase "MJ", captures commands, routes them to action handlers or AI, and responds via speech.

## Proposed Changes

### Foundation

#### [NEW] [requirements.txt](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/requirements.txt)
Python dependencies: `SpeechRecognition`, `pyttsx3`, `PyAudio`, `google-generativeai`, `python-dotenv`.

#### [NEW] [.env.example](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/.env.example)
Template with `GEMINI_API_KEY=your_key_here`.

#### [NEW] [config.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/config.py)
Loads `.env`, exposes `GEMINI_API_KEY`, site URLs, assistant phrases, and settings as constants.

#### [NEW] [README.md](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/README.md)
Setup instructions, prerequisites, usage guide.

---

### Output Layer (TTS) – Build First

#### [NEW] [services/tts_service.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/services/tts_service.py)
- `TTSService` class wrapping `pyttsx3`.
- `speak(text)` — converts text to speech at moderate rate.
- Configurable rate/volume via `config.py`.

#### [NEW] [core/speaker.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/core/speaker.py)
- High-level `say(text)` function used across the app.
- Delegates to `TTSService`, logs what is spoken.

---

### Input Layer (Speech Recognition) – Build Second

#### [NEW] [services/speech_service.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/services/speech_service.py)
- `SpeechService` class using `speech_recognition.Recognizer`.
- `listen(timeout, phrase_limit)` → returns recognized text or `None`.
- Handles `UnknownValueError`, `RequestError`, microphone exceptions.

#### [NEW] [core/listener.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/core/listener.py)
- `wait_for_wake_word()` — loops until "mj" is detected in recognized text.
- `get_command()` — after wake, captures one command with retry logic.

---

### Command Routing

#### [NEW] [core/router.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/core/router.py)
- `route(command_text)` → returns `(intent, payload)`.
- Intents: `OPEN_YOUTUBE`, `OPEN_WHATSAPP`, `OPEN_GOOGLE`, `WEATHER_SEARCH`, `EXIT`, `AI_QUERY`.
- Keyword matching on normalized text; unmatched → `AI_QUERY`.

#### [NEW] [core/utils.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/core/utils.py)
- `normalize(text)` — lowercase, strip filler words.
- `truncate_for_speech(text, max_chars)` — trim AI responses for TTS.

---

### Action Handlers

#### [NEW] [handlers/web_handler.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/handlers/web_handler.py)
- `open_youtube()`, `open_whatsapp()`, `open_google()` using `webbrowser.open()`.
- Returns success/failure boolean.

#### [NEW] [handlers/weather_handler.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/handlers/weather_handler.py)
- `search_weather(query)` — opens `https://www.google.com/search?q=weather+{query}` in browser.

---

### AI Conversation

#### [NEW] [services/model_service.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/services/model_service.py)
- `ModelService` class wrapping `google.generativeai`.
- `ask(prompt)` → returns cleaned response string.
- Handles API errors gracefully.

#### [NEW] [handlers/ai_handler.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/handlers/ai_handler.py)
- `handle_query(question)` — calls `ModelService.ask()`, truncates response, returns text for speaking.

---

### Logging

#### [NEW] [core/logger.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/core/logger.py)
- Configures Python `logging` with file handler (`logs/assistant.log`) + console handler.
- Provides `get_logger(name)` factory.

---

### Main Loop

#### [NEW] [main.py](file:///c:/Users/Rohit%20Gangurde/Documents/Personal%20Projects/MJ-AI%20Assistant/main.py)
- Entry point: initializes TTS, verifies mic, loads config.
- Passive listening loop → wake detection → command capture → route → execute → speak result → repeat.
- All stages wrapped in try/except with spoken error messages.
- Graceful shutdown on keyboard interrupt.

---

## Verification Plan

### Automated Tests

Run all tests with:
```
cd "c:\Users\Rohit Gangurde\Documents\Personal Projects\MJ-AI Assistant"
python -m pytest tests/ -v
```

| Test file | What it covers |
|---|---|
| `tests/test_router.py` | Keyword matching for all intents; unmatched → `AI_QUERY`; edge cases (empty, mixed case) |
| `tests/test_web_handler.py` | `webbrowser.open` called with correct URLs (mocked) |
| `tests/test_ai_handler.py` | Response truncation; API error fallback (mocked) |

### Manual Verification

> [!IMPORTANT]
> Manual testing requires a working microphone, speakers, internet, and a valid `GEMINI_API_KEY` in `.env`.

1. **Launch**: Run `python main.py` — assistant should announce "MJ Assistant is online."
2. **Wake word**: Say "MJ" — assistant should say "Yes, I'm listening."
3. **Open YouTube**: Say "open YouTube" — browser tab opens `youtube.com`.
4. **Open WhatsApp**: Say "open WhatsApp" — browser tab opens `web.whatsapp.com`.
5. **Weather**: Say "weather in Mumbai" — browser tab opens Google weather search.
6. **AI question**: Say "What is Python?" — assistant speaks a concise answer.
7. **Speech failure**: Mumble or stay silent — assistant says "I did not catch that" and retries once.
8. **Exit**: Say "exit" or "stop" — assistant says goodbye and shuts down.
