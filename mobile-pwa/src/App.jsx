import { useState, useEffect, useRef } from 'react';

const WAKE_PHRASE = 'mj';
const RULES = [
  { intent: 'OPEN_YOUTUBE', keywords: [['open', 'youtube'], ['youtube', 'start'], ['launch', 'youtube']] },
  { intent: 'OPEN_WHATSAPP', keywords: [['open', 'whatsapp'], ['whatsapp', 'start'], ['launch', 'whatsapp']] },
  { intent: 'OPEN_GOOGLE', keywords: [['open', 'google'], ['google', 'start'], ['launch', 'google']] },
  { intent: 'WEATHER_SEARCH', keywords: [['weather']] },
  { intent: 'EXIT', keywords: [['exit'], ['stop'], ['quit'], ['shutdown'], ['bye']] },
];

function normalize(text) {
  return text.toLowerCase().replace(/[^\w\s']/g, ' ').replace(/\s+/g, ' ').trim();
}

function routeCommand(command) {
  const text = normalize(command);
  for (const rule of RULES) {
    for (const keywords of rule.keywords) {
      if (keywords.every((word) => text.includes(word))) {
        return { intent: rule.intent, payload: text };
      }
    }
  }
  return { intent: 'AI_QUERY', payload: text };
}

export default function App() {
  const [status, setStatus] = useState('Idle');
  const [transcript, setTranscript] = useState('');
  const [activated, setActivated] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [backendUrl, setBackendUrl] = useState(`http://${window.location.hostname}:5000`);
  const [logs, setLogs] = useState([]);
  
  const synthRef = useRef(window.speechSynthesis);
  const recognitionRef = useRef(null);

  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      recognitionRef.current = new SpeechRecognition();
      recognitionRef.current.continuous = true;
      recognitionRef.current.interimResults = false;
      recognitionRef.current.lang = 'en-US';

      recognitionRef.current.onresult = (event) => {
        const current = event.resultIndex;
        const phrase = event.results[current][0].transcript;
        handleSpeech(phrase);
      };

      recognitionRef.current.onerror = (e) => {
        addLog(`Speech Error: ${e.error}`);
        if(e.error === 'not-allowed') setIsListening(false);
      };
      
      recognitionRef.current.onend = () => {
        // Restart listening if we are supposed to be active
        if(isListeningRef.current) {
           try { recognitionRef.current.start(); } catch(e){}
        }
      }
    } else {
      setStatus('Speech Recognition not supported in this browser.');
    }
    
    return () => {
       if (recognitionRef.current) {
          recognitionRef.current.stop();
       }
    };
  }, []);

  const isListeningRef = useRef(isListening);
  useEffect(() => {
     isListeningRef.current = isListening;
  }, [isListening]);

  const addLog = (msg) => {
    setLogs((prev) => [msg, ...prev].slice(0, 10));
  };

  const speak = (text) => {
    return new Promise((resolve) => {
      addLog(`Assistant: ${text}`);
      setStatus('Speaking...');
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.onend = () => {
        setStatus('Listening...');
        resolve();
      };
      synthRef.current.speak(utterance);
    });
  };

  const fetchAiAnswer = async (query) => {
    try {
      setStatus('Thinking...');
      const response = await fetch(`${backendUrl}/ai`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query }),
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      return data.answer;
    } catch (error) {
      addLog(`Backend Error: ${error.message}`);
      return 'I am currently unable to reach the backend server.';
    }
  };

  const executeCommand = async (text) => {
    const commandText = normalize(text);
    const { intent, payload } = routeCommand(commandText);
    addLog(`Command: ${intent}`);

    // If an action opens a URL, we attempt to open it in a new tab
    if (intent === 'OPEN_YOUTUBE') {
      await speak('Opening YouTube.');
      window.open('https://www.youtube.com', '_blank');
    } else if (intent === 'OPEN_WHATSAPP') {
      await speak('Opening WhatsApp Web.');
      window.open('https://web.whatsapp.com', '_blank');
    } else if (intent === 'OPEN_GOOGLE') {
      await speak('Opening Google.');
      window.open('https://www.google.com', '_blank');
    } else if (intent === 'WEATHER_SEARCH') {
      const q = payload.replace('weather', '').trim() || 'weather';
      await speak('Searching weather.');
      window.open(`https://www.google.com/search?q=weather+${encodeURIComponent(q)}`, '_blank');
    } else if (intent === 'EXIT') {
      await speak('Goodbye!');
      toggleListen(); // turn off
    } else {
      await speak('Let me think...');
      const answer = await fetchAiAnswer(payload);
      await speak(answer);
    }
    
    setActivated(false);
  };

  const handleSpeech = async (phrase) => {
    setTranscript(phrase);
    addLog(`Heard: "${phrase}"`);
    
    const norm = normalize(phrase);
    if (!activated) {
      if (norm.includes(WAKE_PHRASE)) {
        setActivated(true);
        synthRef.current.cancel(); // Stop current speaking
        await speak("Yes, I'm listening.");
        
        const remainder = norm.replace(WAKE_PHRASE, '').trim();
        if (remainder.length > 3) {
          executeCommand(remainder);
        }
      }
    } else {
      executeCommand(phrase);
    }
  };

  const toggleListen = () => {
    if (isListening) {
      setIsListening(false);
      setStatus('Idle');
      if(recognitionRef.current) recognitionRef.current.stop();
    } else {
      setIsListening(true);
      setStatus('Listening... Say "MJ" to wake up.');
      try {
        recognitionRef.current.start();
      } catch(e){}
    }
  };

  return (
    <div className="container">
      <header className="header">
        <h1>MJ Assistant</h1>
        <div className={`status-indicator ${activated ? 'active' : ''} ${isListening ? 'listening' : ''}`}></div>
      </header>

      <main className="main-content">
        <div className="orb-container">
          <div className={`orb ${isListening ? 'pulse' : ''} ${activated ? 'glow' : ''}`} onClick={toggleListen}>
            <span className="material-icon">🎤</span>
          </div>
          <p className="status-text">{status}</p>
        </div>

        <div className="transcript-box">
          <h3>Latest Transcript</h3>
          <p className="transcript">{transcript || 'No speech detected yet.'}</p>
        </div>

        <div className="settings-box">
          <h3>Backend Server</h3>
          <input 
            type="text" 
            value={backendUrl}
            onChange={(e) => setBackendUrl(e.target.value)}
            className="modern-input"
          />
        </div>

        <div className="log-box">
          <h3>System Logs</h3>
          <div className="logs">
            {logs.map((log, i) => (
              <div key={i} className="log-entry">{log}</div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}
