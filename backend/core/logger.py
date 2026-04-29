"""
MJ AI Assistant – Logger
Configures Python logging for the backend.
"""

import logging
import sys

import config


def get_logger(name: str) -> logging.Logger:
    """Return a named logger with console handler."""
    logger = logging.getLogger(name)

    if logger.handlers:
        return logger

    logger.setLevel(getattr(logging, config.LOG_LEVEL, logging.INFO))

    console = logging.StreamHandler(sys.stdout)
    console.setLevel(logging.DEBUG)
    fmt = logging.Formatter(
        "%(asctime)s | %(name)-22s | %(levelname)-7s | %(message)s",
        datefmt="%H:%M:%S",
    )
    console.setFormatter(fmt)
    logger.addHandler(console)

    return logger
