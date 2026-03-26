"""
MJ AI Assistant – Command Router
Classifies user commands into intents and dispatches to the right handler.
"""

from enum import Enum, auto

from core.utils import normalize
from core.logger import get_logger

_log = get_logger("core.router")


class Intent(Enum):
    OPEN_YOUTUBE = auto()
    OPEN_WHATSAPP = auto()
    OPEN_GOOGLE = auto()
    WEATHER_SEARCH = auto()
    EXIT = auto()
    AI_QUERY = auto()


# ── Keyword rules ────────────────────────────────────────────────────────────
# Each rule is (intent, list-of-keyword-sets).  A match occurs when ALL
# keywords in at least one set are found in the normalised text.

_RULES: list[tuple[Intent, list[list[str]]]] = [
    (Intent.OPEN_YOUTUBE,   [["open", "youtube"], ["youtube", "start"], ["launch", "youtube"]]),
    (Intent.OPEN_WHATSAPP,  [["open", "whatsapp"], ["whatsapp", "start"], ["launch", "whatsapp"]]),
    (Intent.OPEN_GOOGLE,    [["open", "google"], ["google", "start"], ["launch", "google"]]),
    (Intent.WEATHER_SEARCH, [["weather"]]),
    (Intent.EXIT,           [["exit"], ["stop"], ["quit"], ["shutdown"], ["shut down"], ["bye"]]),
]


def route(command: str) -> tuple[Intent, str]:
    """Return ``(intent, payload)`` for *command*.

    *payload* is the original (normalised) command text, which handlers
    can use to extract extra detail (e.g. city name for weather).
    """
    text = normalize(command)
    _log.info("Routing: '%s'", text)

    for intent, keyword_sets in _RULES:
        for keywords in keyword_sets:
            if all(kw in text for kw in keywords):
                _log.info("Matched intent: %s", intent.name)
                return intent, text

    _log.info("No direct match → AI_QUERY")
    return Intent.AI_QUERY, text
