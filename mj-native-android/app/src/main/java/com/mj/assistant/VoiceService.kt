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
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceService : Service(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var geminiClient: GeminiClient

    private var isListeningForCommand = false
    private var isSpeaking = false

    override fun onCreate() {
        super.onCreate()
        geminiClient = GeminiClient()
        tts = TextToSpeech(this, this)
        
        setupForegroundNotification()
        setupSpeechRecognizer()
    }

    private fun setupForegroundNotification() {
        val channelId = "mj_voice_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MJ Voice Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MJ Native Assistant")
            .setContentText("Always listening...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(101, notification)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            startListening() // Start loop once TTS is ready
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (!isSpeaking) {
                    startListening() // Restart loop gracefully
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val heardText = matches[0].lowercase(Locale.getDefault())
                    Log.d("VoiceService", "Heard: $heardText")

                    if (!isListeningForCommand) {
                        // STATE 1: Waiting for Wake Word
                        if (heardText.contains("hey mj") || heardText.contains("am jay") || heardText.contains("mj")) {
                            isListeningForCommand = true
                            speak("What can I do for you, Mr Rohit?")
                            // After speaking, it will start listening for command
                        } else {
                            startListening() // Keep hunting for wake word
                        }
                    } else {
                        // STATE 2: Processing Command
                        isListeningForCommand = false
                        geminiClient.fetchResponse(heardText) { response ->
                            var cleanResponse = response
                            
                            // Check for ACTION codes
                            if (response.contains("[ACTION:")) {
                                val actionMatch = Regex("\\[ACTION:(.*?)\\]").find(response)
                                val fullAction = actionMatch?.groupValues?.get(1) ?: ""
                                
                                // Execute Android action
                                handleAndroidAction(fullAction)
                                
                                // Clean prefix from speaking
                                cleanResponse = response.replace(Regex("\\[ACTION:.*?\\]"), "").trim()
                                Log.d("VoiceService", "Action detected: $fullAction")
                            }
                            
                            speak(cleanResponse)
                        }
                    }
                } else {
                    startListening()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleAndroidAction(action: String) {
        try {
            when {
                action.startsWith("OPEN_YOUTUBE") -> {
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    intent?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                action.startsWith("OPEN_GOOGLE") -> {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_APP_BROWSER)
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                action.startsWith("OPEN_WHATSAPP") -> {
                    val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                    intent?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                action.startsWith("SEARCH_WEB:") -> {
                    val query = action.replace("SEARCH_WEB:", "")
                    val intent = Intent(Intent.ACTION_WEB_SEARCH)
                    intent.putExtra(android.app.SearchManager.QUERY, query)
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "Failed to execute action: $action", e)
        }
    }

    private fun speak(text: String) {
        isSpeaking = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mj_tts")
        
        // Polling to know when speaking is done to resume mic
        Thread {
            while (tts.isSpeaking) { Thread.sleep(100) }
            isSpeaking = false
            startListening()
        }.start()
    }

    private fun startListening() {
        if (isSpeaking) return
        
        // Use a Handler to delay logic to avoid tight loops that cause beeping
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    // Try to minimize the "beep" by suggesting no sound (only works on some devices)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }

                // Mute system sounds briefly to hide the 'listening' beep
                val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_SYSTEM, 0, 0)
                
                speechRecognizer.startListening(intent)
                
                // Restore volume shortly after
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_SYSTEM, 2, 0)
                }, 1000)
                
            } catch (e: Exception) {
                Log.e("VoiceService", "Error starting listening", e)
                startListening() // Retry
            }
        }, 1500) // 1.5 second delay between listening sessions stops the 'machine gun' beep
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.stop()
        tts.shutdown()
    }
}
