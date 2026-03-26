"""
MJ AI Assistant – AI Handler
Processes general questions through the AI model and prepares speech output.
"""

from core.utils import truncate_for_speech
from core.logger import get_logger
from services.model_service import ModelService

import config

_log = get_logger("handlers.ai")

# Lazy singleton
_model: ModelService | None = None


def _get_model() -> ModelService:
    global _model
    if _model is None:
        _model = ModelService()
    return _model


def handle_query(question: str) -> str:
    """Send *question* to the AI model and return a speech-ready answer."""
    _log.info("AI query: %s", question)
    raw = _get_model().ask(question)
    answer = truncate_for_speech(raw, config.AI_MAX_RESPONSE_CHARS)
    _log.info("AI answer (trimmed): %s", answer[:120])
    return answer
