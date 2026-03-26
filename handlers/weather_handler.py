"""
MJ AI Assistant – Weather Handler
Performs browser-based weather search (V1 strategy).
"""

import re
import webbrowser

import config
from core.logger import get_logger

_log = get_logger("handlers.weather")


def search_weather(command_text: str) -> bool:
    """Open a Google weather search for the location mentioned in *command_text*.

    If no specific location is found, searches for generic "weather today".
    """
    location = _extract_location(command_text)
    query = f"{location} weather" if location else "weather today"
    url = config.WEATHER_SEARCH_URL.format(query=query.replace(" ", "+"))

    try:
        _log.info("Weather search: %s → %s", query, url)
        webbrowser.open(url)
        return True
    except Exception as exc:
        _log.error("Weather search failed: %s", exc)
        return False


def _extract_location(text: str) -> str:
    """Try to pull a city/location from the command text."""
    # Patterns like "weather in <city>" or "weather of <city>"
    match = re.search(r"weather\s+(?:in|of|for|at)\s+(.+)", text)
    if match:
        return match.group(1).strip()
    # Fallback: anything after the word "weather"
    match = re.search(r"weather\s+(.+)", text)
    if match:
        candidate = match.group(1).strip()
        # Avoid returning filler like "today", "now"
        if candidate not in {"today", "now", "right now", "currently"}:
            return candidate
    return ""
