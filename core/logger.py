"""
MJ AI Assistant – Logger
Configures Python logging with file + console handlers.
"""

import logging
import os

import config


def get_logger(name: str) -> logging.Logger:
    """Return a named logger with file and console handlers."""
    logger = logging.getLogger(name)

    if logger.handlers:
        return logger  # already configured

    logger.setLevel(logging.DEBUG)

    # ── Console handler (INFO+) ──────────────────────────────────────────
    console = logging.StreamHandler()
    console.setLevel(logging.INFO)
    console.setFormatter(logging.Formatter(config.LOG_FORMAT, config.LOG_DATE_FORMAT))
    logger.addHandler(console)

    # ── File handler (DEBUG+) ────────────────────────────────────────────
    log_dir = os.path.dirname(config.LOG_FILE)
    if log_dir:
        os.makedirs(log_dir, exist_ok=True)

    file_handler = logging.FileHandler(config.LOG_FILE, encoding="utf-8")
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(logging.Formatter(config.LOG_FORMAT, config.LOG_DATE_FORMAT))
    logger.addHandler(file_handler)

    return logger
