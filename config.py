"""
MJ AI Assistant – Configuration
Loads environment variables and exposes all settings as constants.
"""

import os
from dotenv import load_dotenv

load_dotenv()

# ── API Keys ─────────────────────────────────────────────────────────────────
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

# ── Assistant Identity ───────────────────────────────────────────────────────
ASSISTANT_NAME = "MJ"
WAKE_PHRASE = "mj"

# ── Phrases ──────────────────────────────────────────────────────────────────
GREETING = "MJ Assistant is online. Say MJ to activate me."
ACKNOWLEDGE = "Yes, I'm listening."
GOODBYE = "Goodbye! Have a great day."
ASK_REPEAT = "I did not catch that. Please say it again."
NO_MIC = "I cannot access the microphone right now."
NO_INTERNET = "Internet connection seems unavailable."
AI_ERROR = "I cannot answer that at the moment."
BROWSER_ERROR = "I could not open that page."
FALLBACK_ERROR = "Something went wrong. Let me try again."

# ── Website URLs ─────────────────────────────────────────────────────────────
URLS = {
    "youtube": "https://www.youtube.com",
    "whatsapp": "https://web.whatsapp.com",
    "google": "https://www.google.com",
}

WEATHER_SEARCH_URL = "https://www.google.com/search?q=weather+{query}"

# ── TTS Settings ─────────────────────────────────────────────────────────────
TTS_RATE = 175          # words per minute
TTS_VOLUME = 1.0        # 0.0 to 1.0

# ── Speech Recognition Settings ─────────────────────────────────────────────
LISTEN_TIMEOUT = 5      # seconds to wait for speech to start
PHRASE_TIME_LIMIT = 8   # max seconds of speech to capture
WAKE_LISTEN_TIMEOUT = 3
WAKE_PHRASE_LIMIT = 4

# ── AI Settings ──────────────────────────────────────────────────────────────
AI_MODEL_NAME = "gemini-2.0-flash"
AI_MAX_RESPONSE_CHARS = 500   # truncate for speech output
AI_SYSTEM_PROMPT = (
    "You are MJ, a helpful and concise voice assistant. "
    "Keep your answers brief and conversational since they will be spoken aloud. "
    "Aim for 2-3 sentences unless the user asks for more detail."
)

# ── Logging ──────────────────────────────────────────────────────────────────
LOG_FILE = "logs/assistant.log"
LOG_FORMAT = "%(asctime)s | %(name)-18s | %(levelname)-7s | %(message)s"
LOG_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"
