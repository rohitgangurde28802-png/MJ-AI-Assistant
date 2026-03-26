"""
MJ AI Assistant – Entry Point
Starts the assistant loop: listen → wake → command → route → act → speak → repeat.
"""

import sys

import config
from core import speaker, listener
from core.router import route, Intent
from core.logger import get_logger
from handlers import web_handler, weather_handler, ai_handler

_log = get_logger("main")


# ── Intent → action mapping ─────────────────────────────────────────────────

def _handle(intent: Intent, payload: str) -> None:
    """Dispatch *intent* to the appropriate handler and speak the result."""

    if intent == Intent.OPEN_YOUTUBE:
        speaker.say("Opening YouTube for you.")
        if not web_handler.open_youtube():
            speaker.say(config.BROWSER_ERROR)

    elif intent == Intent.OPEN_WHATSAPP:
        speaker.say("Opening WhatsApp Web.")
        if not web_handler.open_whatsapp():
            speaker.say(config.BROWSER_ERROR)

    elif intent == Intent.OPEN_GOOGLE:
        speaker.say("Opening Google.")
        if not web_handler.open_google():
            speaker.say(config.BROWSER_ERROR)

    elif intent == Intent.WEATHER_SEARCH:
        speaker.say("Let me search the weather for you.")
        if not weather_handler.search_weather(payload):
            speaker.say(config.BROWSER_ERROR)

    elif intent == Intent.AI_QUERY:
        speaker.say("Let me think about that.")
        answer = ai_handler.handle_query(payload)
        speaker.say(answer)

    elif intent == Intent.EXIT:
        speaker.say(config.GOODBYE)
        _log.info("User requested exit")
        sys.exit(0)


# ── Main loop ────────────────────────────────────────────────────────────────

def main() -> None:
    """Run the assistant."""
    _log.info("=== MJ AI Assistant starting ===")
    speaker.say(config.GREETING)

    while True:
        try:
            # 1. Passive listen for wake word
            if not listener.wait_for_wake_word():
                continue  # didn't hear "MJ", keep listening

            # 2. Acknowledge activation
            speaker.say(config.ACKNOWLEDGE)

            # 3. Capture the command
            command = listener.get_command(retries=1)
            if not command:
                speaker.say(config.ASK_REPEAT)
                continue

            # 4. Route and execute
            intent, payload = route(command)
            _handle(intent, payload)

        except KeyboardInterrupt:
            _log.info("Keyboard interrupt received")
            speaker.say(config.GOODBYE)
            break

        except Exception as exc:
            _log.exception("Unexpected error: %s", exc)
            try:
                speaker.say(config.FALLBACK_ERROR)
            except Exception:
                pass  # TTS itself may have failed

    _log.info("=== MJ AI Assistant stopped ===")


if __name__ == "__main__":
    main()
