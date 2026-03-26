"""
MJ AI Assistant – Listener
Wake-word detection and single-command capture with retry.
"""

import config
from core.logger import get_logger
from services.speech_service import SpeechService

_log = get_logger("core.listener")

# Module-level singleton
_speech: SpeechService | None = None


def _get_speech() -> SpeechService:
    global _speech
    if _speech is None:
        _speech = SpeechService()
    return _speech


def wait_for_wake_word() -> bool:
    """Block until the wake phrase is detected. Returns True on success."""
    svc = _get_speech()
    text = svc.listen(
        timeout=config.WAKE_LISTEN_TIMEOUT,
        phrase_time_limit=config.WAKE_PHRASE_LIMIT,
    )
    if text and config.WAKE_PHRASE in text.lower():
        _log.info("Wake phrase detected!")
        return True
    return False


def get_command(retries: int = 1) -> str | None:
    """Capture one user command after activation.

    Retries once if speech is unclear. Returns the recognised text
    or None if both attempts fail.
    """
    svc = _get_speech()

    for attempt in range(1 + retries):
        if attempt > 0:
            _log.info("Retry %d/%d …", attempt, retries)
        text = svc.listen()
        if text:
            _log.info("Command captured: %s", text)
            return text

    _log.warning("Failed to capture command after %d attempt(s)", 1 + retries)
    return None
