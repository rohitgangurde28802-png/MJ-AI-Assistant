package com.mj.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

/**
 * MJ AI Assistant – Action Executor
 * Handles all device-level actions: opening apps, dialing, searching, etc.
 */
object ActionExecutor {

    private const val TAG = "ActionExecutor"

    // Map of known apps to their package names
    private val APP_PACKAGES = mapOf(
        "YOUTUBE" to "com.google.android.youtube",
        "WHATSAPP" to "com.whatsapp",
        "SPOTIFY" to "com.spotify.music",
        "INSTAGRAM" to "com.instagram.android",
        "FACEBOOK" to "com.facebook.katana",
        "TWITTER" to "com.twitter.android",
        "GMAIL" to "com.google.android.gm",
        "MAPS" to "com.google.android.apps.maps",
        "CHROME" to "com.android.chrome",
        "CALENDAR" to "com.google.android.calendar",
        "CAMERA" to null,  // uses intent
        "SETTINGS" to null,  // uses intent
        "PHONE" to null,  // uses intent
    )

    /**
     * Execute an action parsed from Gemini's response.
     * @return A spoken message to confirm the action.
     */
    fun execute(context: Context, action: String, target: String? = null): String {
        Log.d(TAG, "Executing action: $action, target: $target")

        return try {
            when {
                action.startsWith("OPEN_") -> handleOpenApp(context, action.removePrefix("OPEN_"))
                action.startsWith("SEARCH_WEB") -> handleWebSearch(context, target ?: "")
                action.startsWith("PLAY_MUSIC") -> handlePlayMusic(context, target ?: "")
                else -> "I don't know how to do that yet."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action failed: $action", e)
            "I had trouble doing that. ${e.message}"
        }
    }

    private fun handleOpenApp(context: Context, appName: String): String {
        val name = appName.uppercase()

        // Special intents
        when (name) {
            "CAMERA" -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Opening camera."
            }
            "SETTINGS" -> {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Opening settings."
            }
            "PHONE" -> {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Opening phone dialer."
            }
            "GOOGLE" -> {
                // Try Chrome first, then default browser
                val chromeIntent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                if (chromeIntent != null) {
                    chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chromeIntent)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                return "Opening Google."
            }
            "MAPS" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
                intent.setPackage("com.google.android.apps.maps")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Opening Maps."
            }
        }

        // Package-based launch
        val packageName = APP_PACKAGES[name]
        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Opening ${name.lowercase().replaceFirstChar { it.uppercase() }}."
            }
        }

        // Fallback: search for the app
        val searchIntent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${name.lowercase()}+app"))
        searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(searchIntent)
        return "I couldn't find ${name.lowercase()} directly, so I searched for it."
    }

    private fun handleWebSearch(context: Context, query: String): String {
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return "Searching the web for $query."
    }

    private fun handlePlayMusic(context: Context, song: String): String {
        // Try Spotify first
        try {
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("spotify:search:${Uri.encode(song)}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "Playing $song on Spotify."
        } catch (e: Exception) {
            // Fallback to YouTube search
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(song)}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "Searching for $song on YouTube."
        }
    }
}
