"""
MJ AI Assistant – Chat Handler
Handles /api/assistant/chat endpoint logic.
"""

import uuid

from core.logger import get_logger
from core.router import route, Intent
from services.gemini_service import get_gemini
from services.cache_service import get_cache
from services.tools_service import execute_tool

_log = get_logger("handlers.chat")


async def handle_chat(user_text: str, session_id: str | None = None) -> dict:
    """Process a user chat message and return structured response."""

    if not session_id:
        session_id = str(uuid.uuid4())

    _log.info("[%s] User: %s", session_id[:8], user_text[:100])

    # Check cache first
    cache = get_cache()
    cache_key = f"{session_id}:{user_text.strip().lower()}"
    cached = cache.get(cache_key)
    if cached:
        _log.info("Serving from cache")
        return cached

    # Route intent (deterministic fallback)
    intent, payload = route(user_text)

    # For tool-based intents, execute tool first
    tool_result = None
    if intent == Intent.TIME:
        tool_result = execute_tool("time")
    elif intent == Intent.WEATHER:
        tool_result = execute_tool("weather", location=payload.replace("weather", "").strip())
    elif intent == Intent.NEWS:
        tool_result = execute_tool("news")

    # Always send to Gemini for natural response
    gemini = get_gemini()
    ai_result = gemini.chat(user_text, session_id)

    response = {
        "session_id": session_id,
        "user_text": user_text,
        "intent": intent.value,
        "spoken_response": ai_result["spoken_response"],
        "action": ai_result.get("action"),
        "action_target": ai_result.get("action_target"),
        "tool_result": tool_result,
    }

    # Cache the response
    cache.put(cache_key, response)

    _log.info("[%s] MJ: %s", session_id[:8], response["spoken_response"][:80])
    return response
