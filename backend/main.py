"""
MJ AI Assistant – FastAPI Entry Point
Backend server for the MJ voice assistant Android app.
"""

import sys
import os

# Ensure backend package is importable
sys.path.insert(0, os.path.dirname(__file__))

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

import config
from handlers.chat_handler import handle_chat
from handlers.health_handler import health_check
from services.tools_service import list_tools
from services.gemini_service import get_gemini
from core.logger import get_logger

_log = get_logger("main")

app = FastAPI(
    title="MJ AI Assistant",
    description="Backend API for the MJ voice assistant",
    version="1.0.0",
)

# CORS — allow Android app from any origin
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Request / Response Models ────────────────────────────────────────────────

class ChatRequest(BaseModel):
    user_text: str
    session_id: str | None = None


class ChatResponse(BaseModel):
    session_id: str
    user_text: str
    intent: str
    spoken_response: str
    action: str | None = None
    action_target: str | None = None
    tool_result: dict | None = None


# ── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/api/health")
async def api_health():
    """Health check endpoint."""
    return await health_check()


@app.post("/api/assistant/chat", response_model=ChatResponse)
async def api_chat(req: ChatRequest):
    """Process a text chat message."""
    if not req.user_text.strip():
        raise HTTPException(status_code=400, detail="user_text is required")
    result = await handle_chat(req.user_text, req.session_id)
    return result


@app.get("/api/tools/list")
async def api_tools():
    """List available backend tools."""
    return {"tools": list_tools()}


@app.delete("/api/conversations/{session_id}")
async def api_clear_conversation(session_id: str):
    """Clear conversation history for a session."""
    gemini = get_gemini()
    deleted = gemini.clear_session(session_id)
    return {"deleted": 1 if deleted else 0}


# ── Startup ──────────────────────────────────────────────────────────────────

@app.on_event("startup")
async def on_startup():
    _log.info("=== MJ AI Assistant Backend starting ===")
    _log.info("Gemini key: %s", "SET" if config.GEMINI_API_KEY else "MISSING")
    _log.info("Model: %s", config.AI_MODEL_NAME)
    # Warm up Gemini
    get_gemini()
    _log.info("=== MJ is ONLINE ===")


if __name__ == "__main__":
    import uvicorn

    _log.info("Starting server on %s:%d", config.HOST, config.PORT)
    uvicorn.run(
        "main:app",
        host=config.HOST,
        port=config.PORT,
        reload=True,
    )
