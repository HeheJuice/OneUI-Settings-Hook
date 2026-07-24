package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
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

        // OneUI Color Palette
        val bgColor = if (isDark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        val cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        val cardBorderColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val primaryTextColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val secondaryTextColor = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#6C6C70")
        val accentColor = if (isDark) Color.parseColor("#3E82F7") else Color.parseColor("#0066FF")
        val secondaryBtnColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val starBtnColor = if (isDark) Color.parseColor("#FF9F0A") else Color.parseColor("#FF9500")
        val redBtnColor = if (isDark) Color.parseColor("#FF453A") else Color.parseColor("#FF3B30")
        
        // Translucent dark circle background for back button so cards can be seen scrolling underneath
        val backBtnBgColor = if (isDark) Color.parseColor("#CC3A3A3C") else Color.parseColor("#CCE5E5EA")

        val rawVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        // Root container overlaying fixed top bar & scrollable card body
        val rootFrameLayout = FrameLayout(this).apply {
            setBackgroundColor(bgColor)
        }

        // --- Scrollable Card Container ---
        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isFillViewport = true
            clipToPadding = false // Allows card content to scroll cleanly under back button
            // Top padding (68dp) leaves room for floating back button at start position
            setPadding(dpToPx(16), dpToPx(68), dpToPx(16), dpToPx(32))
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // --- Header Title & Subtitle ---
        val headerTitle = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 30f
            setTextColor(primaryTextColor)
            setPadding(dpToPx(4), 0, dpToPx(4), dpToPx(4))
        }

        val headerSub = TextView(this).apply {
            text = getString(R.string.header_subtitle)
            textSize = 15f
            setTextColor(secondaryTextColor)
            setPadding(dpToPx(4), 0, dpToPx(4), dpToPx(24))
        }

        scrollContent.addView(headerTitle)
        scrollContent.addView(headerSub)

        val buttonHeightPx = dpToPx(54)

        // ==========================================
        // CARD 1: Module Info & Quick Actions
        // ==========================================
        val card1Drawable = GradientDrawable().apply {
            setColor(cardBgColor)
            cornerRadius = dpToPx(28).toFloat()
            setStroke(dpToPx(1), cardBorderColor)
        }

        val card1Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = card1Drawable
            setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(24))
        }

        val versionTv = TextView(this).apply {
            text = getString(R.string.module_version, rawVersion)
            textSize = 17f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(18))
        }

        // Button 1: MIT License
        val licenseBtn = TextView(this).apply {
            text = getString(R.string.btn_mit_license)
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(accentColor)
                cornerRadius = dpToPx(100).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook/blob/main/LICENSE")))
            }
        }

        // Button 2: Star Repo
        val starBtn = TextView(this).apply {
            text = getString(R.string.btn_star_repo)
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(starBtnColor)
                cornerRadius = dpToPx(100).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            ).apply {
                topMargin = dpToPx(12)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook")))
            }
        }

        // Button 3: GitHub Repo
        val githubBtn = TextView(this).apply {
            text = getString(R.string.btn_github_repo)
            textSize = 15f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(secondaryBtnColor)
                cornerRadius = dpToPx(100).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            ).apply {
                topMargin = dpToPx(12)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook")))
            }
        }

        // Button 4: Report Bugs
        val bugBtn = TextView(this).apply {
            text = getString(R.string.btn_report_bugs)
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(redBtnColor)
                cornerRadius = dpToPx(100).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            ).apply {
                topMargin = dpToPx(12)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook/issues")))
            }
        }

        card1Layout.addView(versionTv)
        card1Layout.addView(licenseBtn)
        card1Layout.addView(starBtn)
        card1Layout.addView(githubBtn)
        card1Layout.addView(bugBtn)

        scrollContent.addView(card1Layout)

        // Dynamic Spacer between cards
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(16)
            )
        }
        scrollContent.addView(spacer)

        // ==========================================
        // CARD 2: Developer & Community Info
        // ==========================================
        val card2Drawable = GradientDrawable().apply {
            setColor(cardBgColor)
            cornerRadius = dpToPx(28).toFloat()
            setStroke(dpToPx(1), cardBorderColor)
        }

        val card2Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = card2Drawable
            setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(24))
        }

        val aboutTitle = TextView(this).apply {
            text = getString(R.string.card_about_title)
            textSize = 18f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(18))
        }

        // Button: Developer Profile
        val makerBtn = TextView(this).apply {
            text = getString(R.string.btn_developer)
            textSize = 15f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(secondaryBtnColor)
                cornerRadius = dpToPx(100).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice")))
            }
        }

        // Telegram Row
        val tgRowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                buttonHeightPx
            ).apply {
                topMargin = dpToPx(12)
            }
        }

        val tgChannelBtn = TextView(this).apply {
            text = getString(R.string.btn_tg_channel)
            textSize = 14f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(secondaryBtnColor)
                cornerRadius = dpToPx(100).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dpToPx(6)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/channelhehejuice")))
            }
        }

        val tgChatBtn = TextView(this).apply {
            text = getString(R.string.btn_tg_chat)
            textSize = 14f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(secondaryBtnColor)
                cornerRadius = dpToPx(100).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dpToPx(6)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sechehe")))
            }
        }

        tgRowLayout.addView(tgChannelBtn)
        tgRowLayout.addView(tgChatBtn)

        card2Layout.addView(aboutTitle)
        card2Layout.addView(makerBtn)
        card2Layout.addView(tgRowLayout)

        scrollContent.addView(card2Layout)

        scrollView.addView(scrollContent)
        rootFrameLayout.addView(scrollView)

        // ==========================================
        // FLOATING TOP BAR (Fixed Floating Back Button)
        // ==========================================
        val topBarLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
        }

        // Vector Arrow Drawable
        val backArrowDrawable = object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryTextColor
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(2.2f).toFloat()
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val size = dpToPx(5.5f).toFloat()

                val path = Path().apply {
                    moveTo(cx + size * 0.4f, cy - size)
                    lineTo(cx - size * 0.5f, cy)
                    lineTo(cx + size * 0.4f, cy + size)
                }
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

        val backBtnBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(backBtnBgColor)
        }

        val backBtn = ImageView(this).apply {
            setImageDrawable(backArrowDrawable)
            background = backBtnBackground
            contentDescription = getString(R.string.btn_back)
            isClickable = true
            isFocusable = true
            layoutParams = FrameLayout.LayoutParams(dpToPx(42), dpToPx(42))
            setOnClickListener { finish() }
        }

        topBarLayout.addView(backBtn)
        rootFrameLayout.addView(topBarLayout)

        setContentView(rootFrameLayout)
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(dp: Int): Int = dpToPx(dp.toFloat())
}
