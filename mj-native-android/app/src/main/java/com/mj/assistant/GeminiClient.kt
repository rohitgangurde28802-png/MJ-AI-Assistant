package com.mj.assistant

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class GeminiClient {
    private val client = OkHttpClient()
    // Using the key fetched from previous React Native code
    private val apiKey = "AIzaSyAtvTzzun1ZDmlYmqfHGq-prfxK1KDacJo"
    private val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

    fun fetchResponse(prompt: String, callback: (String) -> Unit) {
        val json = """
            {
              "contents": [
                {
                  "parts": [{"text": "$prompt"}]
                }
              ],
              "systemInstruction": {
                "parts": [{"text": "You are MJ, a helpful and concise voice assistant. Your goal is to help Mr. Rohit with tasks on his Android phone. 

                When you need to perform an action, prepend your response with one of these codes:
                - [ACTION:OPEN_YOUTUBE] for opening YouTube
                - [ACTION:OPEN_GOOGLE] for opening Google
                - [ACTION:OPEN_WHATSAPP] for opening WhatsApp
                - [ACTION:SEARCH_WEB:query] for searching something on Google

                Example: If asked to open YouTube, reply: '[ACTION:OPEN_YOUTUBE] Sure, opening YouTube for you.'
                Keep spoken responses under 2 sentences."}]
              }
            }
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("I am having trouble connecting to my AI brain.")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback("Sorry, I encountered an error connecting to my servers.")
                    return
                }

                val responseData = response.body?.string()
                if (responseData != null) {
                    try {
                        val jsonObject = JSONObject(responseData)
                        val candidates = jsonObject.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                val text = parts.getJSONObject(0).getString("text")
                                callback(text)
                                return
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callback("I couldn't process an answer.")
            }
        })
    }
}
