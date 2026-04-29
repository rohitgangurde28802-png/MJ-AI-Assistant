"""
MJ AI Assistant – Utility Helpers
Text normalization and response trimming.
"""

import re


_FILLER_WORDS = {
    "um", "uh", "like", "you know", "so", "well",
    "actually", "basically", "please", "can you", "could you", "hey",
}


def normalize(text: str) -> str:
    """Lowercase, collapse whitespace, and strip filler words."""
    text = text.lower().strip()
    text = re.sub(r"[^\w\s']", " ", text)
    for filler in _FILLER_WORDS:
        text = re.sub(rf"\b{re.escape(filler)}\b", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def truncate_for_speech(text: str, max_chars: int = 500) -> str:
    """Trim AI responses for spoken output."""
    if len(text) <= max_chars:
        return text.strip()

    truncated = text[:max_chars]
    last_period = truncated.rfind(".")
    if last_period > max_chars // 2:
        return truncated[: last_period + 1].strip()

    last_space = truncated.rfind(" ")
    if last_space > 0:
        return truncated[:last_space].strip() + "..."

    return truncated.strip() + "..."
