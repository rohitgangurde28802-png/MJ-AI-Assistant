"""
MJ AI Assistant – Gemini Service
Handles conversation with Google Gemini API, maintaining session history.
"""

import google.generativeai as genai

import config
from core.logger import get_logger
from core.utils import truncate_for_speech

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
        # Session-based conversation histories: {session_id: [messages]}
        self._sessions: dict[str, list[dict]] = {}
        _log.info("Gemini model ready: %s", config.AI_MODEL_NAME)

    def chat(self, prompt: str, session_id: str | None = None) -> dict:
        """Send prompt to Gemini, return structured response with action parsing."""
        try:
            _log.info("AI prompt [%s]: %s", session_id or "new", prompt[:120])

            # Get or create session history
            if session_id and session_id in self._sessions:
                history = self._sessions[session_id]
            else:
                history = []

            # Create chat with history
            chat = self._model.start_chat(history=history)
            response = chat.send_message(prompt)
            raw_text = response.text.strip()

            # Update session history
            if session_id:
                self._sessions[session_id] = list(chat.history)

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
        if session_id in self._sessions:
            del self._sessions[session_id]
            return True
        return False

    def get_session_count(self) -> int:
        return len(self._sessions)


# Module-level singleton
_instance: GeminiService | None = None


def get_gemini() -> GeminiService:
    global _instance
    if _instance is None:
        _instance = GeminiService()
    return _instance
