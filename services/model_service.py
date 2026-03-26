"""
MJ AI Assistant – Model Service
Provider abstraction for AI conversation (Google Gemini).
"""

import google.generativeai as genai

import config
from core.logger import get_logger

_log = get_logger("services.model")


class ModelService:
    """Wraps the Google Generative AI SDK."""

    def __init__(self):
        if not config.GEMINI_API_KEY:
            _log.warning("GEMINI_API_KEY is not set – AI queries will fail")
        genai.configure(api_key=config.GEMINI_API_KEY)
        self._model = genai.GenerativeModel(
            model_name=config.AI_MODEL_NAME,
            system_instruction=config.AI_SYSTEM_PROMPT,
        )
        _log.info("AI model ready: %s", config.AI_MODEL_NAME)

    def ask(self, prompt: str) -> str:
        """Send *prompt* to the model and return the response text.

        Returns a user-friendly error string on failure.
        """
        try:
            _log.debug("AI prompt: %s", prompt[:120])
            response = self._model.generate_content(prompt)
            answer = response.text.strip()
            _log.debug("AI response (%d chars): %s …", len(answer), answer[:100])
            return answer

        except Exception as exc:
            _log.error("AI API error: %s", exc)
            return config.AI_ERROR
