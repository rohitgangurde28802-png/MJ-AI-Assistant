package com.mj.assistant

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object JarvisSoundManager {
    private const val TAG = "MJ.Sound"
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ToneGenerator: ${e.message}")
        }
    }

    /**
     * Crisp double beep when the assistant wakes up.
     */
    fun playWakeChime() {
        Thread {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
                Thread.sleep(120)
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
            } catch (e: Exception) {
                Log.w(TAG, "Wake chime failed: ${e.message}")
            }
        }.start()
    }

    /**
     * Confirmation chime when a command begins processing.
     */
    fun playConfirmChime() {
        Thread {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
            } catch (e: Exception) {
                Log.w(TAG, "Confirm chime failed: ${e.message}")
            }
        }.start()
    }

    /**
     * Gentle chime when returning to standby mode.
     */
    fun playStandbyChime() {
        Thread {
            try {
                // Short confirmation tone
                toneGen?.startTone(ToneGenerator.TONE_SUP_CONFIRM, 150)
            } catch (e: Exception) {
                Log.w(TAG, "Standby chime failed: ${e.message}")
            }
        }.start()
    }
}
