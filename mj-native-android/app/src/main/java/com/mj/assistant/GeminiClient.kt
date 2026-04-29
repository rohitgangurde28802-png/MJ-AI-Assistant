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
 * MJ AI Assistant – Gemini Client v2.2
 * Handles Gemini 2.0 Flash API with retry logic and quota fallback.
 */
class GeminiClient {

    companion object {
        private const val TAG = "MJ.Gemini"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val model = "gemini-2.0-flash"
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    // Conversation history for context
    private val history = mutableListOf<JSONObject>()

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
     * Send a message and get a response with retry logic.
     */
    fun fetchResponse(userText: String, callback: (String) -> Unit) {
        sendWithRetry(userText, callback, retryCount = 0)
    }

    private fun sendWithRetry(userText: String, callback: (String) -> Unit, retryCount: Int) {
        val body = buildRequestBody(userText)
        val request = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        Log.d(TAG, "Sending to Gemini (attempt ${retryCount + 1}): $userText")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network error: ${e.message}")
                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS)
                    sendWithRetry(userText, callback, retryCount + 1)
                } else {
                    callback(getFallbackResponse(userText))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()

                when {
                    code == 200 -> {
                        val text = parseGeminiResponse(body)
                        if (text != null) {
                            // Add to history for context
                            addToHistory("user", userText)
                            addToHistory("model", text)
                            callback(text.trim())
                        } else {
                            callback("I received an empty response. Could you rephrase that?")
                        }
                    }
                    code == 429 -> {
                        Log.w(TAG, "Rate limited (429). Retry $retryCount/$MAX_RETRIES")
                        if (retryCount < MAX_RETRIES) {
                            Thread.sleep(RETRY_DELAY_MS * (retryCount + 1))
                            sendWithRetry(userText, callback, retryCount + 1)
                        } else {
                            // After retries, use smart fallback
                            callback(getFallbackResponse(userText))
                        }
                    }
                    code == 400 -> {
                        Log.e(TAG, "Bad request: $body")
                        history.clear() // Clear history on bad request
                        callback("Let me reset and try again. What did you need?")
                    }
                    code == 401 || code == 403 -> {
                        Log.e(TAG, "Auth error: $code")
                        callback("I'm having authentication issues right now.")
                    }
                    else -> {
                        Log.e(TAG, "Error $code: $body")
                        if (retryCount < MAX_RETRIES) {
                            Thread.sleep(RETRY_DELAY_MS)
                            sendWithRetry(userText, callback, retryCount + 1)
                        } else {
                            callback(getFallbackResponse(userText))
                        }
                    }
                }
            }
        })
    }

    private fun buildRequestBody(userText: String): JSONObject {
        val contents = JSONArray()

        // Add recent history (last 4 exchanges for context)
        val recentHistory = if (history.size > 8) history.takeLast(8) else history.toList()
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
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
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
        // Keep history bounded
        if (history.size > 20) history.removeAt(0)
    }

    /**
     * Smart fallback for common commands when AI is unavailable.
     */
    private fun getFallbackResponse(input: String): String {
        val t = input.lowercase().trim()
        return when {
            "youtube" in t   -> "[ACTION:OPEN_YOUTUBE] Opening YouTube for you!"
            "whatsapp" in t  -> "[ACTION:OPEN_WHATSAPP] Opening WhatsApp!"
            "spotify" in t || "music" in t -> "[ACTION:OPEN_SPOTIFY] Opening Spotify!"
            "instagram" in t -> "[ACTION:OPEN_INSTAGRAM] Opening Instagram!"
            "maps" in t || "navigate" in t -> "[ACTION:OPEN_MAPS] Opening Maps!"
            "camera" in t || "photo" in t  -> "[ACTION:OPEN_CAMERA] Opening camera!"
            "settings" in t  -> "[ACTION:OPEN_SETTINGS] Opening settings!"
            "weather" in t   -> "[ACTION:SEARCH_WEB:weather today] Checking the weather for you!"
            "news" in t      -> "[ACTION:SEARCH_WEB:latest news] Getting the latest news!"
            "time" in t      -> "I don't have clock access right now, but check your status bar!"
            "joke" in t      -> "Why did the Iron Man suit get a job? Because Tony Stark was tired of doing everything himself!"
            "hello" in t || "hi" in t || "hey" in t -> "Hey there! MJ at your service. What do you need?"
            else -> "[ACTION:SEARCH_WEB:${input}] Let me search that for you!"
        }
    }

    fun clearHistory() { history.clear() }
}
