package com.felo.penguinsolver

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }

        root.addView(TextView(this).apply { text = "Penguin Solver"; textSize = 26f; setTextColor(Color.BLACK) })
        root.addView(TextView(this).apply {
            textSize = 15f
            setPadding(0, pad / 2, 0, pad / 2)
            text = "1. Enable \"Penguin Solver\" in Accessibility (button below).\n" +
                    "2. Turn ON the switch below, or add the Penguin Solver tile to Quick Settings and toggle it there.\n" +
                    "3. Open the game on a level and tap the blue SOLVE bubble (drag it anywhere). Long-press it to hide it."
        })
        root.addView(Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        root.addView(Switch(this).apply {
            text = "Show SOLVE bubble on screen"
            textSize = 16f
            setPadding(0, pad / 2, 0, pad / 2)
            isChecked = prefs.getBoolean("bubble", false)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("bubble", on).apply() }
        })

        root.addView(TextView(this).apply { text = "\nTaps per cell"; textSize = 16f; setTextColor(Color.BLACK) })
        val rg = RadioGroup(this)
        val r1 = RadioButton(this).apply { text = "1 tap places a penguin"; id = 1 }
        val r2 = RadioButton(this).apply { text = "2 taps (1st = X, 2nd = penguin)"; id = 2 }
        rg.addView(r1); rg.addView(r2)
        rg.check(if (prefs.getInt("taps", 2) == 1) 1 else 2)
        rg.setOnCheckedChangeListener { _, id -> prefs.edit().putInt("taps", id).apply() }
        root.addView(rg)

        root.addView(TextView(this).apply { text = "\nDelay between cells: 250 ms (default)"; textSize = 14f })
        root.addView(SeekBar(this).apply {
            max = 800; progress = prefs.getInt("gap", 250)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    prefs.edit().putInt("gap", p.coerceAtLeast(60)).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
