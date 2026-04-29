"""
MJ AI Assistant – Intent Router
Classifies user commands into intents using keyword matching.
Falls back to AI_QUERY for anything not matched.
"""

from enum import Enum, auto

from core.logger import get_logger
from core.utils import normalize

_log = get_logger("core.router")


class Intent(str, Enum):
    OPEN_YOUTUBE = "open_youtube"
    OPEN_WHATSAPP = "open_whatsapp"
    OPEN_GOOGLE = "open_google"
    OPEN_SPOTIFY = "open_spotify"
    OPEN_INSTAGRAM = "open_instagram"
    OPEN_MAPS = "open_maps"
    OPEN_CAMERA = "open_camera"
    OPEN_SETTINGS = "open_settings"
    OPEN_CALENDAR = "open_calendar"
    OPEN_GMAIL = "open_gmail"
    OPEN_PHONE = "open_phone"
    SEARCH_WEB = "search_web"
    WEATHER = "weather"
    TIME = "time"
    NEWS = "news"
    EXIT = "exit"
    AI_QUERY = "ai_query"


_RULES: list[tuple[Intent, list[list[str]]]] = [
    (Intent.OPEN_YOUTUBE,   [["open", "youtube"], ["play", "youtube"], ["launch", "youtube"]]),
    (Intent.OPEN_WHATSAPP,  [["open", "whatsapp"], ["launch", "whatsapp"]]),
    (Intent.OPEN_GOOGLE,    [["open", "google"], ["open", "chrome"], ["launch", "google"]]),
    (Intent.OPEN_SPOTIFY,   [["open", "spotify"], ["play", "spotify"], ["launch", "spotify"]]),
    (Intent.OPEN_INSTAGRAM, [["open", "instagram"], ["launch", "instagram"]]),
    (Intent.OPEN_MAPS,      [["open", "maps"], ["open", "google maps"], ["navigate"]]),
    (Intent.OPEN_CAMERA,    [["open", "camera"], ["take", "photo"], ["take", "picture"]]),
    (Intent.OPEN_SETTINGS,  [["open", "settings"]]),
    (Intent.OPEN_CALENDAR,  [["open", "calendar"]]),
    (Intent.OPEN_GMAIL,     [["open", "gmail"], ["open", "email"], ["open", "mail"]]),
    (Intent.OPEN_PHONE,     [["open", "phone"], ["open", "dialer"], ["call"]]),
    (Intent.WEATHER,        [["weather"]]),
    (Intent.TIME,           [["time"], ["what time"]]),
    (Intent.NEWS,           [["news"], ["headlines"]]),
    (Intent.EXIT,           [["exit"], ["stop"], ["quit"], ["shutdown"], ["bye"]]),
]


def route(command: str) -> tuple[Intent, str]:
    """Return (intent, payload) for the given command text."""
    text = normalize(command)
    _log.info("Routing: '%s'", text)

    for intent, keyword_sets in _RULES:
        for keywords in keyword_sets:
            if all(kw in text for kw in keywords):
                _log.info("Matched intent: %s", intent.value)
                return intent, text

    _log.info("No direct match → AI_QUERY")
    return Intent.AI_QUERY, text
