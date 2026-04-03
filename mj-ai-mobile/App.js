import React, { useState, useRef, useEffect } from 'react';
import { 
  StyleSheet, 
  Text, 
  View, 
  TextInput, 
  TouchableOpacity, 
  ScrollView, 
  KeyboardAvoidingView, 
  Platform,
  ActivityIndicator,
  Keyboard
} from 'react-native';
import * as Speech from 'expo-speech';

import ChatBubble from './components/ChatBubble';
import { fetchAIResponse } from './services/ai';
import { handleLiveData } from './services/live';

export default function App() {
  const [messages, setMessages] = useState([
    { id: '1', text: "Hello! I'm MJ, your AI assistant. How can I help you today?", sender: 'ai' }
  ]);
  const [inputText, setInputText] = useState('');
  const [loading, setLoading] = useState(false);
  const scrollViewRef = useRef();

  // Speak the initial greeting
  useEffect(() => {
    Speech.speak("Hello! I'm MJ, your A I assistant. How can I help you today?", { rate: 0.9 });
  }, []);

  const handleSend = async () => {
    if (!inputText.trim()) return;

    const userMessage = { id: Date.now().toString(), text: inputText.trim(), sender: 'user' };
    setMessages(prev => [...prev, userMessage]);
    setInputText('');
    setLoading(true);
    Keyboard.dismiss();

    let responseText = '';

    // Route 1: Live Data Check
    const liveUpdate = await handleLiveData(userMessage.text);
    if (liveUpdate) {
      responseText = liveUpdate;
    } else {
      // Route 2: Gemini AI
      responseText = await fetchAIResponse(userMessage.text);
    }

    const aiMessage = { id: (Date.now() + 1).toString(), text: responseText, sender: 'ai' };
    setMessages(prev => [...prev, aiMessage]);
    setLoading(false);

    // Speak the response
    Speech.speak(responseText, { rate: 0.95 });
  };

  return (
    <KeyboardAvoidingView 
      style={styles.container} 
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.header}>
        <Text style={styles.headerTitle}>MJ Assistant</Text>
      </View>

      <ScrollView 
        ref={scrollViewRef}
        style={styles.chatContainer} 
        contentContainerStyle={styles.chatContent}
        onContentSizeChange={() => scrollViewRef.current?.scrollToEnd({ animated: true })}
      >
        {messages.map(msg => (
          <ChatBubble key={msg.id} message={msg} />
        ))}
        {loading && (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="small" color="#00ff88" />
            <Text style={styles.loadingText}>MJ is thinking...</Text>
          </View>
        )}
      </ScrollView>

      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          placeholder="Type or dictate a message..."
          placeholderTextColor="#666"
          value={inputText}
          onChangeText={setInputText}
          multiline
        />
        <TouchableOpacity 
          style={[styles.sendButton, !inputText.trim() && styles.sendButtonDisabled]} 
          onPress={handleSend}
          disabled={!inputText.trim() || loading}
        >
          <Text style={styles.sendButtonText}>Send</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f1115',
  },
  header: {
    paddingTop: 50,
    paddingBottom: 15,
    backgroundColor: '#1a1d24',
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#2a2d36',
  },
  headerTitle: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
  chatContainer: {
    flex: 1,
  },
  chatContent: {
    padding: 15,
    paddingBottom: 20,
  },
  loadingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 10,
    paddingLeft: 10,
  },
  loadingText: {
    color: '#8b8f9e',
    marginLeft: 10,
    fontStyle: 'italic',
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 12,
    backgroundColor: '#1a1d24',
    borderTopWidth: 1,
    borderTopColor: '#2a2d36',
    alignItems: 'flex-end',
  },
  input: {
    flex: 1,
    backgroundColor: '#0f1115',
    color: '#fff',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 12,
    fontSize: 16,
    maxHeight: 120,
    minHeight: 45,
  },
  sendButton: {
    backgroundColor: '#00ff88',
    borderRadius: 25,
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 10,
    height: 45,
    paddingHorizontal: 20,
  },
  sendButtonDisabled: {
    backgroundColor: '#334c41',
  },
  sendButtonText: {
    color: '#000',
    fontWeight: 'bold',
    fontSize: 16,
  },
});
