import axios from 'axios';

// IMPORTANT: Do NOT expose this on a public Github repository for a production app without a backend.
// For this tutorial/demo to work completely offline server-wise, we inject it directly.
const GEMINI_API_KEY = 'AIzaSyAtvTzzun1ZDmlYmqfHGq-prfxK1KDacJo';

export const fetchAIResponse = async (prompt) => {
  try {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`;
    
    // We attempt an API call directly to Gemini
    const response = await axios.post(
      url,
      {
        contents: [
          {
            parts: [{ text: prompt }]
          }
        ],
        // Ask it to keep it short for voice synthesis
        systemInstruction: {
          parts: [{ text: "You are MJ, a helpful and concise voice assistant. Your output will be spoken out loud, so keep it under 3 sentences."}]
        }
      },
      {
        headers: { 'Content-Type': 'application/json' }
      }
    );

    const data = response.data;
    if (data.candidates && data.candidates.length > 0) {
      return data.candidates[0].content.parts[0].text;
    }
    
    return "I couldn't process an answer.";
  } catch (error) {
    console.error('Gemini API Error:', error?.response?.data || error.message);
    return 'I am having trouble connecting to my AI brain.';
  }
};
