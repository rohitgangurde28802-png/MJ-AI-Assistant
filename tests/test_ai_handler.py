"""Tests for handlers.ai_handler – AI query handling."""

from unittest.mock import patch, MagicMock

from handlers import ai_handler
import config


class TestHandleQuery:
    @patch("handlers.ai_handler._get_model")
    def test_returns_trimmed_answer(self, mock_get_model):
        mock_model = MagicMock()
        mock_model.ask.return_value = "Python is a programming language."
        mock_get_model.return_value = mock_model

        result = ai_handler.handle_query("What is Python?")
        assert result == "Python is a programming language."
        mock_model.ask.assert_called_once_with("What is Python?")

    @patch("handlers.ai_handler._get_model")
    def test_truncates_long_answer(self, mock_get_model):
        mock_model = MagicMock()
        # Generate a response longer than AI_MAX_RESPONSE_CHARS
        long_text = "This is a sentence. " * 100
        mock_model.ask.return_value = long_text
        mock_get_model.return_value = mock_model

        result = ai_handler.handle_query("Tell me everything")
        assert len(result) <= config.AI_MAX_RESPONSE_CHARS + 10  # small margin for "..."

    @patch("handlers.ai_handler._get_model")
    def test_handles_api_error(self, mock_get_model):
        mock_model = MagicMock()
        mock_model.ask.return_value = config.AI_ERROR
        mock_get_model.return_value = mock_model

        result = ai_handler.handle_query("broken query")
        assert result == config.AI_ERROR
