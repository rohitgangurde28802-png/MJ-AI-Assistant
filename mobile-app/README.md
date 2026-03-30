# MJ AI Assistant Mobile

This folder contains a React Native mobile frontend for the MJ assistant.
It uses native mobile speech recognition and text-to-speech, and sends AI requests to a Python backend.

## What is included

- `App.js` - mobile voice assistant UI and wake-word logic
- `package.json` - React Native dependencies

## How it works

1. Press `Start Listening`
2. Say `MJ` to activate the assistant
3. Speak a command, e.g. `Open YouTube`, `Weather in Mumbai`, or a general question
4. The app routes the command and speaks the response

## Setup

### 1. Create or update the React Native scaffold

If `mobile-app` does not already contain Android/iOS folders, generate a new project in a sibling directory and move the code in:

```bash
cd "MJ-AI-Assistant"
npx react-native init mj-ai-assistant-mobile
```

Then copy `App.js`, `index.js`, `app.json`, `babel.config.js`, and `package.json` from `mobile-app` into the generated project.

### 2. Install Node.js dependencies

```bash
cd "MJ-AI-Assistant/mobile-app"
npm install
```

### 3. Install native modules

```bash
npx pod-install ios
```

### 4. Run on Android

```bash
npx react-native run-android
```

### 5. Run on iOS

```bash
npx react-native run-ios
```

## Notes

- `@react-native-voice/voice` requires native linking and microphone permissions.
- `react-native-tts` provides text-to-speech output.
- Update `BACKEND_URL` in `App.js` to match your Python backend host if your phone is not an emulator.
- For Android emulator use `http://10.0.2.2:5000`.
- For iOS simulator use `http://localhost:5000`.
- For a physical device, use your PC's local network IP address, e.g. `http://192.168.1.100:5000`.
