package com.HeheJuice.OneUISettingsHook

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.security.MessageDigest

private fun hashPassword(input: String): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

class SettingsActivity : Activity() {

    companion object {
        private const val STYLE_DEFAULT = 0
        private const val STYLE_MONET = 1
        private const val STYLE_ONEUI6 = 2
    }

    private lateinit var themeSelectionRow: LinearLayout
    private var currentStyle = STYLE_DEFAULT
    private var primaryTextColor: Int = 0
    private var secondaryTextColor: Int = 0
    private var accentColor: Int = 0
    private var inputBgColor: Int = 0
    private var cardBgColor: Int = 0
    private var cardBorderColor: Int = 0
    private var secondaryBtnColor: Int = 0
    private var redBtnColor: Int = 0
    private var backBtnBgColor: Int = 0
    private var buttonHeightPx: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val bgColor = if (isDark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        cardBorderColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        primaryTextColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        secondaryTextColor = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#6C6C70")
        accentColor = if (isDark) Color.parseColor("#3E82F7") else Color.parseColor("#0066FF")
        secondaryBtnColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val starBtnColor = if (isDark) Color.parseColor("#FF9F0A") else Color.parseColor("#FF9500")
        redBtnColor = if (isDark) Color.parseColor("#FF453A") else Color.parseColor("#FF3B30")
        backBtnBgColor = if (isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA")
        inputBgColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7")

        buttonHeightPx = dpToPx(54f)

        val prefs = getSharedPreferences("mod_settings", Context.MODE_PRIVATE)
        makePrefsWorldReadable()

        val rawVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        // --- DIALOG LAUNCH SEQUENCE (PASSWORD ONLY FOR DEBUG BUILDS) ---
        if (rawVersion.contains("Debug", ignoreCase = true)) {
            showDebugWarningDialog(
                cardBgColor, cardBorderColor, primaryTextColor, secondaryTextColor,
                redBtnColor, secondaryBtnColor, buttonHeightPx
            ) {
                showPasswordProtectionDialog(
                    cardBgColor, cardBorderColor, primaryTextColor, secondaryTextColor,
                    accentColor, secondaryBtnColor, inputBgColor, buttonHeightPx
                ) {
                    // Password verified -> enters main Settings UI
                }
            }
        }

        val rootFrameLayout = FrameLayout(this).apply { setBackgroundColor(bgColor) }
        val statusBarHeight = getStatusBarHeight()

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_ALWAYS
            clipToPadding = false
            setPadding(dpToPx(16f), statusBarHeight + dpToPx(68f), dpToPx(16f), dpToPx(180f))
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        // ---------- PAGE 1: MODULE INFO ----------
        val moduleInfoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Header Card
        val headerCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }
        val headerTitle = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTextColor(primaryTextColor)
        }
        val headerSub = TextView(this).apply {
            text = getString(R.string.header_subtitle)
            textSize = 15f
            setTextColor(secondaryTextColor)
            setPadding(0, dpToPx(4f), 0, 0)
        }
        headerCardLayout.addView(headerTitle)
        headerCardLayout.addView(headerSub)
        moduleInfoLayout.addView(headerCardLayout)

        moduleInfoLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f))
        })

        // Card 1 – Module Actions
        val card1Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }
        val versionTv = TextView(this).apply {
            text = getString(R.string.module_version, rawVersion)
            textSize = 17f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(18f))
        }
        val licenseBtn = createAnimatedButton(getString(R.string.btn_mit_license), Color.WHITE, accentColor, buttonHeightPx) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook/blob/main/LICENSE")))
        }
        val starBtn = createAnimatedButton(getString(R.string.btn_star_repo), Color.WHITE, starBtnColor, buttonHeightPx) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook")))
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(12f) }
        val githubBtn = createAnimatedButton(getString(R.string.btn_github_repo), primaryTextColor, secondaryBtnColor, buttonHeightPx) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook")))
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(12f) }
        val bugBtn = createAnimatedButton(getString(R.string.btn_report_bugs), Color.WHITE, redBtnColor, buttonHeightPx) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook/issues")))
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(12f) }

        card1Layout.addView(versionTv)
        card1Layout.addView(licenseBtn)
        card1Layout.addView(starBtn)
        card1Layout.addView(githubBtn)
        card1Layout.addView(bugBtn)
        moduleInfoLayout.addView(card1Layout)

        moduleInfoLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f))
        })

        // Card 2 – About
        val card2Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }
        val aboutTitle = TextView(this).apply {
            text = getString(R.string.card_about_title)
            textSize = 18f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(18f))
        }
        val makerBtn = createAnimatedButton(getString(R.string.btn_developer), primaryTextColor, secondaryBtnColor, buttonHeightPx) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice")))
        }
        val tgRowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, buttonHeightPx).apply {
                topMargin = dpToPx(12f)
            }
        }
        val tgChannelBtn = createAnimatedButton(getString(R.string.btn_tg_channel), primaryTextColor, secondaryBtnColor, LinearLayout.LayoutParams.MATCH_PARENT) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/channelhehejuice")))
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dpToPx(6f)
            }
        }
        val tgChatBtn = createAnimatedButton(getString(R.string.btn_tg_chat), primaryTextColor, secondaryBtnColor, LinearLayout.LayoutParams.MATCH_PARENT) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sechehe")))
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dpToPx(6f)
            }
        }
        tgRowLayout.addView(tgChannelBtn)
        tgRowLayout.addView(tgChatBtn)
        card2Layout.addView(aboutTitle)
        card2Layout.addView(makerBtn)
        card2Layout.addView(tgRowLayout)
        moduleInfoLayout.addView(card2Layout)

        // ---------- PAGE 2: ADVANCED ----------
        val advancedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // ----- Dashboard Style Card (3 options) -----
        val dashboardStyleCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val dashboardTitle = TextView(this).apply {
            text = getString(R.string.dashboard_theme_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(16f))
        }

        themeSelectionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            weightSum = 3f
        }

        currentStyle = prefs.getInt("dashboard_style", STYLE_DEFAULT)

        // Create the three options using the member function createStyleOption
        val defaultOption = createStyleOption(
            getString(R.string.dashboard_option_default),
            R.drawable.icon_default,
            STYLE_DEFAULT
        )
        val monetOption = createStyleOption("Monet", R.drawable.icon_monet, STYLE_MONET)
        val oneui6Option = createStyleOption("OneUI 6 Monet", R.drawable.icon_oneui6, STYLE_ONEUI6)

        themeSelectionRow.addView(defaultOption)
        themeSelectionRow.addView(monetOption)
        themeSelectionRow.addView(oneui6Option)

        dashboardStyleCardLayout.addView(dashboardTitle)
        dashboardStyleCardLayout.addView(themeSelectionRow)
        advancedLayout.addView(dashboardStyleCardLayout)

        advancedLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f))
        })