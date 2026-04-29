package com.mj.assistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
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
    private val chatLog = StringBuilder()

    // Views
    private lateinit var root: RelativeLayout
    private lateinit var orbCore: View
    private lateinit var ring1: View
    private lateinit var ring2: View
    private lateinit var glow: View
    private lateinit var tvMJ: TextView
    private lateinit var tvState: TextView
    private lateinit var tvTime: TextView
    private lateinit var chatScroll: ScrollView
    private lateinit var tvChat: TextView
    private lateinit var btnMic: TextView
    private lateinit var etInput: EditText

    private val C = object {
        val bg       = Color.parseColor("#050510")
        val bgCard   = Color.parseColor("#0A0A1A")
        val cyan     = Color.parseColor("#00F0FF")
        val cyanDim  = Color.parseColor("#003A45")
        val gold     = Color.parseColor("#C9A84C")
        val purple   = Color.parseColor("#B026FF")
        val green    = Color.parseColor("#00FA9A")
        val red      = Color.parseColor("#FF3333")
        val white    = Color.parseColor("#F0F0FF")
        val dim      = Color.parseColor("#3A4A5A")
        val mono     = Color.parseColor("#4A6A7A")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#030308")
        window.navigationBarColor = C.bg
        buildMarvelUI()
        setupCallbacks()
        updateTime()
        addChat("MJ", "System online. Tap the mic or a quick action to begin, Mr. Rohit.")
        setState("idle")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildMarvelUI() {
        root = RelativeLayout(this).apply { setBackgroundColor(C.bg) }
        setContentView(root)

        // Grid lines overlay (HUD effect)
        val gridOverlay = HudGridView(this)
        root.addView(gridOverlay, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        ))

        // ── TOP HUD BAR ─────────────────────────────────────────────────────
        val topBar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(46), dp(16), dp(10))
        }

        val tvTitle = TextView(this).apply {
            text = "M.J. ASSISTANT"
            textSize = 11f; setTextColor(C.cyan)
            letterSpacing = 0.25f
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvTime = TextView(this).apply {
            text = "00:00"; textSize = 10f
            setTextColor(C.mono); typeface = Typeface.MONOSPACE; letterSpacing = 0.15f
        }
        val tvVer = TextView(this).apply {
            text = "  v2.1"; textSize = 9f
            setTextColor(C.dim); typeface = Typeface.MONOSPACE
        }
        topBar.addView(tvTitle); topBar.addView(tvTime); topBar.addView(tvVer)
        root.addView(topBar, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.ALIGN_PARENT_TOP) })

        // ── STATUS STRIP ────────────────────────────────────────────────────
        val statusStrip = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), dp(8))
        }
        val statusDot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(C.green)
            }
            layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply { setMargins(0,0,dp(7),0) }
        }
        tvState = TextView(this).apply {
            text = "SYSTEM ONLINE // STANDBY"; textSize = 9f
            setTextColor(C.mono); typeface = Typeface.MONOSPACE; letterSpacing = 0.12f
        }
        statusStrip.addView(statusDot); statusStrip.addView(tvState)
        root.addView(statusStrip, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.BELOW, topBar.id) })

        // ── ARC REACTOR ORB ─────────────────────────────────────────────────
        val orbArea = FrameLayout(this).apply { id = View.generateViewId() }

        // Outermost glow
        glow = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#4000F0FF"), Color.TRANSPARENT)
                gradientType = GradientDrawable.RADIAL_GRADIENT
                setSize(dp(220), dp(220))
            }
        }
        orbArea.addView(glow, FrameLayout.LayoutParams(dp(220), dp(220), Gravity.CENTER))

        // Rotating outer ring (dashed)
        ring1 = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.parseColor("#6600F0FF"))
                dashWidth = dp(8).toFloat(); dashGap = dp(4).toFloat()
            }
        }
        orbArea.addView(ring1, FrameLayout.LayoutParams(dp(190), dp(190), Gravity.CENTER))

        // Counter-rotating middle ring
        ring2 = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.parseColor("#3300F0FF"))
            }
        }
        orbArea.addView(ring2, FrameLayout.LayoutParams(dp(145), dp(145), Gravity.CENTER))

        // Inner decorative ring
        val ring3 = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.parseColor("#1500F0FF"))
            }
        }
        orbArea.addView(ring3, FrameLayout.LayoutParams(dp(110), dp(110), Gravity.CENTER))

        // Core orb
        orbCore = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#00F0FF"), Color.parseColor("#006080"))
                gradientType = GradientDrawable.RADIAL_GRADIENT
                setSize(dp(82), dp(82))
            }
            elevation = dp(12).toFloat()
        }
        orbArea.addView(orbCore, FrameLayout.LayoutParams(dp(82), dp(82), Gravity.CENTER))

        // MJ label
        tvMJ = TextView(this).apply {
            text = "MJ"; textSize = 22f
            setTextColor(C.bg); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; elevation = dp(13).toFloat()
        }
        orbArea.addView(tvMJ, FrameLayout.LayoutParams(dp(82), dp(82), Gravity.CENTER))

        root.addView(orbArea, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, dp(230)
        ).apply {
            addRule(RelativeLayout.BELOW, statusStrip.id)
            topMargin = dp(4)
        })

        // ── STATS ROW ───────────────────────────────────────────────────────
        val statsRow = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(4), dp(24), dp(4))
        }
        listOf("GEMINI 2.0" to "AI MODEL", "ONLINE" to "NETWORK", "ACTIVE" to "VOICE").forEach { (val_, label) ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            cell.addView(TextView(this).apply {
                text = val_; textSize = 10f; setTextColor(C.cyan)
                typeface = Typeface.MONOSPACE; gravity = Gravity.CENTER; letterSpacing = 0.1f
            })
            cell.addView(TextView(this).apply {
                text = label; textSize = 8f; setTextColor(C.mono)
                typeface = Typeface.MONOSPACE; gravity = Gravity.CENTER; letterSpacing = 0.1f
            })
            statsRow.addView(cell)
        }
        root.addView(statsRow, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.BELOW, orbArea.id) })

        // ── QUICK CHIPS ─────────────────────────────────────────────────────
        val hScroll = HorizontalScrollView(this).apply {
            id = View.generateViewId()
            isHorizontalScrollBarEnabled = false
        }
        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(6), dp(14), dp(6))
        }
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
                text = lbl; textSize = 12f; setTextColor(C.cyan)
                setPadding(dp(14), dp(7), dp(14), dp(7))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#08001A20"))
                    setStroke(dp(1), Color.parseColor("#2200F0FF"))
                    cornerRadius = dp(999).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, dp(8), 0) }
                setOnClickListener { submitText(query) }
            }
            chipsRow.addView(chip)
        }
        hScroll.addView(chipsRow)
        root.addView(hScroll, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.BELOW, statsRow.id) })

        // ── BOTTOM CONTROLS ──────────────────────────────────────────────────
        val bottomBar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(20))
        }

        // Input row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0A001520"))
                setStroke(dp(1), Color.parseColor("#1500F0FF"))
                cornerRadius = dp(30).toFloat()
            }
            setPadding(dp(14), dp(2), dp(6), dp(2))
        }
        etInput = EditText(this).apply {
            hint = "Command interface…"; setHintTextColor(C.mono)
            setTextColor(C.white); textSize = 14f; background = null
            typeface = Typeface.MONOSPACE
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_SEND) { sendInput(); true } else false
            }
        }
        val btnSend = TextView(this).apply {
            text = "→"; textSize = 18f; setTextColor(C.bg); gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(C.cyan)
            }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { setMargins(dp(6), dp(4), dp(4), dp(4)) }
            setOnClickListener { sendInput() }
        }
        inputRow.addView(etInput); inputRow.addView(btnSend)

        // Mic button (Marvel style)
        btnMic = TextView(this).apply {
            text = "🎙  ACTIVATE VOICE INTERFACE"
            textSize = 13f; setTextColor(C.bg)
            typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.1f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                colors = intArrayOf(C.cyan, Color.parseColor("#008899"))
                gradientType = GradientDrawable.LINEAR_GRADIENT
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = dp(40).toFloat()
            }
            setPadding(0, dp(16), 0, dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(10), 0, 0) }
            setOnClickListener { handleMic() }
        }

        val hint = TextView(this).apply {
            text = "◈  Powered by Gemini 2.0 Flash  ◈"
            textSize = 9f; setTextColor(C.mono); typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER; letterSpacing = 0.1f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(8), 0, 0) }
        }

        bottomBar.addView(inputRow)
        bottomBar.addView(btnMic)
        bottomBar.addView(hint)

        root.addView(bottomBar, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply { addRule(RelativeLayout.ALIGN_PARENT_BOTTOM) })

        // ── CHAT PANEL ───────────────────────────────────────────────────────
        chatScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#06FFFFFF"))
                setStroke(dp(1), Color.parseColor("#0F00F0FF"))
                cornerRadius = dp(14).toFloat()
            }
        }
        tvChat = TextView(this).apply {
            text = ""; textSize = 13f; setTextColor(C.white)
            typeface = Typeface.MONOSPACE; lineHeight = dp(22)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        chatScroll.addView(tvChat)

        root.addView(chatScroll, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, hScroll.id)
            addRule(RelativeLayout.ABOVE, bottomBar.id)
            setMargins(dp(14), dp(6), dp(14), dp(6))
        })

        startAnimations()
    }

    // ── HUD Grid Background ──────────────────────────────────────────────────
    inner class HudGridView(ctx: android.content.Context) : View(ctx) {
        private val paint = Paint().apply {
            color = Color.parseColor("#06001A30"); strokeWidth = 0.5f
        }
        override fun onDraw(canvas: Canvas) {
            val step = dp(28)
            var x = 0; while (x < width) { canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint); x += step }
            var y = 0; while (y < height) { canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint); y += step }
        }
    }

    // ── Animations ───────────────────────────────────────────────────────────
    private fun startAnimations() {
        // Orb pulse
        ObjectAnimator.ofFloat(orbCore, "scaleX", 1f, 1.08f, 1f).apply {
            duration = 2200; repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator(); start()
        }
        ObjectAnimator.ofFloat(orbCore, "scaleY", 1f, 1.08f, 1f).apply {
            duration = 2200; repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator(); start()
        }
        // Ring rotations
        ObjectAnimator.ofFloat(ring1, "rotation", 0f, 360f).apply {
            duration = 14000; repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator(); start()
        }
        ObjectAnimator.ofFloat(ring2, "rotation", 0f, -360f).apply {
            duration = 22000; repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator(); start()
        }
        // Glow pulse
        ObjectAnimator.ofFloat(glow, "alpha", 0.3f, 0.7f, 0.3f).apply {
            duration = 2800; repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator(); start()
        }
    }

    // ── Time ─────────────────────────────────────────────────────────────────
    private fun updateTime() {
        val cal = java.util.Calendar.getInstance()
        tvTime.text = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        handler.postDelayed({ updateTime() }, 30000)
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private fun setState(s: String) {
        state = s
        val (color, label, btnText, btnColor) = when (s) {
            "listening" -> arrayOf(C.green,  "LISTENING  //  VOICE ACTIVE",  "⏹  STOP",            C.red)
            "thinking"  -> arrayOf(C.purple, "PROCESSING  //  GEMINI ACTIVE", "⏳  PROCESSING…",    C.purple)
            "speaking"  -> arrayOf(C.gold,   "SPEAKING  //  OUTPUT ACTIVE",   "🔊  SPEAKING…",      C.gold)
            "error"     -> arrayOf(C.red,    "ERROR  //  CHECK CONNECTION",    "🎙  RETRY",          C.cyan)
            else        -> arrayOf(C.cyan,   "STANDBY  //  AWAITING INPUT",   "🎙  ACTIVATE VOICE INTERFACE", C.cyan)
        }
        tvState.setTextColor(color as Int)
        tvState.text = label as String
        btnMic.text = btnText as String
        btnMic.setTextColor(C.bg)
        (btnMic.background as? GradientDrawable)?.apply {
            colors = intArrayOf(btnColor as Int, adjustAlpha(btnColor, 0.6f))
            orientation = GradientDrawable.Orientation.LEFT_RIGHT
        }
        (orbCore.background as? GradientDrawable)?.colors = intArrayOf(color, adjustAlpha(color, 0.5f))
    }

    private fun adjustAlpha(color: Int, factor: Float) = Color.argb(
        (Color.alpha(color) * factor).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    // ── Chat ─────────────────────────────────────────────────────────────────
    private fun addChat(role: String, msg: String) {
        val prefix = if (role == "MJ") "🔵 MJ: " else "⚪ You: "
        chatLog.append("$prefix$msg\n\n")
        tvChat.text = chatLog
        handler.postDelayed({ chatScroll.fullScroll(View.FOCUS_DOWN) }, 80)
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────
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

    // ── Mic ─────────────────────────────────────────────────────────────────
    private fun handleMic() {
        when (state) {
            "thinking", "speaking" -> { Toast.makeText(this, "MJ is busy…", Toast.LENGTH_SHORT).show(); return }
            "listening" -> { setState("idle"); return }
        }
        if (!hasMicPerm()) { requestMicPerm(); return }
        startVoice(); setState("listening")
    }

    private fun startVoice() {
        val i = Intent(this, VoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }

    // ── Text ──────────────────────────────────────────────────────────────────
    private fun sendInput() {
        val t = etInput.text.toString().trim(); if (t.isEmpty()) return
        etInput.setText("")
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(etInput.windowToken, 0)
        submitText(t)
    }

    private fun submitText(text: String) {
        addChat("YOU", text); setState("thinking")
        gemini.fetchResponse(text) { resp ->
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
                addChat("MJ", clean); setState("idle")
            }
        }
    }

    // ── Permissions ────────────────────────────────────────────────────────────
    private fun hasMicPerm() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestMicPerm() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERM)
    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == PERM && gr.firstOrNull() == PackageManager.PERMISSION_GRANTED) handleMic()
        else Toast.makeText(this, "Microphone permission required!", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() { VoiceService.onStateChanged = null; VoiceService.onResponse = null; super.onDestroy() }
}
