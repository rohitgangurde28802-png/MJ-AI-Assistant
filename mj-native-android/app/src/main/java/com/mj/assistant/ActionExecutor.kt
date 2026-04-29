package com.mj.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

object ActionExecutor {
    private const val TAG = "MJ.Action"

    fun execute(context: Context, action: String, target: String? = null): String {
        Log.d(TAG, "Action=$action target=$target")
        return try {
            when (action.uppercase().trim()) {
                "OPEN_YOUTUBE"    -> launch(context, "com.google.android.youtube") ?: openUrl(context, "https://youtube.com")
                "OPEN_WHATSAPP"   -> launch(context, "com.whatsapp") ?: openUrl(context, "https://web.whatsapp.com")
                "OPEN_SPOTIFY"    -> launch(context, "com.spotify.music") ?: openUrl(context, "https://open.spotify.com")
                "OPEN_INSTAGRAM"  -> launch(context, "com.instagram.android") ?: openUrl(context, "https://instagram.com")
                "OPEN_GMAIL"      -> launch(context, "com.google.android.gm") ?: "Gmail not installed"
                "OPEN_MAPS"       -> openUrl(context, "geo:0,0?q=")
                "OPEN_CAMERA"     -> openCamera(context)
                "OPEN_SETTINGS"   -> openSettings(context)
                "OPEN_PHONE"      -> openDialer(context)
                "OPEN_CALENDAR"   -> launch(context, "com.google.android.calendar") ?: "Calendar not found"
                "OPEN_GOOGLE"     -> openUrl(context, "https://www.google.com")
                "OPEN_FACEBOOK"   -> launch(context, "com.facebook.katana") ?: openUrl(context, "https://facebook.com")
                "OPEN_TWITTER"    -> launch(context, "com.twitter.android") ?: openUrl(context, "https://twitter.com")
                "OPEN_NETFLIX"    -> launch(context, "com.netflix.mediaclient") ?: openUrl(context, "https://netflix.com")
                "SEARCH_WEB"      -> searchWeb(context, target ?: "")
                "PLAY_MUSIC"      -> playMusic(context, target ?: "")
                else -> {
                    // Try smart fallback: treat action as app name
                    val appName = action.replace("OPEN_", "").replace("_", " ").lowercase()
                    searchWeb(context, "$appName app")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action failed: ${e.message}")
            "Action failed: ${e.message}"
        }
    }

    private fun launch(ctx: Context, pkg: String): String? {
        return try {
            val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return null
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            "Opened."
        } catch (e: Exception) { null }
    }

    private fun openUrl(ctx: Context, url: String): String {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
        return "Opened."
    }

    private fun searchWeb(ctx: Context, query: String): String {
        val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(intent)
        return "Searching for $query."
    }

    private fun playMusic(ctx: Context, song: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(song)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            "Playing $song on Spotify."
        } catch (e: Exception) {
            searchWeb(ctx, "$song song youtube")
        }
    }

    private fun openCamera(ctx: Context): String {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(intent); return "Opening camera."
    }

    private fun openSettings(ctx: Context): String {
        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(intent); return "Opening settings."
    }

    private fun openDialer(ctx: Context): String {
        val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(intent); return "Opening dialer."
    }
}
