package com.felo.penguinsolver

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class SolverService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var button: TextView? = null
    private lateinit var lp: WindowManager.LayoutParams
    private val ui = Handler(Looper.getMainLooper())
    private val exec = Executors.newSingleThreadExecutor()
    private var busy = false

    private val prefs by lazy { getSharedPreferences("cfg", MODE_PRIVATE) }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "bubble") ui.post { applyBubble() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs.registerOnSharedPreferenceChangeListener(listener)
        applyBubble()
    }

    /** Bubble only exists while the "bubble" switch / Quick Settings tile is ON (default OFF). */
    private fun applyBubble() {
        val on = prefs.getBoolean("bubble", false)
        if (on && button == null) addButton()
        else if (!on) removeButton()
    }

    private fun removeButton() {
        button?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        button = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        removeButton()
        super.onDestroy()
    }

    private fun addButton() {
        val tv = TextView(this).apply {
            text = "SOLVE"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(36, 24, 36, 24)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E5007AFF")); cornerRadius = 60f
            }
        }
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 40; y = resources.displayMetrics.heightPixels * 78 / 100 }

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false; var downAt = 0L
        tv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false; downAt = System.currentTimeMillis() }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (Math.abs(dx) > 15 || Math.abs(dy) > 15) moved = true
                    if (moved) { lp.x = ox + dx.toInt(); lp.y = oy + dy.toInt(); wm.updateViewLayout(tv, lp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) {
                    if (System.currentTimeMillis() - downAt > 800) {
                        prefs.edit().putBoolean("bubble", false).apply()
                        toast("Solve bubble hidden")
                    } else solveNow()
                }
            }
            true
        }
        wm.addView(tv, lp)
        button = tv
    }

    private fun toast(s: String) = ui.post { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }

    private fun solveNow() {
        if (busy) return
        busy = true
        button?.visibility = View.INVISIBLE
        ui.postDelayed({
            takeScreenshot(Display.DEFAULT_DISPLAY, exec, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val hb = result.hardwareBuffer
                    val bmp = Bitmap.wrapHardwareBuffer(hb, result.colorSpace)
                        ?.copy(Bitmap.Config.ARGB_8888, false)
                    hb.close()
                    ui.post { button?.visibility = View.VISIBLE }
                    if (bmp == null) { toast("Screenshot failed"); busy = false; return }
                    process(bmp)
                }
                override fun onFailure(errorCode: Int) {
                    ui.post { button?.visibility = View.VISIBLE }
                    toast("Screenshot error $errorCode"); busy = false
                }
            })
        }, 200)
    }

    private fun process(bmp: Bitmap) {
        val board = BoardReader.read(bmp)
        if (board == null) { toast("Couldn't read the board. Open the level screen and try again."); busy = false; return }
        val sol = Solver.solve(board.regions, board.fixed)
        if (sol == null) { toast("No solution found (misread colors?)"); busy = false; return }

        val taps = prefs.getInt("taps", 2)
        val gap = prefs.getInt("gap", 250).toLong()
        val todo = ArrayList<Pair<Float, Float>>()
        for (r in 0 until board.n) {
            val c = sol[r]
            if (board.fixed.contains(Pair(r, c))) continue
            todo.add(Pair(board.centersX[c], board.centersY[r]))
        }
        toast("Solved ${board.n}x${board.n}: tapping ${todo.size} cells")
        var i = 0
        fun next() {
            if (i >= todo.size) { busy = false; return }
            val (x, y) = todo[i++]
            val b = GestureDescription.Builder()
            for (k in 0 until taps) {
                val p = Path().apply { moveTo(x, y) }
                b.addStroke(GestureDescription.StrokeDescription(p, k * 140L, 50L))
            }
            dispatchGesture(b.build(), object : GestureResultCallback() {
                override fun onCompleted(g: GestureDescription?) { ui.postDelayed({ next() }, gap) }
                override fun onCancelled(g: GestureDescription?) { ui.postDelayed({ next() }, gap) }
            }, null)
        }
        ui.post { next() }
    }
}
