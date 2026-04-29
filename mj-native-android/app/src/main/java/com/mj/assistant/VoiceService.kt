package com.mj.assistant

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * MJ AI Assistant – Voice Service
 * Background service for wake-word detection and voice command processing.
 * Configured for female voice (MJ-style) with silent mic activation.
 */
class VoiceService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "MJ.VoiceService"
        private const val CHANNEL_ID = "mj_voice_channel"
        private const val NOTIFICATION_ID = 101
        private const val MIC_RESTART_DELAY_MS = 2000L

        // Callback to send state updates to MainActivity
        var onStateChanged: ((String, String?) -> Unit)? = null
        var onResponse: ((String, String) -> Unit)? = null  // (userText, mjResponse)
    }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var geminiClient: GeminiClient

    private var isListeningForCommand = false
    private var isSpeaking = false
    private var isDestroyed = false

    override fun onCreate() {
        super.onCreate()
        geminiClient = GeminiClient()
        tts = TextToSpeech(this, this)
        setupForegroundNotification()
        setupSpeechRecognizer()
    }

    private fun setupForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MJ Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MJ is listening in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US

            // Select female voice (MJ-style, warm and confident)
            selectFemaleVoice()

            // Set pitch and rate for a confident, warm female voice
            tts.setPitch(1.05f)    // Slightly higher pitch for female
            tts.setSpeechRate(0.95f) // Slightly slower for clarity

            Log.i(TAG, "TTS initialized with female voice preference")
            onStateChanged?.invoke("idle", null)
            startListening()
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    /**
     * Attempt to select a female voice from available TTS voices.
     */
    private fun selectFemaleVoice() {
        try {
            val voices = tts.voices ?: return
            // Priority: look for female voices, prefer English
            val femaleVoice = voices.filter { voice ->
                val name = voice.name.lowercase()
                !voice.isNetworkConnectionRequired &&
                voice.locale.language == "en" &&
                (name.contains("female") || name.contains("woman") ||
                 name.contains("en-us-x-sfg") || name.contains("en-gb-x-fis") ||
                 name.contains("en-us-x-tpf") || name.contains("en-in-x-ahp"))
            }.minByOrNull { it.quality }

            if (femaleVoice != null) {
                tts.voice = femaleVoice
                Log.i(TAG, "Selected female voice: ${femaleVoice.name}")
            } else {
                // Fallback: try any voice with higher pitch
                Log.i(TAG, "No explicit female voice found, using pitch adjustment")
                tts.setPitch(1.1f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error selecting female voice: ${e.message}")
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorName = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "TIMEOUT"
                    SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
                    SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
                    else -> "ERROR_$error"
                }
                Log.d(TAG, "Recognition error: $errorName")

                if (!isSpeaking && !isDestroyed) {
                    restartListening()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val heardText = matches[0].lowercase(Locale.getDefault())
                    Log.d(TAG, "Heard: $heardText")

                    if (!isListeningForCommand) {
                        // STATE 1: Waiting for Wake Word
                        if (heardText.contains("hey mj") ||
                            heardText.contains("am jay") ||
                            heardText.contains("emjay") ||
                            heardText.contains("mj") ||
                            heardText.contains("hey m j")) {

                            isListeningForCommand = true
                            onStateChanged?.invoke("listening", null)
                            speak("What can I do for you?")
                        } else {
                            restartListening()
                        }
                    } else {
                        // STATE 2: Processing Command
                        isListeningForCommand = false
                        onStateChanged?.invoke("thinking", heardText)

                        geminiClient.fetchResponse(heardText) { response ->
                            processGeminiResponse(heardText, response)
                        }
                    }
                } else {
                    restartListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    /**
     * Process Gemini response: extract actions and speak the clean response.
     */
    private fun processGeminiResponse(userText: String, response: String) {
        var cleanResponse = response

        // Check for ACTION codes
        if (response.contains("[ACTION:")) {
            val actionMatch = Regex("\\[ACTION:([^\\]]+)\\]").find(response)
            val fullAction = actionMatch?.groupValues?.get(1) ?: ""

            // Parse action and target
            val action: String
            val target: String?
            if (":" in fullAction) {
                val parts = fullAction.split(":", limit = 2)
                action = parts[0].trim()
                target = parts[1].trim()
            } else {
                action = fullAction.trim()
                target = null
            }

            // Execute the action
            Log.d(TAG, "Action: $action, Target: $target")
            ActionExecutor.execute(this, action, target)

            // Clean ACTION tag from spoken response
            cleanResponse = response.replace(Regex("\\[ACTION:[^\\]]+\\]\\s*"), "").trim()
        }

        // Notify UI
        onResponse?.invoke(userText, cleanResponse)
        onStateChanged?.invoke("speaking", null)

        // Speak the response
        speak(cleanResponse)
    }

    /**
     * Process a text command directly (from keyboard input).
     */
    fun processTextCommand(text: String) {
        onStateChanged?.invoke("thinking", text)
        geminiClient.fetchResponse(text) { response ->
            processGeminiResponse(text, response)
        }
    }

    private fun speak(text: String) {
        isSpeaking = true
        onStateChanged?.invoke("speaking", null)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mj_tts")

        // Poll until speech is done, then restart listening
        Thread {
            while (tts.isSpeaking) {
                Thread.sleep(100)
            }
            isSpeaking = false
            if (!isDestroyed) {
                onStateChanged?.invoke("idle", null)
                restartListening()
            }
        }.start()
    }

    /**
     * Restart listening with a delay to prevent beeping.
     */
    private fun restartListening() {
        if (isSpeaking || isDestroyed) return

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            if (!isSpeaking && !isDestroyed) {
                startListening()
            }
        }, MIC_RESTART_DELAY_MS)
    }

    fun startListening() {
        if (isSpeaking || isDestroyed) return

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post {
            try {
                // Mute system sounds to prevent beeping
                val am = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                am.setStreamVolume(android.media.AudioManager.STREAM_SYSTEM, 0, 0)
                am.setStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, 0, 0)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }

                speechRecognizer.startListening(intent)

                // Restore volumes after a delay
                handler.postDelayed({
                    try {
                        am.setStreamVolume(
                            android.media.AudioManager.STREAM_SYSTEM,
                            am.getStreamMaxVolume(android.media.AudioManager.STREAM_SYSTEM) / 2, 0
                        )
                        am.setStreamVolume(
                            android.media.AudioManager.STREAM_NOTIFICATION,
                            am.getStreamMaxVolume(android.media.AudioManager.STREAM_NOTIFICATION) / 2, 0
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Volume restore error: ${e.message}")
                    }
                }, 1500)

            } catch (e: Exception) {
                Log.e(TAG, "Error starting listening", e)
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Stop listening error: ${e.message}")
        }
    }

    fun stopSpeaking() {
        tts.stop()
        isSpeaking = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isDestroyed = true
        try {
            speechRecognizer.destroy()
        } catch (e: Exception) {}
        try {
            tts.stop()
            tts.shutdown()
        } catch (e: Exception) {}
        onStateChanged = null
        onResponse = null
        super.onDestroy()
    }
}
