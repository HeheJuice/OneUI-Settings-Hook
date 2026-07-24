package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                     android.content.res.Configuration.UI_MODE_NIGHT_YES

        // Color palette mimicking OneUI 8.5
        val bgColor = if (isDark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        val cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        val cardBorderColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val primaryTextColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val secondaryTextColor = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#6C6C70")
        val accentColor = if (isDark) Color.parseColor("#3E82F7") else Color.parseColor("#0066FF")

        // Main Scroll View
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(bgColor)
            isVerticalScrollBarEnabled = false
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(36), dpToPx(20), dpToPx(36))
        }

        // --- OneUI 8.5 Header (Thumb-friendly large header) ---
        val headerTitle = TextView(this).apply {
            text = "OneUI Settings Hook"
            textSize = 28f
            setTextColor(primaryTextColor)
            setPadding(dpToPx(8), 0, dpToPx(8), dpToPx(4))
        }

        val headerSub = TextView(this).apply {
            text = "Module configuration & information"
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(dpToPx(8), 0, dpToPx(8), dpToPx(24))
        }

        rootLayout.addView(headerTitle)
        rootLayout.addView(headerSub)

        // --- OneUI 8.5 Card Container (Rounded Card with subtle border) ---
        val cardDrawable = GradientDrawable().apply {
            setColor(cardBgColor)
            cornerRadius = dpToPx(26).toFloat() // OneUI standard 26dp radius
            setStroke(dpToPx(1), cardBorderColor) // Subtle outline
        }

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardDrawable
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        // Card Content
        val cardTitle = TextView(this).apply {
            text = "Status: Active"
            textSize = 18f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(6))
        }

        val cardDesc = TextView(this).apply {
            text = "All Xposed hooks are active and loaded into com.android.settings."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, dpToPx(20))
        }

        // Pill / Capsule Action Button
        val buttonDrawable = GradientDrawable().apply {
            setColor(accentColor)
            cornerRadius = dpToPx(100).toFloat() // Pill shape
        }

        val githubBtn = TextView(this).apply {
            text = "View GitHub Repository"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = buttonDrawable
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Patch")
                )
                startActivity(intent)
            }
        }

        cardLayout.addView(cardTitle)
        cardLayout.addView(cardDesc)
        cardLayout.addView(githubBtn)

        rootLayout.addView(cardLayout)
        scrollView.addView(rootLayout)

        setContentView(scrollView)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
