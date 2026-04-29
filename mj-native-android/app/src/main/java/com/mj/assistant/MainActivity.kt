package com.mj.assistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * MJ AI Assistant – Main Activity (Stable v2.1)
 * Iron Man Arc Reactor themed UI — robustness-first build.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MJ.Main"
        private const val PERM_MIC = 100
    }

    // Views
    private lateinit var tvStatus: TextView
    private lateinit var tvOrb: TextView
    private lateinit var tvState: TextView
    private lateinit var tvChat: TextView
    private lateinit var scrollChat: ScrollView
    private lateinit var btnMic: Button
    private lateinit var btnSend: Button
    private lateinit var etInput: EditText
    private lateinit var chipsRow: LinearLayout
    private lateinit var orbView: View
    private lateinit var orbOuter: View
    private lateinit var tvHint: TextView

    private val gemini = GeminiClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentState = "idle"
    private var chatLog = StringBuilder()

    // State colors
    private val cyan = Color.parseColor("#00F0FF")
    private val green = Color.parseColor("#00FA9A")
    private val purple = Color.parseColor("#B026FF")
    private val gold = Color.parseColor("#FFB800")
    private val red = Color.parseColor("#FF2A2A")
    private val dark = Color.parseColor("#050510")

    private val quickChips = listOf(
        "Open YouTube" to "open YouTube",
        "Weather" to "what is the weather today",
        "News" to "give me latest news",
        "Open Maps" to "open maps",
        "What time is it" to "what time is it",
        "Open WhatsApp" to "open WhatsApp",
        "Tell a joke" to "tell me a joke",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUI()
        setupVoiceCallbacks()
        setState("idle")
        addToChat("MJ", "System online. Tap the mic or a quick action to begin, Mr. Rohit.")
    }

    // ── Build entire UI programmatically (no XML dependency issues) ──────────
    private fun buildUI() {
        val root = RelativeLayout(this).apply {
            setBackgroundColor(dark)
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(root)
        window.statusBarColor = Color.parseColor("#030308")
        window.navigationBarColor = dark

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        // ── HUD Status Bar ───────────────────────────────────────────────────
        tvStatus = TextView(this).apply {
            id = View.generateViewId()
            text = "MJ ASSISTANT // SYSTEM ONLINE"
            textSize = 10f
            setTextColor(Color.parseColor("#6E7681"))
            letterSpacing = 0.15f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(dp(18), dp(48), dp(18), dp(8))
        }
        root.addView(tvStatus, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.ALIGN_PARENT_TOP) })

        // ── Arc Reactor Orb ──────────────────────────────────────────────────
        val orbContainer = FrameLayout(this).apply {
            id = View.generateViewId()
        }
        val orbContainerParams = RelativeLayout.LayoutParams(dp(180), dp(180)).apply {
            addRule(RelativeLayout.BELOW, tvStatus.id)
            addRule(RelativeLayout.CENTER_HORIZONTAL)
            topMargin = dp(8)
        }

        // Outer pulse ring
        orbOuter = View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), Color.parseColor("#3300F0FF"))
            }
        }
        orbContainer.addView(orbOuter, FrameLayout.LayoutParams(dp(180), dp(180), Gravity.CENTER))

        // Inner ring
        val orbInner = View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.parseColor("#2200F0FF"))
            }
        }
        orbContainer.addView(orbInner, FrameLayout.LayoutParams(dp(130), dp(130), Gravity.CENTER))

        // Core orb
        orbView = View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(cyan)
            }
        }
        orbContainer.addView(orbView, FrameLayout.LayoutParams(dp(80), dp(80), Gravity.CENTER))

        // MJ Label on orb
        tvOrb = TextView(this).apply {
            text = "MJ"
            textSize = 20f
            setTextColor(dark)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        orbContainer.addView(tvOrb, FrameLayout.LayoutParams(dp(80), dp(80), Gravity.CENTER))

        root.addView(orbContainer, orbContainerParams)

        // ── State label ──────────────────────────────────────────────────────
        tvState = TextView(this).apply {
            id = View.generateViewId()
            text = "I AM HERE"
            textSize = 11f
            setTextColor(cyan)
            letterSpacing = 0.2f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        root.addView(tvState, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, orbContainer.id)
            addRule(RelativeLayout.CENTER_HORIZONTAL)
        })

        // ── Quick Action Chips ───────────────────────────────────────────────
        val chipsScroll = HorizontalScrollView(this).apply {
            id = View.generateViewId()
            isHorizontalScrollBarEnabled = false
        }
        chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), 0, dp(14), 0)
        }
        chipsScroll.addView(chipsRow)

        quickChips.forEach { (label, query) ->
            val chip = TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(Color.parseColor("#E0E0E0"))
                setPadding(dp(14), dp(8), dp(14), dp(8))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#141922"))
                    setStroke(dp(1), Color.parseColor("#2A3142"))
                    cornerRadius = dp(999).toFloat()
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, dp(8), 0)
                layoutParams = lp
                setOnClickListener { submitText(query) }
            }
            chipsRow.addView(chip)
        }

        root.addView(chipsScroll, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, tvState.id)
            topMargin = dp(10)
        })

        // ── Chat Transcript ──────────────────────────────────────────────────
        scrollChat = ScrollView(this).apply {
            id = View.generateViewId()
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#08FFFFFF"))
                setStroke(dp(1), Color.parseColor("#15FFFFFF"))
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(2), dp(2), dp(2), dp(2))
            isVerticalScrollBarEnabled = false
        }
        tvChat = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.parseColor("#E8E8E8"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setLineSpacing(dp(5).toFloat(), 1f)
            movementMethod = ScrollingMovementMethod()
        }
        scrollChat.addView(tvChat)

        // ── Bottom Section ───────────────────────────────────────────────────
        val bottomSection = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }

        // Text input row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#0AFFFFFF"))
                setStroke(dp(1), Color.parseColor("#18FFFFFF"))
                cornerRadius = dp(28).toFloat()
            }
            setPadding(dp(12), 0, dp(8), 0)
        }
        etInput = EditText(this).apply {
            hint = "Type a command…"
            setHintTextColor(Color.parseColor("#6E7681"))
            setTextColor(Color.parseColor("#F5F5F7"))
            textSize = 15f
            background = null
            setPadding(dp(8), dp(12), dp(8), dp(12))
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEND) { sendTextInput(); true } else false
            }
        }
        btnSend = Button(this).apply {
            text = "→"
            textSize = 18f
            setTextColor(dark)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#F5F5F7"))
            }
            val lp = LinearLayout.LayoutParams(dp(40), dp(40))
            lp.setMargins(dp(4), 0, 0, 0)
            layoutParams = lp
            setOnClickListener { sendTextInput() }
        }
        inputRow.addView(etInput)
        inputRow.addView(btnSend)

        // Mic button
        btnMic = Button(this).apply {
            text = "🎙 TAP TO TALK"
            textSize = 14f
            setTextColor(dark)
            letterSpacing = 0.08f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F5F5F7"))
                cornerRadius = dp(36).toFloat()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
            )
            lp.setMargins(0, dp(12), 0, 0)
            layoutParams = lp
            setOnClickListener { handleMicTap() }
        }

        tvHint = TextView(this).apply {
            text = "Say \"Hey MJ\" after tapping · Powered by Gemini 2.0"
            textSize = 10f
            setTextColor(Color.parseColor("#6E7681"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        bottomSection.addView(inputRow)
        bottomSection.addView(btnMic)
        bottomSection.addView(tvHint)

        root.addView(bottomSection, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.ALIGN_PARENT_BOTTOM) })

        // Chat fills remaining space
        root.addView(scrollChat, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, chipsScroll.id)
            addRule(RelativeLayout.ABOVE, bottomSection.id)
            setMargins(dp(14), dp(10), dp(14), dp(8))
        })

        startOrbAnimation()
    }

    // ── Voice Service Callbacks ──────────────────────────────────────────────
    private fun setupVoiceCallbacks() {
        VoiceService.onStateChanged = { state, extra ->
            mainHandler.post {
                setState(state)
                if (state == "thinking" && extra != null) {
                    addToChat("YOU", extra)
                }
            }
        }
        VoiceService.onResponse = { userText, mjResponse ->
            mainHandler.post {
                addToChat("MJ", mjResponse)
                setState("idle")
            }
        }
    }

    // ── Mic tap ─────────────────────────────────────────────────────────────
    private fun handleMicTap() {
        when (currentState) {
            "listening" -> {
                // Already listening — cancel
                setState("idle")
                btnMic.text = "🎙 TAP TO TALK"
            }
            "thinking", "speaking" -> {
                // Busy — ignore
                Toast.makeText(this, "MJ is busy, please wait…", Toast.LENGTH_SHORT).show()
            }
            else -> {
                if (!checkMicPermission()) {
                    requestMicPermission()
                    return
                }
                startVoiceService()
                setState("listening")
                btnMic.text = "⏹ STOP LISTENING"
            }
        }
    }

    // ── Text submit ──────────────────────────────────────────────────────────
    private fun sendTextInput() {
        val text = etInput.text.toString().trim()
        if (text.isNotEmpty()) {
            etInput.setText("")
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etInput.windowToken, 0)
            submitText(text)
        }
    }

    private fun submitText(text: String) {
        addToChat("YOU", text)
        setState("thinking")

        gemini.fetchResponse(text) { response ->
            mainHandler.post {
                var cleanResponse = response
                if (response.contains("[ACTION:")) {
                    val match = Regex("\\[ACTION:([^\\]]+)\\]").find(response)
                    val fullAction = match?.groupValues?.get(1) ?: ""
                    val (action, target) = if (":" in fullAction) {
                        val parts = fullAction.split(":", limit = 2)
                        parts[0].trim() to parts[1].trim()
                    } else {
                        fullAction.trim() to null
                    }
                    try { ActionExecutor.execute(this@MainActivity, action, target) }
                    catch (e: Exception) { Log.e(TAG, "Action error: ${e.message}") }
                    cleanResponse = response.replace(Regex("\\[ACTION:[^\\]]+\\]\\s*"), "").trim()
                }
                addToChat("MJ", cleanResponse)
                setState("idle")
            }
        }
    }

    // ── Chat log ─────────────────────────────────────────────────────────────
    private fun addToChat(role: String, message: String) {
        val prefix = if (role == "MJ") "🔵 MJ: " else "⚪ You: "
        chatLog.append("$prefix$message\n\n")
        tvChat.text = chatLog.toString()
        mainHandler.postDelayed({
            scrollChat.fullScroll(View.FOCUS_DOWN)
        }, 100)
    }

    // ── State machine ────────────────────────────────────────────────────────
    private fun setState(state: String) {
        currentState = state
        val (color, label, btnLabel) = when (state) {
            "idle"      -> Triple(cyan,   "I AM HERE",  "🎙 TAP TO TALK")
            "listening" -> Triple(green,  "LISTENING",  "⏹ STOP LISTENING")
            "thinking"  -> Triple(purple, "THINKING…",  "⏳ THINKING…")
            "speaking"  -> Triple(gold,   "SPEAKING",   "🔊 SPEAKING…")
            "error"     -> Triple(red,    "ERROR",      "🎙 TAP TO TALK")
            else        -> Triple(cyan,   "I AM HERE",  "🎙 TAP TO TALK")
        }
        tvState.text = label
        tvState.setTextColor(color)
        tvOrb.setTextColor(dark)
        (orbView.background as? android.graphics.drawable.GradientDrawable)?.setColor(color)
        btnMic.text = btnLabel
        btnMic.setTextColor(dark)
        (btnMic.background as? android.graphics.drawable.GradientDrawable)?.setColor(
            if (state == "listening") red else Color.parseColor("#F5F5F7")
        )
    }

    // ── Orb animation ────────────────────────────────────────────────────────
    private fun startOrbAnimation() {
        // Pulse the core orb
        ObjectAnimator.ofFloat(orbView, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 2000; repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator(); start()
        }
        ObjectAnimator.ofFloat(orbView, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 2000; repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator(); start()
        }
        // Rotate outer ring
        ObjectAnimator.ofFloat(orbOuter, "rotation", 0f, 360f).apply {
            duration = 12000; repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator(); start()
        }
    }

    // ── Permissions ──────────────────────────────────────────────────────────
    private fun checkMicPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestMicPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERM_MIC)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_MIC && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            handleMicTap()
        } else {
            Toast.makeText(this, "MJ needs microphone permission to listen to you!", Toast.LENGTH_LONG).show()
        }
    }

    // ── Voice Service ────────────────────────────────────────────────────────
    private fun startVoiceService() {
        val intent = Intent(this, VoiceService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VoiceService: ${e.message}")
            Toast.makeText(this, "Could not start voice service", Toast.LENGTH_SHORT).show()
            setState("idle")
        }
    }

    override fun onDestroy() {
        VoiceService.onStateChanged = null
        VoiceService.onResponse = null
        super.onDestroy()
    }
}
