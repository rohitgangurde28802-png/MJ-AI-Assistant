"""
MJ AI Assistant – Cache Service
In-memory LRU cache for repeated queries and session management.
"""

import time
from collections import OrderedDict

import config
from core.logger import get_logger

_log = get_logger("services.cache")


class CacheService:
    """Simple LRU cache with TTL for query responses."""

    def __init__(self, max_size: int = None, ttl: int = None):
        self._max_size = max_size or config.CACHE_MAX_SIZE
        self._ttl = ttl or config.CACHE_TTL_SECONDS
        self._cache: OrderedDict[str, tuple[float, dict]] = OrderedDict()
        _log.info("Cache ready (max=%d, ttl=%ds)", self._max_size, self._ttl)

    def get(self, key: str) -> dict | None:
        """Get cached response if exists and not expired."""
        if key not in self._cache:
            return None

        timestamp, value = self._cache[key]
        if time.time() - timestamp > self._ttl:
            del self._cache[key]
            return None

        # Move to end (most recently used)
        self._cache.move_to_end(key)
        _log.debug("Cache HIT: %s", key[:60])
        return value

    def put(self, key: str, value: dict) -> None:
        """Store a response in cache."""
        if key in self._cache:
            self._cache.move_to_end(key)
        self._cache[key] = (time.time(), value)

        # Evict oldest if over max size
        while len(self._cache) > self._max_size:
            evicted = self._cache.popitem(last=False)
            _log.debug("Cache EVICT: %s", evicted[0][:60])

    def clear(self) -> int:
        """Clear all cached entries. Returns count cleared."""
        count = len(self._cache)
        self._cache.clear()
        _log.info("Cache cleared: %d entries", count)
        return count

    @property
    def size(self) -> int:
        return len(self._cache)


# Module-level singleton
_instance: CacheService | None = None


def get_cache() -> CacheService:
    global _instance
    if _instance is None:
        _instance = CacheService()
    return _instance
