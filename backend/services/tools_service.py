"""
MJ AI Assistant – Tools Service
Registry of backend tools (time, weather, news, web search).
"""

import datetime

from core.logger import get_logger

_log = get_logger("services.tools")


def get_current_time() -> dict:
    """Return current date and time."""
    now = datetime.datetime.now()
    return {
        "tool": "time",
        "result": {
            "time": now.strftime("%I:%M %p"),
            "date": now.strftime("%A, %B %d, %Y"),
            "spoken": f"It's {now.strftime('%I:%M %p')} on {now.strftime('%A, %B %d')}.",
        },
    }


def get_weather(location: str = "") -> dict:
    """Return weather search suggestion (no API key needed, uses Google)."""
    query = f"{location} weather" if location else "weather today"
    return {
        "tool": "weather",
        "result": {
            "query": query,
            "spoken": f"Let me search the weather for {location or 'your area'}.",
            "search_url": f"https://www.google.com/search?q={query.replace(' ', '+')}",
        },
    }


def get_news() -> dict:
    """Return news search suggestion."""
    return {
        "tool": "news",
        "result": {
            "spoken": "Here are the latest headlines. Let me search that for you.",
            "search_url": "https://news.google.com",
        },
    }


def web_search(query: str) -> dict:
    """Return a web search URL."""
    return {
        "tool": "web_search",
        "result": {
            "query": query,
            "spoken": f"Searching the web for {query}.",
            "search_url": f"https://www.google.com/search?q={query.replace(' ', '+')}",
        },
    }


# Tool registry
TOOLS = {
    "time": {
        "name": "time",
        "description": "Get the current date and time",
        "handler": get_current_time,
    },
    "weather": {
        "name": "weather",
        "description": "Get weather information for a location",
        "handler": get_weather,
    },
    "news": {
        "name": "news",
        "description": "Get latest news headlines",
        "handler": get_news,
    },
    "web_search": {
        "name": "web_search",
        "description": "Search the web for information",
        "handler": web_search,
    },
}


def list_tools() -> list[dict]:
    """Return list of available tools."""
    return [{"name": t["name"], "description": t["description"]} for t in TOOLS.values()]


def execute_tool(tool_name: str, **kwargs) -> dict | None:
    """Execute a tool by name."""
    tool = TOOLS.get(tool_name)
    if not tool:
        _log.warning("Unknown tool: %s", tool_name)
        return None
    try:
        result = tool["handler"](**kwargs)
        _log.info("Tool executed: %s", tool_name)
        return result
    except Exception as exc:
        _log.error("Tool %s failed: %s", tool_name, exc)
        return None
