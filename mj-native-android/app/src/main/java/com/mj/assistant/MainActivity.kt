package com.mj.assistant

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
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
 * MJ AI Assistant – Main Activity
 * Full Iron Man / Arc Reactor themed UI with animated orb, chat transcript,
 * quick action chips, voice controls, and text input.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MJ.MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    // UI elements
    private lateinit var statusDot: View
    private lateinit var tvStatusHud: TextView
    private lateinit var tvState: TextView
    private lateinit var busyRow: LinearLayout
    private lateinit var tvBusyText: TextView
    private lateinit var orbGlow: View
    private lateinit var orbRingOuter: View
    private lateinit var orbRingInner: View
    private lateinit var orbCore: FrameLayout
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollTranscript: ScrollView
    private lateinit var tvPlaceholder: TextView
    private lateinit var textInputBar: LinearLayout
    private lateinit var voiceControlsRow: LinearLayout
    private lateinit var etTextInput: EditText
    private lateinit var btnMic: ImageButton
    private lateinit var micPulseRing: View
    private lateinit var chipsRow: LinearLayout

    // State
    private var currentState = "idle"
    private var isListening = false
    private var geminiClient = GeminiClient()
    private var orbPulseAnimator: AnimatorSet? = null
    private var ringRotateAnimator: ObjectAnimator? = null

    // Colors for states
    private val colorIdle = Color.parseColor("#00F0FF")
    private val colorListening = Color.parseColor("#00FA9A")
    private val colorThinking = Color.parseColor("#B026FF")
    private val colorSpeaking = Color.parseColor("#FFB800")
    private val colorError = Color.parseColor("#FF2A2A")

    // Quick action data
    private data class QuickAction(val label: String, val query: String)
    private val quickActions = listOf(
        QuickAction("YouTube", "open YouTube"),
        QuickAction("Weather", "what's the weather"),
        QuickAction("News", "latest news"),
        QuickAction("Maps", "open maps"),
        QuickAction("Time", "what time is it"),
        QuickAction("Calendar", "open calendar"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Make status bar transparent
        window.statusBarColor = Color.parseColor("#030308")
        window.navigationBarColor = Color.parseColor("#050510")

        bindViews()
        setupChips()
        setupListeners()
        startOrbAnimation()

        // Set initial state
        updateState("idle", null)
    }

    private fun bindViews() {
        statusDot = findViewById(R.id.statusDot)
        tvStatusHud = findViewById(R.id.tvStatusHud)
        tvState = findViewById(R.id.tvState)
        busyRow = findViewById(R.id.busyRow)
        tvBusyText = findViewById(R.id.tvBusyText)
        orbGlow = findViewById(R.id.orbGlow)
        orbRingOuter = findViewById(R.id.orbRingOuter)
        orbRingInner = findViewById(R.id.orbRingInner)
        orbCore = findViewById(R.id.orbCore)
        chatContainer = findViewById(R.id.chatContainer)
        scrollTranscript = findViewById(R.id.scrollTranscript)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)
        textInputBar = findViewById(R.id.textInputBar)
        voiceControlsRow = findViewById(R.id.voiceControlsRow)
        etTextInput = findViewById(R.id.etTextInput)
        btnMic = findViewById(R.id.btnMic)
        micPulseRing = findViewById(R.id.micPulseRing)
        chipsRow = findViewById(R.id.chipsRow)
    }

    private fun setupChips() {
        quickActions.forEach { action ->
            val chip = TextView(this).apply {
                text = action.label
                setTextColor(Color.parseColor("#F5F5F7"))
                textSize = 12f
                letterSpacing = 0.08f
                setBackgroundResource(R.drawable.chip_bg)
                setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, dpToPx(8), 0)
                layoutParams = params

                setOnClickListener {
                    submitText(action.query)
                }
            }
            chipsRow.addView(chip)
        }
    }

    private fun setupListeners() {
        // Mic button
        btnMic.setOnClickListener {
            when (currentState) {
                "listening" -> stopListeningAndProcess()
                "speaking", "thinking" -> cancelAll()
                else -> startMic()
            }
        }

        // Keyboard toggle
        findViewById<ImageButton>(R.id.btnKeyboard).setOnClickListener {
            showTextInput()
        }

        // Cancel button
        findViewById<ImageButton>(R.id.btnCancel).setOnClickListener {
            cancelAll()
        }

        // Send button
        findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            val text = etTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                submitText(text)
            }
        }

        // Close text input
        findViewById<ImageButton>(R.id.btnCloseText).setOnClickListener {
            hideTextInput()
        }

        // Text input submit on enter
        etTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = etTextInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    submitText(text)
                }
                true
            } else false
        }

        // Settings button
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // Setup VoiceService callbacks
        VoiceService.onStateChanged = { state, extra ->
            runOnUiThread { updateState(state, extra) }
        }
        VoiceService.onResponse = { userText, mjResponse ->
            runOnUiThread {
                addChatBubble("YOU", userText, isUser = true)
                addChatBubble("MJ", mjResponse, isUser = false)
            }
        }
    }

    private fun startMic() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        if (!isServiceRunning()) {
            startVoiceService()
        }

        updateState("listening", null)
        isListening = true
    }

    private fun stopListeningAndProcess() {
        isListening = false
        updateState("thinking", null)
    }

    private fun cancelAll() {
        isListening = false
        updateState("idle", null)
    }

    private fun submitText(text: String) {
        hideTextInput()
        etTextInput.setText("")
        addChatBubble("YOU", text, isUser = true)
        updateState("thinking", text)

        geminiClient.fetchResponse(text) { response ->
            runOnUiThread {
                var cleanResponse = response

                // Handle actions
                if (response.contains("[ACTION:")) {
                    val actionMatch = Regex("\\[ACTION:([^\\]]+)\\]").find(response)
                    val fullAction = actionMatch?.groupValues?.get(1) ?: ""

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

                    ActionExecutor.execute(this@MainActivity, action, target)
                    cleanResponse = response.replace(Regex("\\[ACTION:[^\\]]+\\]\\s*"), "").trim()
                }

                addChatBubble("MJ", cleanResponse, isUser = false)
                updateState("idle", null)
            }
        }
    }

    private fun showTextInput() {
        textInputBar.visibility = View.VISIBLE
        voiceControlsRow.visibility = View.GONE
        etTextInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etTextInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideTextInput() {
        textInputBar.visibility = View.GONE
        voiceControlsRow.visibility = View.VISIBLE
        etTextInput.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etTextInput.windowToken, 0)
    }

    /**
     * Add a chat bubble to the conversation panel.
     */
    private fun addChatBubble(role: String, text: String, isUser: Boolean) {
        // Hide placeholder
        tvPlaceholder.visibility = View.GONE

        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bgRes = if (isUser) R.drawable.bubble_user else R.drawable.bubble_mj
            setBackgroundResource(bgRes)
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, dpToPx(8))
            params.gravity = if (isUser) Gravity.END else Gravity.START
            layoutParams = params
        }

        // Role label
        val roleLabel = TextView(this).apply {
            this.text = role
            textSize = 9f
            setTextColor(Color.parseColor("#86868B"))
            letterSpacing = 0.15f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Message text
        val messageText = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor("#F5F5F7"))
            setPadding(0, dpToPx(4), 0, 0)
            setLineSpacing(dpToPx(3).toFloat(), 1f)
        }

        bubbleLayout.addView(roleLabel)
        bubbleLayout.addView(messageText)
        chatContainer.addView(bubbleLayout)

        // Scroll to bottom
        scrollTranscript.post {
            scrollTranscript.fullScroll(View.FOCUS_DOWN)
        }
    }

    /**
     * Update the UI state (orb color, label, busy indicator).
     */
    private fun updateState(state: String, extra: String?) {
        currentState = state

        val (color, label, busyText) = when (state) {
            "idle" -> Triple(colorIdle, "I AM HERE", null)
            "listening" -> Triple(colorListening, "LISTENING", null)
            "thinking" -> Triple(colorThinking, "THINKING", "MJ is thinking…")
            "speaking" -> Triple(colorSpeaking, "SPEAKING", null)
            "error" -> Triple(colorError, "ERROR", null)
            else -> Triple(colorIdle, "I AM HERE", null)
        }

        tvState.text = label
        tvState.setTextColor(color)

        // Busy indicator
        if (busyText != null) {
            busyRow.visibility = View.VISIBLE
            tvBusyText.text = busyText
            tvBusyText.setTextColor(color)
        } else {
            busyRow.visibility = View.GONE
        }

        // Mic button appearance
        if (state == "listening") {
            btnMic.setBackgroundResource(R.drawable.mic_fab_bg)
            (btnMic.background as? GradientDrawable)?.setColor(colorError)
            startMicPulse()
        } else {
            btnMic.setBackgroundResource(R.drawable.mic_fab_bg)
            stopMicPulse()
        }

        // Update orb glow color
        updateOrbColor(color)

        // HUD status
        tvStatusHud.text = "SYS.STATUS // $label"
    }

    private fun updateOrbColor(color: Int) {
        // Update orb core color
        val coreDrawable = orbCore.background
        if (coreDrawable is GradientDrawable) {
            coreDrawable.setColor(color)
        }

        // Update glow
        orbGlow.alpha = if (currentState == "idle") 0.3f else 0.6f
    }

    /**
     * Start the orb pulse and ring rotation animations.
     */
    private fun startOrbAnimation() {
        // Pulse animation on core
        val scaleX = ObjectAnimator.ofFloat(orbCore, "scaleX", 1f, 1.08f, 1f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(orbCore, "scaleY", 1f, 1.08f, 1f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        orbPulseAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }

        // Outer ring rotation
        ObjectAnimator.ofFloat(orbRingOuter, "rotation", 0f, 360f).apply {
            duration = 16000
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            start()
        }

        // Inner ring counter-rotation
        ObjectAnimator.ofFloat(orbRingInner, "rotation", 0f, -360f).apply {
            duration = 24000
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            start()
        }

        // Glow pulse
        ObjectAnimator.ofFloat(orbGlow, "alpha", 0.25f, 0.5f, 0.25f).apply {
            duration = 2800
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startMicPulse() {
        micPulseRing.alpha = 0.6f
        val scaleX = ObjectAnimator.ofFloat(micPulseRing, "scaleX", 1f, 1.4f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val scaleY = ObjectAnimator.ofFloat(micPulseRing, "scaleY", 1f, 1.4f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val alpha = ObjectAnimator.ofFloat(micPulseRing, "alpha", 0.6f, 0f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            start()
        }
    }

    private fun stopMicPulse() {
        micPulseRing.alpha = 0f
        micPulseRing.scaleX = 1f
        micPulseRing.scaleY = 1f
    }

    // ── Permissions ──────────────────────────────────────────────────────

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startMic()
        } else {
            Toast.makeText(this, "MJ needs microphone permission to listen", Toast.LENGTH_LONG).show()
        }
    }

    // ── Voice Service ────────────────────────────────────────────────────

    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.i(TAG, "VoiceService started")
    }

    private fun isServiceRunning(): Boolean {
        // Simple check — service is considered running if callbacks are set
        return VoiceService.onStateChanged != null
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        orbPulseAnimator?.cancel()
        VoiceService.onStateChanged = null
        VoiceService.onResponse = null
        super.onDestroy()
    }
}
