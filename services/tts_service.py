"""
MJ AI Assistant – TTS Service
Wraps pyttsx3 for text-to-speech output.
"""

import pyttsx3

import config
from core.logger import get_logger

_log = get_logger("services.tts")


class TTSService:
    """Thin wrapper around pyttsx3 engine."""

    def __init__(self):
        self._engine = pyttsx3.init()
        self._engine.setProperty("rate", config.TTS_RATE)
        self._engine.setProperty("volume", config.TTS_VOLUME)
        _log.info("TTS engine initialised (rate=%d, vol=%.1f)", config.TTS_RATE, config.TTS_VOLUME)

    def speak(self, text: str) -> None:
        """Convert *text* to speech and block until finished."""
        if not text:
            return
        _log.debug("Speaking: %s", text[:80])
        self._engine.say(text)
        self._engine.runAndWait()
