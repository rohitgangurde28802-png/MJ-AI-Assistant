package com.mj.assistant

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * MJ AI Assistant – Gemini Client v3.0
 * Multi-model fallback strategy:
 *   1. gemini-2.0-flash (primary)
 *   2. gemini-1.5-flash (secondary – different quota pool)
 *   3. gemini-1.0-pro  (tertiary)
 *   4. Smart offline fallback (always works)
 *
 * On 429 quota errors, instantly switches model instead of waiting.
 * On auth/key errors, falls back to smart offline mode.
 */
class GeminiClient {

    companion object {
        private const val TAG = "MJ.Gemini"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 1500L

        // Model cascade – if one quota is hit, try the next
        private val MODELS = listOf(
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b",
            "gemini-1.0-pro"
        )
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.GEMINI_API_KEY

    // Conversation history for multi-turn context
    private val history = mutableListOf<JSONObject>()

    // Track which model index is currently working
    private var currentModelIndex = 0

    private val systemPrompt = """
You are MJ, a smart and witty personal AI assistant for Mr. Rohit — inspired by Iron Man's AI assistant.
You speak naturally like a confident female friend. Keep answers SHORT (2-3 sentences) since they're spoken aloud.

When you need to open an app or perform a device action, put the ACTION TAG at the very START:

[ACTION:OPEN_YOUTUBE] — open YouTube
[ACTION:OPEN_WHATSAPP] — open WhatsApp  
[ACTION:OPEN_SPOTIFY] — open Spotify
[ACTION:OPEN_INSTAGRAM] — open Instagram
[ACTION:OPEN_GMAIL] — open Gmail
[ACTION:OPEN_MAPS] — open Google Maps
[ACTION:OPEN_CAMERA] — open Camera
[ACTION:OPEN_SETTINGS] — open Settings
[ACTION:OPEN_PHONE] — open Phone
[ACTION:OPEN_CALENDAR] — open Calendar
[ACTION:OPEN_GOOGLE] — open Chrome/Google
[ACTION:OPEN_FACEBOOK] — open Facebook
[ACTION:OPEN_NETFLIX] — open Netflix
[ACTION:SEARCH_WEB:your query] — search Google
[ACTION:PLAY_MUSIC:song name] — play on Spotify

Examples:
- "open youtube" → [ACTION:OPEN_YOUTUBE] Opening YouTube for you!
- "search cats" → [ACTION:SEARCH_WEB:cats] Searching for cats!
- "play Shape of You" → [ACTION:PLAY_MUSIC:Shape of You] Playing that for you!
- "tell me a joke" → (no action tag) Why don't scientists trust atoms? Because they make up everything!

ALWAYS include a short spoken response AFTER the action tag.
""".trimIndent()

    /**
     * Main entry point — send a message and get a response.
     */
    fun fetchResponse(userText: String, callback: (String) -> Unit) {
        // Quick check: if API key is clearly invalid, go to offline fallback immediately
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
            Log.w(TAG, "API key not configured — using offline fallback")
            callback(getOfflineFallback(userText))
            return
        }
        attemptWithModel(userText, callback, modelIndex = currentModelIndex, retryCount = 0)
    }

