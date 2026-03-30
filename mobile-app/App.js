import React, { useEffect, useState } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  Button,
  Linking,
  ScrollView,
  Platform,
  TextInput,
} from 'react-native';
import Voice from '@react-native-voice/voice';
import Tts from 'react-native-tts';

const BACKEND_URL = Platform.select({
  android: 'http://10.0.2.2:5000',
  ios: 'http://localhost:5000',
  default: 'http://10.0.2.2:5000',
});
const WAKE_PHRASE = 'mj';

// If you run on a physical device, replace BACKEND_URL with your PC's local IP, e.g.
// 'http://192.168.1.100:5000'

const RULES = [
  { intent: 'OPEN_YOUTUBE', keywords: [['open', 'youtube'], ['youtube', 'start'], ['launch', 'youtube']] },
  { intent: 'OPEN_WHATSAPP', keywords: [['open', 'whatsapp'], ['whatsapp', 'start'], ['launch', 'whatsapp']] },
  { intent: 'OPEN_GOOGLE', keywords: [['open', 'google'], ['google', 'start'], ['launch', 'google']] },
  { intent: 'WEATHER_SEARCH', keywords: [['weather']] },
  { intent: 'EXIT', keywords: [['exit'], ['stop'], ['quit'], ['shutdown'], ['shut down'], ['bye']] },
];

