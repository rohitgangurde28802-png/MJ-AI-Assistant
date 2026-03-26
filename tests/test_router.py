"""Tests for core.router – intent classification."""

import pytest
from core.router import route, Intent


class TestRouteDirectActions:
    """Known commands should match their expected intent."""

    @pytest.mark.parametrize("command, expected_intent", [
        ("open YouTube", Intent.OPEN_YOUTUBE),
        ("Open YouTube please", Intent.OPEN_YOUTUBE),
        ("launch youtube", Intent.OPEN_YOUTUBE),
        ("start YouTube", Intent.OPEN_YOUTUBE),

        ("open WhatsApp", Intent.OPEN_WHATSAPP),
        ("Open whatsapp web", Intent.OPEN_WHATSAPP),
        ("launch whatsapp", Intent.OPEN_WHATSAPP),

        ("open Google", Intent.OPEN_GOOGLE),
        ("launch google", Intent.OPEN_GOOGLE),

        ("weather in Mumbai", Intent.WEATHER_SEARCH),
        ("What is the weather today", Intent.WEATHER_SEARCH),
        ("weather", Intent.WEATHER_SEARCH),

        ("exit", Intent.EXIT),
        ("stop", Intent.EXIT),
        ("quit", Intent.EXIT),
        ("bye", Intent.EXIT),
        ("shutdown", Intent.EXIT),
    ])
    def test_known_commands(self, command, expected_intent):
        intent, _ = route(command)
        assert intent == expected_intent


class TestRouteAIFallback:
    """Unrecognised commands should fall back to AI_QUERY."""

    @pytest.mark.parametrize("command", [
        "What is Python?",
        "Tell me a joke",
        "How far is the moon?",
        "",
        "asdf1234",
    ])
    def test_fallback(self, command):
        intent, _ = route(command)
        assert intent == Intent.AI_QUERY


class TestRoutePayload:
    """Payload should be the normalised command text."""

    def test_payload_is_normalised(self):
        _, payload = route("  Open  YOUTUBE  ")
        assert payload == "open youtube"
