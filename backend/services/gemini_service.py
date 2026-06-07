"""
MJ AI Assistant – Gemini Service
Handles conversation with Google Gemini API, maintaining session history in SQLite.
"""

import google.generativeai as genai

import config
from core.logger import get_logger
from core.utils import truncate_for_speech
from core import database

_log = get_logger("services.gemini")


class GeminiService:
    """Wraps the Google Generative AI SDK with session-based conversation."""

    def __init__(self):
        if not config.GEMINI_API_KEY:
            _log.warning("GEMINI_API_KEY is not set – AI queries will fail")
        genai.configure(api_key=config.GEMINI_API_KEY)
        self._model = genai.GenerativeModel(
            model_name=config.AI_MODEL_NAME,
            system_instruction=config.AI_SYSTEM_PROMPT,
        )
        _log.info("Gemini model ready: %s", config.AI_MODEL_NAME)

    def chat(self, prompt: str, session_id: str | None = None) -> dict:
        """Send prompt to Gemini, return structured response with action parsing."""
        try:
            _log.info("AI prompt [%s]: %s", session_id or "new", prompt[:120])

            # Get session history from SQLite
            history = []
            if session_id:
                db_messages = database.get_messages(session_id, limit=20)
                for msg in db_messages:
                    history.append({
                        "role": msg["role"],
                        "parts": [msg["content"]]
                    })

            # Create chat with history
            chat = self._model.start_chat(history=history)
            response = chat.send_message(prompt)
            raw_text = response.text.strip()

            # Parse action tags from response
            action = None
            action_target = None
            spoken_text = raw_text

            if "[ACTION:" in raw_text:
                import re
                match = re.search(r"\[ACTION:([^\]]+)\]", raw_text)
                if match:
                    full_action = match.group(1)
                    if ":" in full_action:
                        parts = full_action.split(":", 1)
                        action = parts[0].strip()
                        action_target = parts[1].strip()
                    else:
                        action = full_action.strip()
                    spoken_text = re.sub(r"\[ACTION:[^\]]+\]\s*", "", raw_text).strip()

            spoken_text = truncate_for_speech(spoken_text, config.AI_MAX_RESPONSE_CHARS)

            # Save chat turns to SQLite
            if session_id:
                database.save_message(session_id, "user", prompt)
                database.save_message(session_id, "model", raw_text)

            _log.info("AI response (%d chars): %s", len(spoken_text), spoken_text[:100])

            return {
                "raw_response": raw_text,
                "spoken_response": spoken_text,
                "action": action,
                "action_target": action_target,
            }

        except Exception as exc:
            _log.error("Gemini API error: %s", exc)
            return {
                "raw_response": config.AI_ERROR,
                "spoken_response": config.AI_ERROR,
                "action": None,
                "action_target": None,
            }

    def clear_session(self, session_id: str) -> bool:
        """Clear conversation history for a session."""
        return database.clear_session_messages(session_id)


# Module-level singleton
_instance: GeminiService | None = None


def get_gemini() -> GeminiService:
    global _instance
    if _instance is None:
        _instance = GeminiService()
    return _instance

