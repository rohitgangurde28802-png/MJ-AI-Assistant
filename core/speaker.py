"""
MJ AI Assistant – Speaker
High-level voice output used by the rest of the app.
"""

from services.tts_service import TTSService
from core.logger import get_logger

_log = get_logger("core.speaker")

# Module-level singleton (created lazily)
_tts: TTSService | None = None


def _get_tts() -> TTSService:
    global _tts
    if _tts is None:
        _tts = TTSService()
    return _tts


def say(text: str) -> None:
    """Speak *text* aloud and log it."""
    _log.info("🔊 %s", text)
    _get_tts().speak(text)
