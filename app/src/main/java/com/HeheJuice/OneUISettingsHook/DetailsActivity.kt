package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Color

class DetailsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F2F2F7"))
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }
        
        val title = TextView(this).apply {
            text = "Details"
            textSize = 24f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        
        val content = TextView(this).apply {
            text = "This is the details page.\nYou can add more content here."
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        
        layout.addView(title)
        layout.addView(content)
        setContentView(layout)
    }
}