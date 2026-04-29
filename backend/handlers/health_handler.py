"""
MJ AI Assistant – Health Handler
"""

import config
from core.logger import get_logger

_log = get_logger("handlers.health")


async def health_check() -> dict:
    """Return system health status."""
    gemini_ok = bool(config.GEMINI_API_KEY)
    return {
        "status": "healthy",
        "assistant": config.ASSISTANT_NAME,
        "gemini": gemini_ok,
        "model": config.AI_MODEL_NAME,
    }
