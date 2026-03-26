"""
MJ AI Assistant – Speech Recognition Service
Captures microphone input and converts it to text.
"""

import speech_recognition as sr

import config
from core.logger import get_logger

_log = get_logger("services.speech")


class SpeechService:
    """Wraps speech_recognition.Recognizer for microphone capture."""

    def __init__(self):
        self._recognizer = sr.Recognizer()
        self._recognizer.dynamic_energy_threshold = True
        _log.info("Speech recogniser initialised")

    def listen(
        self,
        timeout: int | None = None,
        phrase_time_limit: int | None = None,
    ) -> str | None:
        """Listen to the microphone and return recognised text, or None on failure.

        Parameters
        ----------
        timeout : int, optional
            Seconds to wait for speech to start (default from config).
        phrase_time_limit : int, optional
            Max seconds of speech to capture (default from config).
        """
        timeout = timeout or config.LISTEN_TIMEOUT
        phrase_time_limit = phrase_time_limit or config.PHRASE_TIME_LIMIT

        try:
            with sr.Microphone() as source:
                _log.debug("Adjusting for ambient noise …")
                self._recognizer.adjust_for_ambient_noise(source, duration=0.5)
                _log.debug("Listening (timeout=%ds, limit=%ds) …", timeout, phrase_time_limit)
                audio = self._recognizer.listen(
                    source,
                    timeout=timeout,
                    phrase_time_limit=phrase_time_limit,
                )

            text = self._recognizer.recognize_google(audio)
            _log.info("Recognised: %s", text)
            return text

        except sr.WaitTimeoutError:
            _log.debug("Listen timed out – no speech detected")
            return None

        except sr.UnknownValueError:
            _log.debug("Speech was not understood")
            return None

        except sr.RequestError as exc:
            _log.error("Speech API request failed: %s", exc)
            return None

        except OSError as exc:
            _log.error("Microphone error: %s", exc)
            return None