    private fun attemptWithModel(
        userText: String,
        callback: (String) -> Unit,
        modelIndex: Int,
        retryCount: Int
    ) {
        if (modelIndex >= MODELS.size) {
            // All models exhausted — use smart offline fallback
            Log.w(TAG, "All Gemini models exhausted — offline fallback")
            callback(getOfflineFallback(userText))
            return
        }

        val model = MODELS[modelIndex]
        val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"
        val bodyJson = buildRequestBody(userText)

        val request = Request.Builder()
            .url(endpoint)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        Log.d(TAG, "→ Sending to $model (attempt ${retryCount + 1}): ${userText.take(80)}")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network error on $model: ${e.message}")
                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS)
                    attemptWithModel(userText, callback, modelIndex, retryCount + 1)
                } else {
                    // Network failed — try next model
                    attemptWithModel(userText, callback, modelIndex + 1, 0)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val bodyStr = response.body?.string()

                when {
                    code == 200 -> {
                        val text = parseGeminiResponse(bodyStr)
                        if (text != null) {
                            currentModelIndex = modelIndex // Remember working model
                            addToHistory("user", userText)
                            addToHistory("model", text)
                            Log.d(TAG, "← Response from $model: ${text.take(100)}")
                            callback(text.trim())
                        } else {
                            // Empty/malformed response — try next model
                            Log.w(TAG, "Empty response from $model")
                            attemptWithModel(userText, callback, modelIndex + 1, 0)
                        }
                    }

                    code == 429 -> {
                        // Quota exceeded — immediately try next model (no point waiting)
                        Log.w(TAG, "Quota exceeded on $model → trying next model")
                        attemptWithModel(userText, callback, modelIndex + 1, 0)
                    }

                    code == 400 -> {
                        // Bad request — clear history and retry same model once
                        Log.e(TAG, "Bad request on $model: ${bodyStr?.take(200)}")
                        history.clear()
                        if (retryCount < 1) {
                            attemptWithModel(userText, callback, modelIndex, retryCount + 1)
                        } else {
                            attemptWithModel(userText, callback, modelIndex + 1, 0)
                        }
                    }

                    code == 401 || code == 403 -> {
                        // Auth error — API key problem, go offline
                        Log.e(TAG, "Auth error ($code) — API key may be invalid")
                        callback(getOfflineFallback(userText))
                    }

                    code >= 500 -> {
                        // Server error — retry with backoff, then next model
                        if (retryCount < MAX_RETRIES) {
                            Log.w(TAG, "Server error $code on $model — retry ${retryCount + 1}")
                            Thread.sleep(RETRY_DELAY_MS * (retryCount + 1))
                            attemptWithModel(userText, callback, modelIndex, retryCount + 1)
                        } else {
                            attemptWithModel(userText, callback, modelIndex + 1, 0)
                        }
                    }

                    else -> {
                        Log.e(TAG, "Unexpected $code on $model: ${bodyStr?.take(200)}")
                        if (retryCount < MAX_RETRIES) {
                            Thread.sleep(RETRY_DELAY_MS)
                            attemptWithModel(userText, callback, modelIndex, retryCount + 1)
                        } else {
                            attemptWithModel(userText, callback, modelIndex + 1, 0)
                        }
                    }
                }
            }
        })
    }

    private fun buildRequestBody(userText: String): JSONObject {
        val contents = JSONArray()

        // Include recent history (last 6 exchanges = 12 messages)
        val recentHistory = if (history.size > 12) history.takeLast(12) else history.toList()
        recentHistory.forEach { contents.put(it) }

        // Add current user message
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userText)))
        })

        return JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.8)
                put("maxOutputTokens", 300)
                put("topP", 0.95)
            })
        }
    }

    private fun parseGeminiResponse(body: String?): String? {
        return try {
            val json = JSONObject(body ?: return null)

            // Check for API-level error
            if (json.has("error")) {
                val err = json.getJSONObject("error")
                Log.e(TAG, "API error: ${err.optString("message")}")
                return null
            }

            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val candidate = candidates.getJSONObject(0)

            // Check finish reason
            val finishReason = candidate.optString("finishReason", "")
            if (finishReason == "SAFETY" || finishReason == "RECITATION") {
                Log.w(TAG, "Response blocked: $finishReason")
                return "I can't respond to that one, but I'm here for anything else!"
            }

            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            parts.getJSONObject(0).optString("text")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            null
        }
    }

    private fun addToHistory(role: String, text: String) {
        history.add(JSONObject().apply {
            put("role", role)
            put("parts", JSONArray().put(JSONObject().put("text", text)))
        })
        // Keep history bounded to last 20 messages
        while (history.size > 20) history.removeAt(0)
    }

    /**
     * Smart offline fallback — handles common commands without AI.
     * This is the last resort when ALL Gemini models are unavailable.
     */
    private fun getOfflineFallback(input: String): String {
        val t = input.lowercase().trim()
        return when {
            // App launches
            "youtube" in t         -> "[ACTION:OPEN_YOUTUBE] Opening YouTube for you, Mr. Rohit!"
            "whatsapp" in t        -> "[ACTION:OPEN_WHATSAPP] Opening WhatsApp!"
            "spotify" in t || "music" in t -> "[ACTION:OPEN_SPOTIFY] Let's get some music going!"
            "instagram" in t       -> "[ACTION:OPEN_INSTAGRAM] Opening Instagram!"
            "maps" in t || "navigate" in t || "direction" in t -> "[ACTION:OPEN_MAPS] Opening Maps — where are we heading?"
            "camera" in t || "photo" in t || "selfie" in t -> "[ACTION:OPEN_CAMERA] Say cheese!"
            "settings" in t        -> "[ACTION:OPEN_SETTINGS] Opening device settings."
            "gmail" in t || "email" in t -> "[ACTION:OPEN_GMAIL] Opening Gmail!"
            "calendar" in t || "schedule" in t -> "[ACTION:OPEN_CALENDAR] Opening your calendar!"
            "netflix" in t         -> "[ACTION:OPEN_NETFLIX] Grab some popcorn — opening Netflix!"
            "facebook" in t        -> "[ACTION:OPEN_FACEBOOK] Opening Facebook!"
            "phone" in t || "call" in t || "dial" in t -> "[ACTION:OPEN_PHONE] Opening the dialer!"

            // Web searches
            "weather" in t         -> "[ACTION:SEARCH_WEB:weather today ${extractCity(t)}] Checking the weather for you!"
            "news" in t            -> "[ACTION:SEARCH_WEB:latest news today] Fetching the latest headlines!"
            "cricket" in t         -> "[ACTION:SEARCH_WEB:cricket score today] Getting the cricket scores!"
            "stock" in t || "market" in t -> "[ACTION:SEARCH_WEB:stock market today] Checking the markets!"

            // Time / Date
            "time" in t            -> "Check the top of your screen for the time, Mr. Rohit!"
            "date" in t || "today" in t -> "Check your status bar for today's date!"

            // Greetings
            "hello" in t || "hi" in t || "hey" in t || "sup" in t ->
                "Hey, Mr. Rohit! MJ at your service. What do you need?"
            "how are you" in t || "how r u" in t ->
                "Systems are green and I'm ready to help! What can I do for you?"

            // Fun
            "joke" in t ->
                "Why did Tony Stark build an AI assistant? Because even billionaires need a second opinion!"
            "sing" in t || "song" in t ->
                "[ACTION:OPEN_SPOTIFY] I'll let the experts handle the singing — opening Spotify!"

            // Default: search for it
            else -> "[ACTION:SEARCH_WEB:${input.take(100)}] Let me search that for you, Mr. Rohit!"
        }
    }

    /** Extract a city name from a weather query if present. */
    private fun extractCity(text: String): String {
        val keywords = listOf("in ", "at ", "for ")
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx >= 0) {
                val city = text.substring(idx + kw.length).trim()
                if (city.isNotBlank() && city != "weather") return city
            }
        }
        return ""
    }

    fun clearHistory() {
        history.clear()
        currentModelIndex = 0 // Reset model to primary on clear
    }
}
