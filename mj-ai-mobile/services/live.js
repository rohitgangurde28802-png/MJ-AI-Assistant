import axios from 'axios';

// The user did not provide an OpenWeather key, so you need to paste yours here.
const OPENWEATHER_API_KEY = 'YOUR_OPENWEATHER_API_KEY';

export const handleLiveData = async (query) => {
  const norm = query.toLowerCase();

  // If query contains weather, extract city or default to a generic one
  if (norm.includes('weather')) {
    let city = 'Mumbai'; // Default fallback
    
    // Attempt to extract city from "weather in [city]"
    if (norm.includes('in ')) {
      const parts = norm.split('in ');
      if (parts.length > 1) {
        city = parts[1].trim();
      }
    }

    if (OPENWEATHER_API_KEY === 'YOUR_OPENWEATHER_API_KEY') {
        return `I cannot check the live weather for ${city} without an OpenWeather API key. Please update services/live.js!`;
    }

    try {
      const url = `https://api.openweathermap.org/data/2.5/weather?q=${city}&appid=${OPENWEATHER_API_KEY}&units=metric`;
      const response = await axios.get(url);
      const data = response.data;
      
      const temp = Math.round(data.main.temp);
      const desc = data.weather[0].description;
      return `The current weather in ${city} is ${temp} degrees Celsius with ${desc}.`;
    } catch (error) {
      console.error("OpenWeather Error", error);
      return `I could not fetch the live weather data for ${city}.`;
    }
  }

  // Not a live data query
  return null;
};