function normalize(text) {
  return text
    .toLowerCase()
    .replace(/[^\w\s']/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function routeCommand(command) {
  const text = normalize(command);
  for (const rule of RULES) {
    for (const keywords of rule.keywords) {
      if (keywords.every((keyword) => text.includes(keyword))) {
        return { intent: rule.intent, payload: text };
      }
    }
  }
  return { intent: 'AI_QUERY', payload: text };
}

export default function App() {
  const [status, setStatus] = useState('Ready');
  const [transcript, setTranscript] = useState('');
  const [activated, setActivated] = useState(false);
  const [log, setLog] = useState([]);
  const [backendUrl, setBackendUrl] = useState(BACKEND_URL);

  useEffect(() => {
    Tts.getInitStatus()
      .then(() => Tts.setDefaultRate(0.5))
      .catch(() => {});

    Voice.onSpeechResults = onSpeechResults;
    Voice.onSpeechError = onSpeechError;

    return () => {
      Voice.destroy().then(Voice.removeAllListeners);
    };
  }, []);

  const addLog = (message) => {
    setLog((current) => [message, ...current].slice(0, 20));
  };

  const speak = async (message) => {
    setStatus(message);
    addLog(`TTS: ${message}`);
    try {
      Tts.stop();
      Tts.speak(message);
    } catch (error) {
      addLog(`TTS error: ${error.message}`);
    }
  };

  const openUrl = async (url) => {
    try {
      const supported = await Linking.canOpenURL(url);
      if (supported) {
        await Linking.openURL(url);
      } else {
        await speak('I cannot open that page.');
      }
    } catch (error) {
      await speak('I could not open the link.');
    }
  };

  const fetchAiAnswer = async (query) => {
    try {
      const response = await fetch(`${backendUrl}/ai`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query }),
      });
      if (!response.ok) {
        throw new Error(`Status ${response.status}`);
      }
      const data = await response.json();
      return data.answer || 'I could not get an answer.';
    } catch (error) {
      addLog(`AI backend error: ${error.message}`);
      return `AI backend is unavailable. Please start the mobile backend server at ${backendUrl}.`;
    }
  };

  const executeCommand = async (text) => {
    const commandText = normalize(text);
    const { intent, payload } = routeCommand(commandText);
    addLog(`Command: ${commandText} → ${intent}`);

    if (intent === 'OPEN_YOUTUBE') {
      await speak('Opening YouTube.');
      await openUrl('https://www.youtube.com');
    } else if (intent === 'OPEN_WHATSAPP') {
      await speak('Opening WhatsApp Web.');
      await openUrl('https://web.whatsapp.com');
    } else if (intent === 'OPEN_GOOGLE') {
      await speak('Opening Google.');
      await openUrl('https://www.google.com');
    } else if (intent === 'WEATHER_SEARCH') {
      const query = payload.replace('weather', '').trim() || 'weather';
      await speak('Searching weather.');
      await openUrl(`https://www.google.com/search?q=weather+${encodeURIComponent(query)}`);
    } else if (intent === 'EXIT') {
      await speak('Goodbye!');
      setActivated(false);
    } else {
      await speak('Let me think about that.');
      const answer = await fetchAiAnswer(payload);
      await speak(answer);
    }

    setActivated(false);
    setTranscript('');
  };

  const onSpeechResults = async (event) => {
    const phrase = event.value?.[0] || '';
    if (!phrase) {
      return;
    }

    setTranscript(phrase);
    addLog(`Heard: ${phrase}`);

    const normalizedPhrase = normalize(phrase);
    if (!activated) {
      if (normalizedPhrase.includes(WAKE_PHRASE)) {
        setActivated(true);
        await speak("Yes, I'm listening.");
        const remainder = normalizedPhrase.replace(WAKE_PHRASE, '').trim();
        if (remainder.length > 0) {
          await executeCommand(remainder);
        } else {
          setStatus('Listening for command...');
        }
      } else {
        setStatus('Say "MJ" first to activate me.');
      }
    } else {
      await executeCommand(phrase);
    }
  };

  const onSpeechError = (error) => {
    addLog(`Speech error: ${JSON.stringify(error)}`);
    setStatus('Speech recognition error. Try again.');
  };

  const startListening = async () => {
    try {
      setStatus('Listening... Say "MJ" to activate.');
      await Voice.start('en-US');
    } catch (error) {
      addLog(`Voice start error: ${error.message}`);
      setStatus('Could not start voice recognition.');
    }
  };

  const stopListening = async () => {
    try {
      await Voice.stop();
      setStatus('Stopped listening.');
    } catch (error) {
      addLog(`Voice stop error: ${error.message}`);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>MJ Mobile Assistant</Text>
      <Text style={styles.status}>{status}</Text>
      <View style={styles.buttonRow}>
        <View style={styles.buttonWrapper}>
          <Button title="Start Listening" onPress={startListening} />
        </View>
        <View style={styles.buttonWrapper}>
          <Button title="Stop" onPress={stopListening} />
        </View>
      </View>
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Backend URL</Text>
        <TextInput
          style={styles.input}
          value={backendUrl}
          onChangeText={setBackendUrl}
          autoCapitalize="none"
          autoCorrect={false}
          placeholder="http://10.0.2.2:5000"
          placeholderTextColor="#888"
        />
        <Text style={styles.cardText}>Current backend host used for AI queries.</Text>
      </View>
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Last transcript</Text>
        <Text style={styles.cardText}>{transcript || '...'}</Text>
      </View>
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Recent log</Text>
        <ScrollView style={styles.logBox}>
          {log.map((entry, index) => (
            <Text key={`${index}-${entry}`} style={styles.logEntry}>
              {entry}
            </Text>
          ))}
        </ScrollView>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#121212',
    padding: 16,
  },
  title: {
    color: '#fff',
    fontSize: 24,
    fontWeight: '700',
    marginBottom: 16,
  },
  status: {
    color: '#d0d0d0',
    marginBottom: 16,
  },
  buttonRow: {
    flexDirection: 'row',
    marginBottom: 16,
  },
  buttonWrapper: {
    flex: 1,
    marginRight: 8,
  },
  card: {
    backgroundColor: '#1e1e1e',
    borderRadius: 12,
    padding: 14,
    marginBottom: 12,
  },
  input: {
    backgroundColor: '#252525',
    color: '#fff',
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#333',
  },
  cardTitle: {
    color: '#fff',
    fontSize: 16,
    marginBottom: 8,
  },
  cardText: {
    color: '#ccc',
    fontSize: 14,
  },
  logBox: {
    maxHeight: 220,
  },
  logEntry: {
    color: '#aaa',
    marginBottom: 6,
  },
});
