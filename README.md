# MJ AI Assistant for Windows

A lightweight voice-first desktop assistant built in Python. Say **"MJ"** to activate, then speak a command.

## Features (V1)

- 🎙️ Voice activation with the wake phrase **"MJ"**
- 🌐 Quick-open YouTube, WhatsApp Web, Google
- 🌦️ Browser-based weather search
- 🤖 AI-powered answers via Google Gemini
- 🔊 Spoken responses using text-to-speech

## Prerequisites

| Requirement | Details |
|---|---|
| Python | 3.10 or above |
| OS | Windows 10+ |
| Hardware | Microphone + Speakers |
| API Key | [Google Gemini API key](https://aistudio.google.com/app/apikey) |

## Setup

```bash
# 1. Clone or download the project
cd "MJ-AI Assistant"

# 2. Create a virtual environment (recommended)
python -m venv venv
venv\Scripts\activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Configure your API key
copy .env.example .env
# Edit .env and paste your Gemini API key

# 5. Run the assistant
python main.py
```

## Supported Commands

| Say this | What happens |
|---|---|
| "MJ" | Activates the assistant |
| "Open YouTube" | Opens YouTube in browser |
| "Open WhatsApp" | Opens WhatsApp Web in browser |
| "Open Google" | Opens Google in browser |
| "Weather in Mumbai" | Searches weather on Google |
| Any question | Gets an AI-generated answer |
| "Stop" / "Exit" | Shuts down the assistant |

## Project Structure

```
MJ-AI Assistant/
├── main.py              # Entry point
├── config.py            # Settings & constants
├── core/                # Listener, speaker, router, logger, utils
├── handlers/            # Web, weather, AI handlers
├── services/            # TTS, speech, AI model services
├── tests/               # Unit tests
└── logs/                # Runtime logs
```

## License

Personal project by Rohit Gangurde.
