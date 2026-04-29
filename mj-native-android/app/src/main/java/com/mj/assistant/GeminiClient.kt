package com.mj.assistant

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * MJ AI Assistant – Gemini Client
 * Communicates with Google Gemini API for natural language understanding.
 * API key loaded from BuildConfig.
 */
class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val model = "gemini-2.0-flash"
    private val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    private val systemPrompt = """
        You are MJ, a brilliant and witty personal AI assistant — inspired by the AI assistants from Iron Man, but female and with your own unique personality.
        You speak naturally, like a helpful female friend who is sharp, warm, and confident.
        Keep your answers brief and conversational since they will be spoken aloud on an Android phone.
        Aim for 2-3 sentences unless the user asks for more detail.
        You assist Mr. Rohit with everything — answering questions, opening apps, searching the web, giving weather updates, telling jokes, and managing tasks.

        When you need to perform a device action, include an ACTION tag at the START of your response:
        - [ACTION:OPEN_YOUTUBE] — open YouTube
        - [ACTION:OPEN_WHATSAPP] — open WhatsApp
        - [ACTION:OPEN_GOOGLE] — open Google/Chrome
        - [ACTION:OPEN_SPOTIFY] — open Spotify
        - [ACTION:OPEN_INSTAGRAM] — open Instagram
        - [ACTION:OPEN_MAPS] — open Google Maps
        - [ACTION:OPEN_CAMERA] — open Camera
        - [ACTION:OPEN_SETTINGS] — open device Settings
        - [ACTION:OPEN_CALENDAR] — open Calendar
        - [ACTION:OPEN_GMAIL] — open Gmail
        - [ACTION:OPEN_PHONE] — open Phone dialer
        - [ACTION:SEARCH_WEB:query] — search Google for something
        - [ACTION:PLAY_MUSIC:song] — play music on Spotify

        Example: If asked to open YouTube, reply: '[ACTION:OPEN_YOUTUBE] Sure thing, opening YouTube for you!'
        After the ACTION tag, always include a short friendly spoken response.
        If no action is needed, just answer naturally without any ACTION tag.
    """.trimIndent()

    /**
     * Send a prompt to Gemini and return the response via callback.
     * Callback receives the raw response text (may include ACTION tags).
     */
    fun fetchResponse(prompt: String, callback: (String) -> Unit) {
        val json = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("system_instruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("I'm having trouble connecting to my servers right now. Please check your internet connection.")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val code = response.code
                    val errMsg = when (code) {
                        429 -> "I've hit my thinking limit for now. Please try again in a moment."
                        401, 403 -> "There's an authentication issue with my AI connection."
                        else -> "Sorry, I encountered an error (code $code)."
                    }
                    callback(errMsg)
                    return
                }

                val responseData = response.body?.string()
                if (responseData != null) {
                    try {
                        val jsonObj = JSONObject(responseData)
                        val candidates = jsonObj.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                val text = parts.getJSONObject(0).getString("text")
                                callback(text.trim())
                                return
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callback("I couldn't process that response. Please try again.")
            }
        })
    }
}
