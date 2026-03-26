"""
MJ AI Assistant – Web Handler
Opens common websites in the default browser.
"""

import webbrowser

import config
from core.logger import get_logger

_log = get_logger("handlers.web")


def open_youtube() -> bool:
    """Open YouTube in the default browser."""
    return _open("YouTube", config.URLS["youtube"])


def open_whatsapp() -> bool:
    """Open WhatsApp Web in the default browser."""
    return _open("WhatsApp Web", config.URLS["whatsapp"])


def open_google() -> bool:
    """Open Google in the default browser."""
    return _open("Google", config.URLS["google"])


def _open(name: str, url: str) -> bool:
    """Helper: open *url* and return success flag."""
    try:
        _log.info("Opening %s → %s", name, url)
        webbrowser.open(url)
        return True
    except Exception as exc:
        _log.error("Failed to open %s: %s", name, exc)
        return False
