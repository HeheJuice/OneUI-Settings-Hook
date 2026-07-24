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
import android.view.Window
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                     android.content.res.Configuration.UI_MODE_NIGHT_YES

        // OneUI 8.5 Color Palette
        val bgColor = if (isDark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        val cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        val cardBorderColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val primaryTextColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val secondaryTextColor = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#6C6C70")
        val accentColor = if (isDark) Color.parseColor("#3E82F7") else Color.parseColor("#0066FF")
        val secondaryBtnColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")

        // Detect module version programmatically
        val moduleVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(bgColor)
            isVerticalScrollBarEnabled = false
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(80), dpToPx(20), dpToPx(36))
        }

        // --- Header Title & Subtitle ---
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

        // ==========================================
        // CARD 1: Module Info & License
        // ==========================================
        val card1Drawable = GradientDrawable().apply {
            setColor(cardBgColor)
            cornerRadius = dpToPx(26).toFloat()
            setStroke(dpToPx(1), cardBorderColor)
        }

        val card1Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = card1Drawable
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        val versionTv = TextView(this).apply {
            text = "Version $moduleVersion"
            textSize = 16f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(4))
        }

        val cardDesc = TextView(this).apply {
            text = "All Xposed hooks are active and loaded into com.android.settings."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, dpToPx(20))
        }

        // Button 1: MIT License Button
        val licenseBtnDrawable = GradientDrawable().apply {
            setColor(accentColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val licenseBtn = TextView(this).apply {
            text = "View MIT License"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = licenseBtnDrawable
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook/blob/main/LICENSE")
                )
                startActivity(intent)
            }
        }

        // Button 2: GitHub Repository Link
        val githubBtnDrawable = GradientDrawable().apply {
            setColor(secondaryBtnColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val githubBtn = TextView(this).apply {
            text = "View GitHub Repository"
            textSize = 15f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = githubBtnDrawable
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Patch")
                )
                startActivity(intent)
            }
        }

        card1Layout.addView(versionTv)
        card1Layout.addView(cardDesc)
        card1Layout.addView(licenseBtn)
        card1Layout.addView(githubBtn)

        rootLayout.addView(card1Layout)

        // Spacer between cards
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(16)
            )
        }
        rootLayout.addView(spacer)

        // ==========================================
        // CARD 2: Developer Info
        // ==========================================
        val card2Drawable = GradientDrawable().apply {
            setColor(cardBgColor)
            cornerRadius = dpToPx(26).toFloat()
            setStroke(dpToPx(1), cardBorderColor)
        }

        val card2Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = card2Drawable
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }

        val aboutTitle = TextView(this).apply {
            text = "About Module"
            textSize = 18f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(16))
        }

        // Button: Developer Profile
        val makerBtnDrawable = GradientDrawable().apply {
            setColor(secondaryBtnColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val makerBtn = TextView(this).apply {
            text = "Developer: HeheJuice"
            textSize = 15f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = makerBtnDrawable
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/HeheJuice")
                )
                startActivity(intent)
            }
        }

        card2Layout.addView(aboutTitle)
        card2Layout.addView(makerBtn)

        rootLayout.addView(card2Layout)

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
