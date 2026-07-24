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

        // OneUI 8.5 Color Palette
        val bgColor = if (isDark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        val cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        val cardBorderColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val primaryTextColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val secondaryTextColor = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#6C6C70")
        val accentColor = if (isDark) Color.parseColor("#3E82F7") else Color.parseColor("#0066FF")
        val secondaryBtnColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val starBtnColor = if (isDark) Color.parseColor("#FF9F0A") else Color.parseColor("#FF9500")
        val redBtnColor = if (isDark) Color.parseColor("#FF453A") else Color.parseColor("#FF3B30")

        // Detect module version programmatically
        val rawVersion = try {
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
            setPadding(dpToPx(16), dpToPx(20), dpToPx(16), dpToPx(36))
        }

        // --- OneUI Back Arrow Vector Drawable ---
        val backArrowDrawable = object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryTextColor
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(2).toFloat()
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val size = dpToPx(6).toFloat()

                val path = Path().apply {
                    moveTo(cx + size * 0.5f, cy - size)
                    lineTo(cx - size * 0.5f, cy)
                    lineTo(cx + size * 0.5f, cy + size)
                }
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

        // --- Back Button (Top Action Bar) ---
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)

        val backBtn = ImageView(this).apply {
            setImageDrawable(backArrowDrawable)
            setBackgroundResource(outValue.resourceId)
            contentDescription = getString(R.string.btn_back)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                bottomMargin = dpToPx(12)
            }
            setOnClickListener {
                finish()
            }
        }

        rootLayout.addView(backBtn)

        // --- Header Title & Subtitle ---
        val headerTitle = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTextColor(primaryTextColor)
            setPadding(dpToPx(8), 0, dpToPx(8), dpToPx(4))
        }

        val headerSub = TextView(this).apply {
            text = getString(R.string.header_subtitle)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(dpToPx(8), 0, dpToPx(8), dpToPx(24))
        }

        rootLayout.addView(headerTitle)
        rootLayout.addView(headerSub)

        // ==========================================
        // CARD 1: Module Info & Quick Actions
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
            text = getString(R.string.module_version, rawVersion)
            textSize = 16f
            setTextColor(primaryTextColor)
            setPadding(0, 0, 0, dpToPx(16))
        }

        // Button 1: MIT License Button
        val licenseBtnDrawable = GradientDrawable().apply {
            setColor(accentColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val licenseBtn = TextView(this).apply {
            text = getString(R.string.btn_mit_license)
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

        // Button 2: Star Repo Button
        val starBtnDrawable = GradientDrawable().apply {
            setColor(starBtnColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val starBtn = TextView(this).apply {
            text = getString(R.string.btn_star_repo)
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = starBtnDrawable
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
                    Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook")
                )
                startActivity(intent)
            }
        }

        // Button 3: GitHub Repository Link
        val githubBtnDrawable = GradientDrawable().apply {
            setColor(secondaryBtnColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val githubBtn = TextView(this).apply {
            text = getString(R.string.btn_github_repo)
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
                    Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook")
                )
                startActivity(intent)
            }
        }

        // Button 4: Red Report Bugs Button
        val bugBtnDrawable = GradientDrawable().apply {
            setColor(redBtnColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val bugBtn = TextView(this).apply {
            text = getString(R.string.btn_report_bugs)
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = bugBtnDrawable
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
                    Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Hook/issues")
                )
                startActivity(intent)
            }
        }

        card1Layout.addView(versionTv)
        card1Layout.addView(licenseBtn)
        card1Layout.addView(starBtn)
        card1Layout.addView(githubBtn)
        card1Layout.addView(bugBtn)

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
        // CARD 2: Developer & Community Info
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
            text = getString(R.string.card_about_title)
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
            text = getString(R.string.btn_developer)
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

        // --- Telegram Buttons Row (Side-by-Side) ---
        val tgRowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
        }

        val tgChannelDrawable = GradientDrawable().apply {
            setColor(secondaryBtnColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val tgChannelBtn = TextView(this).apply {
            text = getString(R.string.btn_tg_channel)
            textSize = 14f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = tgChannelDrawable
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dpToPx(5)
            }
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/channelhehejuice")
                )
                startActivity(intent)
            }
        }

        val tgChatDrawable = GradientDrawable().apply {
            setColor(secondaryBtnColor)
            cornerRadius = dpToPx(100).toFloat()
        }

        val tgChatBtn = TextView(this).apply {
            text = getString(R.string.btn_tg_chat)
            textSize = 14f
            setTextColor(primaryTextColor)
            gravity = Gravity.CENTER
            background = tgChatDrawable
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(5)
            }
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/sechehe")
                )
                startActivity(intent)
            }
        }

        tgRowLayout.addView(tgChannelBtn)
        tgRowLayout.addView(tgChatBtn)

        card2Layout.addView(aboutTitle)
        card2Layout.addView(makerBtn)
        card2Layout.addView(tgRowLayout)

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
