"""Tests for handlers.web_handler – browser opening."""

from unittest.mock import patch

from handlers import web_handler
import config


class TestOpenYouTube:
    @patch("handlers.web_handler.webbrowser.open")
    def test_opens_correct_url(self, mock_open):
        result = web_handler.open_youtube()
        mock_open.assert_called_once_with(config.URLS["youtube"])
        assert result is True

    @patch("handlers.web_handler.webbrowser.open", side_effect=Exception("browser error"))
    def test_returns_false_on_error(self, mock_open):
        result = web_handler.open_youtube()
        assert result is False


class TestOpenWhatsApp:
    @patch("handlers.web_handler.webbrowser.open")
    def test_opens_correct_url(self, mock_open):
        result = web_handler.open_whatsapp()
        mock_open.assert_called_once_with(config.URLS["whatsapp"])
        assert result is True


class TestOpenGoogle:
    @patch("handlers.web_handler.webbrowser.open")
    def test_opens_correct_url(self, mock_open):
        result = web_handler.open_google()
        mock_open.assert_called_once_with(config.URLS["google"])
        assert result is True
