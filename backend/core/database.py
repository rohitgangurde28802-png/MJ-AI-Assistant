"""
MJ AI Assistant – Database Service
SQLite persistence for chat history and tool logs.
"""

import os
import sqlite3
from datetime import datetime

# Database file in the backend root
DB_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "mj_assistant.db")


def get_db_connection():
    """Create a connection to the SQLite database."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """Initialize database tables and indexes."""
    with get_db_connection() as conn:
        cursor = conn.cursor()
        
        # 1. Chat Messages Table
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """
        )
        
        # 2. Tool Execution Logs Table
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS tool_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                tool_name TEXT NOT NULL,
                arguments TEXT,
                result TEXT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """
        )
        
        # Indexes for fast retrieval
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_messages_session ON messages(session_id)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_messages_time ON messages(timestamp)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_tool_logs_session ON tool_logs(session_id)")
        
        conn.commit()


def save_message(session_id: str, role: str, content: str):
    """Save a chat message to the database."""
    with get_db_connection() as conn:
        conn.execute(
            "INSERT INTO messages (session_id, role, content) VALUES (?, ?, ?)",
            (session_id, role, content),
        )
        conn.commit()


def get_messages(session_id: str, limit: int = 20) -> list[dict]:
    """Retrieve chat history for a session."""
    with get_db_connection() as conn:
        cursor = conn.execute(
            """
            SELECT role, content, timestamp 
            FROM messages 
            WHERE session_id = ? 
            ORDER BY timestamp ASC 
            LIMIT ?
            """,
            (session_id, limit),
        )
        return [dict(row) for row in cursor.fetchall()]


def log_tool(session_id: str, tool_name: str, arguments: str | None, result: str | None):
    """Log tool execution to database."""
    with get_db_connection() as conn:
        conn.execute(
            "INSERT INTO tool_logs (session_id, tool_name, arguments, result) VALUES (?, ?, ?, ?)",
            (session_id, tool_name, arguments, result),
        )
        conn.commit()


def get_tool_logs(session_id: str) -> list[dict]:
    """Retrieve tool execution logs for a session."""
    with get_db_connection() as conn:
        cursor = conn.execute(
            "SELECT tool_name, arguments, result, timestamp FROM tool_logs WHERE session_id = ? ORDER BY timestamp ASC",
            (session_id,),
        )
        return [dict(row) for row in cursor.fetchall()]


def clear_session_messages(session_id: str) -> bool:
    """Clear message history for a session."""
    with get_db_connection() as conn:
        cursor = conn.execute("DELETE FROM messages WHERE session_id = ?", (session_id,))
        conn.commit()
        return cursor.rowcount > 0
