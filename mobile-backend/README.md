# MJ AI Assistant Mobile Backend

This Python backend exposes a single AI endpoint for the mobile app.
It reuses the existing `handlers.ai_handler` and the Gemini model configuration from the desktop assistant.

## Setup

1. Install dependencies:

```bash
cd "MJ-AI-Assistant/mobile-backend"
pip install -r requirements.txt
```

2. Copy the root `.env.example` to `.env` and add your `GEMINI_API_KEY`:

```bash
cd "MJ-AI-Assistant"
copy .env.example .env
```

3. Start the backend server:

```bash
cd "MJ-AI-Assistant/mobile-backend"
python app.py
```

4. Keep the backend running while the mobile app is active.

## API

- `POST /ai`
  - Request JSON: `{ "query": "What is MJ?" }`
  - Response JSON: `{ "answer": "..." }`

### Emulator notes

- Android emulator: use `http://10.0.2.2:5000`
- iOS simulator: use `http://localhost:5000`
- Physical device: use the machine IP address on the same network
