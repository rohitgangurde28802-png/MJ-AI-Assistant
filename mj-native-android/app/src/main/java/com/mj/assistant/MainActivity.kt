package com.mj.assistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MJ"
        private const val PERM = 100
    }

    private val gemini = GeminiClient()
    private val handler = Handler(Looper.getMainLooper())
    private var state = "idle"

    // Animators
    private var scaleXAnimator: ObjectAnimator? = null
    private var scaleYAnimator: ObjectAnimator? = null
    private var rotationOuterAnimator: ObjectAnimator? = null
    private var rotationInnerAnimator: ObjectAnimator? = null
    private var glowAlphaAnimator: ObjectAnimator? = null
    private var pulseRingScaleXAnimator: ObjectAnimator? = null
    private var pulseRingScaleYAnimator: ObjectAnimator? = null
    private var pulseRingAlphaAnimator: ObjectAnimator? = null

    // Views
    private lateinit var statusDot: View
    private lateinit var tvStatusHud: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var orbGlow: View
    private lateinit var orbRingOuter: View
    private lateinit var orbRingInner: View
    private lateinit var orbCore: View
    private lateinit var tvState: TextView
    private lateinit var busyRow: View
    private lateinit var tvBusyText: TextView
    private lateinit var scrollTranscript: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var tvPlaceholder: TextView
    private lateinit var chipsRow: LinearLayout
    private lateinit var textInputBar: View
    private lateinit var etTextInput: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnCloseText: ImageButton
    private lateinit var voiceControlsRow: View
    private lateinit var btnKeyboard: ImageButton
    private lateinit var micPulseRing: View
    private lateinit var btnMic: ImageButton
    private lateinit var btnCancel: ImageButton
    private lateinit var tvHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind Views
        statusDot = findViewById(R.id.statusDot)
        tvStatusHud = findViewById(R.id.tvStatusHud)
        btnSettings = findViewById(R.id.btnSettings)
        orbGlow = findViewById(R.id.orbGlow)
        orbRingOuter = findViewById(R.id.orbRingOuter)
        orbRingInner = findViewById(R.id.orbRingInner)
        orbCore = findViewById(R.id.orbCore)
        tvState = findViewById(R.id.tvState)
        busyRow = findViewById(R.id.busyRow)
        tvBusyText = findViewById(R.id.tvBusyText)
        scrollTranscript = findViewById(R.id.scrollTranscript)
        chatContainer = findViewById(R.id.chatContainer)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)
        chipsRow = findViewById(R.id.chipsRow)
        textInputBar = findViewById(R.id.textInputBar)
        etTextInput = findViewById(R.id.etTextInput)
        btnSend = findViewById(R.id.btnSend)
        btnCloseText = findViewById(R.id.btnCloseText)
        voiceControlsRow = findViewById(R.id.voiceControlsRow)
        btnKeyboard = findViewById(R.id.btnKeyboard)
        micPulseRing = findViewById(R.id.micPulseRing)
        btnMic = findViewById(R.id.btnMic)
        btnCancel = findViewById(R.id.btnCancel)
        tvHint = findViewById(R.id.tvHint)

        setupLayout()
        setupCallbacks()
        updateTime()
        addChat("MJ", "System online. Tap the mic or a quick action to begin, Mr. Rohit.")
        setState("idle")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun setupLayout() {
        // Quick Action Chips Row
        chipsRow.removeAllViews()
        listOf(
            "▶ YouTube" to "open YouTube",
            "🌤 Weather" to "what is the weather today",
            "📰 News" to "latest news headlines",
            "🗺 Maps" to "open maps",
            "💬 WhatsApp" to "open WhatsApp",
            "🎵 Spotify" to "open Spotify",
            "🕐 Time" to "what time is it now"
        ).forEach { (lbl, query) ->
            val chip = TextView(this).apply {
                text = lbl
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.arc_cyan))
                setPadding(dp(14), dp(7), dp(14), dp(7))
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.chip_bg)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, dp(8), 0) }
                setOnClickListener { submitText(query) }
            }
            chipsRow.addView(chip)
        }

        // Keyboard Button Toggle
        btnKeyboard.setOnClickListener {
            textInputBar.visibility = View.VISIBLE
            voiceControlsRow.visibility = View.GONE
            etTextInput.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etTextInput, InputMethodManager.SHOW_IMPLICIT)
        }

        // Close Text Input Toggle
        btnCloseText.setOnClickListener {
            textInputBar.visibility = View.GONE
            voiceControlsRow.visibility = View.VISIBLE
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etTextInput.windowToken, 0)
        }

        // Input Actions
        etTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendInput()
                true
            } else false
        }
        btnSend.setOnClickListener { sendInput() }

        // Mic Button
        btnMic.setOnClickListener { handleMic() }

        // Cancel Button
        btnCancel.setOnClickListener {
            val i = Intent(this@MainActivity, VoiceService::class.java)
            stopService(i)
            setState("idle")
        }

        // Settings Button Dialog
        btnSettings.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("MJ Assistant Settings")

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(10), dp(20), dp(10))
            }

            val tvLabel = TextView(this).apply {
                text = "Gemini API Key:"
                setTextColor(Color.WHITE)
                textSize = 14f
            }

            val prefs = getSharedPreferences("mj_prefs", MODE_PRIVATE)
            val savedKey = prefs.getString("gemini_api_key", "")

            val etKey = EditText(this).apply {
                setText(savedKey)
                hint = "Enter Gemini API Key"
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }

            val btnClear = Button(this).apply {
                text = "Clear Chat History"
                setTextColor(Color.parseColor("#FF3333"))
                background = null
                setOnClickListener {
                    chatContainer.removeAllViews()
                    tvPlaceholder.visibility = View.VISIBLE
                    gemini.clearHistory()
                    Toast.makeText(this@MainActivity, "Chat history cleared", Toast.LENGTH_SHORT).show()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(16); gravity = Gravity.CENTER_HORIZONTAL }
            }

            layout.addView(tvLabel)
            layout.addView(etKey)
            layout.addView(btnClear)
            builder.setView(layout)

            builder.setPositiveButton("Save") { dialog, _ ->
                val newKey = etKey.text.toString().trim()
                prefs.edit().putString("gemini_api_key", newKey).apply()
                Toast.makeText(this, "API Key saved!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            val alert = builder.create()
            alert.setOnShowListener {
                alert.window?.setBackgroundDrawable(GradientDrawable().apply {
                    setColor(Color.parseColor("#0A0A1A"))
                    setStroke(dp(1), Color.parseColor("#00F0FF"))
                    cornerRadius = dp(14).toFloat()
                })
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#00F0FF"))
                alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
            }
            alert.show()
        }

        startAnimations()
    }

    private fun startAnimations() {
        // Orb Core Pulse
        scaleXAnimator = ObjectAnimator.ofFloat(orbCore, "scaleX", 1f, 1.06f, 1f).apply {
            duration = 2200; repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator(); start()
        }
        scaleYAnimator = ObjectAnimator.ofFloat(orbCore, "scaleY", 1f, 1.06f, 1f).apply {
            duration = 2200; repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator(); start()
        }

        // Outer and Inner Rings Rotations
        rotationOuterAnimator = ObjectAnimator.ofFloat(orbRingOuter, "rotation", 0f, 360f).apply {
            duration = 14000; repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator(); start()
        }
        rotationInnerAnimator = ObjectAnimator.ofFloat(orbRingInner, "rotation", 0f, -360f).apply {
            duration = 22000; repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator(); start()
        }

        // Glow Pulse
        glowAlphaAnimator = ObjectAnimator.ofFloat(orbGlow, "alpha", 0.3f, 0.7f, 0.3f).apply {
            duration = 2800; repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator(); start()
        }
    }

    private fun updateTime() {
        val cal = java.util.Calendar.getInstance()
        val timeStr = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        tvStatusHud.text = "SYS.STATUS // ONLINE | $timeStr"
        handler.postDelayed({ updateTime() }, 30000)
    }

    private fun setState(s: String) {
        state = s
        val color = when (s) {
            "listening" -> ContextCompat.getColor(this, R.color.listening_green)
            "thinking"  -> ContextCompat.getColor(this, R.color.thinking_purple)
            "speaking"  -> ContextCompat.getColor(this, R.color.speaking_amber)
            "error"     -> ContextCompat.getColor(this, R.color.error_red)
            else        -> ContextCompat.getColor(this, R.color.arc_cyan)
        }

        val label = when (s) {
            "listening" -> "LISTENING // VOICE ACTIVE"
            "thinking"  -> "PROCESSING // GEMINI ACTIVE"
            "speaking"  -> "SPEAKING // OUTPUT ACTIVE"
            "error"     -> "ERROR // CHECK CONNECTION"
            else        -> "STANDBY // AWAITING INPUT"
        }

        tvState.text = label
        tvState.setTextColor(color)

        // Dynamically Tint Drawables
        statusDot.background?.setTint(color)
        orbCore.background?.setTint(color)
        orbGlow.background?.setTint(adjustAlpha(color, 0.4f))

        // Handle Thinking Indicator Row
        if (s == "thinking") {
            busyRow.visibility = View.VISIBLE
            tvBusyText.text = "MJ is thinking…"
            tvBusyText.setTextColor(color)
        } else {
            busyRow.visibility = View.GONE
        }

        // Handle Mic Pulsing Ring for Listening State
        if (s == "listening") {
            micPulseRing.visibility = View.VISIBLE
            micPulseRing.background?.setTint(color)
            
            pulseRingScaleXAnimator?.cancel()
            pulseRingScaleYAnimator?.cancel()
            pulseRingAlphaAnimator?.cancel()

            pulseRingScaleXAnimator = ObjectAnimator.ofFloat(micPulseRing, "scaleX", 1.0f, 1.4f).apply {
                duration = 1000; repeatCount = ValueAnimator.INFINITE; start()
            }
            pulseRingScaleYAnimator = ObjectAnimator.ofFloat(micPulseRing, "scaleY", 1.0f, 1.4f).apply {
                duration = 1000; repeatCount = ValueAnimator.INFINITE; start()
            }
            pulseRingAlphaAnimator = ObjectAnimator.ofFloat(micPulseRing, "alpha", 0.6f, 0.0f).apply {
                duration = 1000; repeatCount = ValueAnimator.INFINITE; start()
            }
        } else {
            micPulseRing.visibility = View.GONE
            pulseRingScaleXAnimator?.cancel()
            pulseRingScaleYAnimator?.cancel()
            pulseRingAlphaAnimator?.cancel()
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int = Color.argb(
        (Color.alpha(color) * factor).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun addChat(role: String, msg: String) {
        tvPlaceholder.visibility = View.GONE

        val bubble = TextView(this).apply {
            text = msg
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
            background = ContextCompat.getDrawable(this@MainActivity,
                if (role == "MJ") R.drawable.bubble_mj else R.drawable.bubble_user
            )
            setPadding(dp(14), dp(10), dp(14), dp(10))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (role == "MJ") Gravity.START else Gravity.END
                topMargin = dp(6)
                bottomMargin = dp(6)
                if (role == "MJ") {
                    rightMargin = dp(48)
                    leftMargin = 0
                } else {
                    leftMargin = dp(48)
                    rightMargin = 0
                }
            }
            layoutParams = params
        }

        chatContainer.addView(bubble)
        handler.postDelayed({ scrollTranscript.fullScroll(View.FOCUS_DOWN) }, 100)
    }

    private fun setupCallbacks() {
        VoiceService.onStateChanged = { s, extra ->
            handler.post {
                setState(s)
                if (s == "thinking" && extra != null) addChat("YOU", extra)
            }
        }
        VoiceService.onResponse = { _, resp ->
            handler.post { addChat("MJ", resp); setState("idle") }
        }
    }

    private fun handleMic() {
        when (state) {
            "thinking", "speaking" -> { Toast.makeText(this, "MJ is busy…", Toast.LENGTH_SHORT).show(); return }
            "listening" -> {
                val i = Intent(this, VoiceService::class.java)
                stopService(i)
                setState("idle")
                return
            }
        }
        if (!hasPermissions()) { requestPermissions(); return }
        startVoice()
        setState("listening")
    }

    private fun startVoice() {
        val i = Intent(this, VoiceService::class.java).apply {
            putExtra("EXTRA_DIRECT_LISTEN", true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }

    private fun sendInput() {
        val t = etTextInput.text.toString().trim()
        if (t.isEmpty()) return
        etTextInput.setText("")
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etTextInput.windowToken, 0)

        // Close text section, return to voice section
        textInputBar.visibility = View.GONE
        voiceControlsRow.visibility = View.VISIBLE

        submitText(t)
    }

    private fun submitText(text: String) {
        addChat("YOU", text)
        setState("thinking")
        gemini.fetchResponse(this, text) { resp ->
            handler.post {
                var clean = resp
                if (resp.contains("[ACTION:")) {
                    val m = Regex("\\[ACTION:([^\\]]+)\\]").find(resp)
                    val full = m?.groupValues?.get(1) ?: ""
                    val action = if (":" in full) full.substringBefore(":") else full
                    val target = if (":" in full) full.substringAfter(":") else null
                    try { ActionExecutor.execute(this@MainActivity, action.trim(), target?.trim()) }
                    catch (e: Exception) { Log.e(TAG, "Action err: ${e.message}") }
                    clean = resp.replace(Regex("\\[ACTION:[^\\]]+\\]\\s*"), "").trim()
                }
                addChat("MJ", clean)
                setState("idle")
            }
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    private fun hasPermissions(): Boolean {
        for (perm in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERM)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == PERM && gr.isNotEmpty() && gr.all { it == PackageManager.PERMISSION_GRANTED }) {
            handleMic()
        } else {
            Toast.makeText(this, "Permissions (microphone and notifications) are required for MJ!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        VoiceService.onStateChanged = null
        VoiceService.onResponse = null
        
        // Stop animations to prevent memory leaks
        scaleXAnimator?.cancel()
        scaleYAnimator?.cancel()
        rotationOuterAnimator?.cancel()
        rotationInnerAnimator?.cancel()
        glowAlphaAnimator?.cancel()
        pulseRingScaleXAnimator?.cancel()
        pulseRingScaleYAnimator?.cancel()
        pulseRingAlphaAnimator?.cancel()
        
        super.onDestroy()
    }
}
