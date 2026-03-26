"""
MJ AI Assistant – Utility Helpers
Text normalization and response trimming for voice delivery.
"""

import re


# Common filler words to strip before intent matching
_FILLER_WORDS = {"um", "uh", "like", "you know", "so", "well", "actually", "basically", "please", "can you", "could you", "hey"}


def normalize(text: str) -> str:
    """Lowercase, collapse whitespace, and strip filler words."""
    text = text.lower().strip()
    # Remove punctuation except apostrophes (for contractions)
    text = re.sub(r"[^\w\s']", " ", text)
    # Remove filler words
    for filler in _FILLER_WORDS:
        text = re.sub(rf"\b{re.escape(filler)}\b", " ", text)
    # Collapse whitespace
    text = re.sub(r"\s+", " ", text).strip()
    return text


def truncate_for_speech(text: str, max_chars: int = 500) -> str:
    """Trim AI responses so TTS doesn't read an essay.

    Tries to cut at a sentence boundary; falls back to word boundary.
    """
    if len(text) <= max_chars:
        return text.strip()

    truncated = text[:max_chars]

    # Try to end at the last sentence boundary
    last_period = truncated.rfind(".")
    if last_period > max_chars // 2:
        return truncated[: last_period + 1].strip()

    # Fall back to word boundary
    last_space = truncated.rfind(" ")
    if last_space > 0:
        return truncated[:last_space].strip() + "..."

    return truncated.strip() + "..."
