package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.os.Build
import android.content.Intent
import android.net.Uri

class DetailsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val bgColor = if (isDark) Color.parseColor("#000000") else Color.parseColor("#F2F2F7")
        val cardBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")
        val cardBorderColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val primaryTextColor = if (isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#000000")
        val secondaryTextColor = if (isDark) Color.parseColor("#8E8E93") else Color.parseColor("#6C6C70")
        val accentColor = if (isDark) Color.parseColor("#3E82F7") else Color.parseColor("#0066FF")
        val backBtnBgColor = if (isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA")

        val statusBarHeight = getStatusBarHeight()
        val dpToPx = { dp: Float -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt() }

        val rootFrameLayout = FrameLayout(this).apply { setBackgroundColor(bgColor) }

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

        // ----- Banner Card -----
        val bannerCard = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(0, Color.TRANSPARENT)
            }
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val backgroundImage = ImageView(this).apply {
            val imageResId = resources.getIdentifier("hehejuicebanner", "drawable", packageName)
            if (imageResId != 0) {
                setImageResource(imageResId)
            } else {
                setBackgroundColor(accentColor)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(200f)
            )
        }
        bannerCard.addView(backgroundImage)

        val dimOverlay = View(this).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        bannerCard.addView(dimOverlay)

        val customFont = try {
            Typeface.createFromAsset(assets, "SamsungSharpSans-Bold.ttf")
        } catch (e: Exception) {
            Typeface.DEFAULT_BOLD
        }

        val titleText = TextView(this).apply {
            text = "OneUI Settings Hook"
            textSize = 28f
            setTextColor(Color.WHITE)
            setTypeface(customFont)
            gravity = Gravity.CENTER
            translationY = -dpToPx(3f).toFloat()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        bannerCard.addView(titleText)

        scrollContent.addView(bannerCard)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(16f)
            )
        }
        scrollContent.addView(spacer)

        // ----- Credits Card -----
        val creditsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val creditsTitle = TextView(this).apply {
            text = "Credits"
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(16f))
        }
        creditsCard.addView(creditsTitle)

        fun createCreditRow(label: String, username: String, onClick: () -> Unit): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(12f)
                }
                // IMPORTANT: set weightSum to distribute space
                weightSum = 1f

                // Left: Description (label)
                val labelView = TextView(context).apply {
                    text = label
                    textSize = 15f
                    setTextColor(secondaryTextColor)
                    // Width = 0, weight = 1 so it takes all remaining space
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                addView(labelView)

                // Right: Name (without "@")
                val valueView = TextView(context).apply {
                    text = username
                    textSize = 15f
                    setTextColor(accentColor)
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.END
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onClick() }
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.8f).setDuration(80).start()
                            }
                            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).start()
                            }
                        }
                        false
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                addView(valueView)

                // Divider
                val divider = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(1f)
                    ).apply {
                        topMargin = dpToPx(12f)
                    }
                    setBackgroundColor(cardBorderColor)
                }
                addView(divider)
            }
        }

        creditsCard.addView(createCreditRow(
            label = "Developer",
            username = "HeheJuice",
            onClick = { openTelegram("HeheJuice") }
        ))
        creditsCard.addView(createCreditRow(
            label = "Designer of PUI Theme",
            username = "FifthSnow",
            onClick = { openTelegram("FifthSnow") }
        ))
        creditsCard.addView(createCreditRow(
            label = "VI Language Helps",
            username = "ncatt",
            onClick = { openTelegram("ncatt") }
        ))
        creditsCard.addView(createCreditRow(
            label = "Method for Patching Android",
            username = "LSPosed",
            onClick = { openTelegram("LSPosed") }
        ))

        scrollContent.addView(creditsCard)

        scrollView.addView(scrollContent)
        rootFrameLayout.addView(scrollView)

        // ---------- TOP BAR ----------
        val topBarLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16f), statusBarHeight + dpToPx(12f), dpToPx(16f), dpToPx(12f))
        }

        val topBarTitle = TextView(this).apply {
            text = "Details"
            textSize = 16f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(backBtnBgColor)
                cornerRadius = dpToPx(100f).toFloat()
            }
            setPadding(dpToPx(20f), 0, dpToPx(20f), 0)
            alpha = 0f
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dpToPx(48f), Gravity.CENTER)
        }

        val backArrowDrawable = object : android.graphics.drawable.Drawable() {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryTextColor
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = dpToPx(2.5f).toFloat()
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            override fun draw(canvas: android.graphics.Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val size = dpToPx(6.5f)
                val path = android.graphics.Path().apply {
                    moveTo(cx + size * 0.4f, cy - size)
                    lineTo(cx - size * 0.5f, cy)
                    lineTo(cx + size * 0.4f, cy + size)
                }
                canvas.drawPath(path, paint)
            }
            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: android.graphics.ColorFilter?) { paint.colorFilter = cf }
            @Deprecated("Deprecated in Java") override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }

        val backBtn = android.widget.ImageView(this).apply {
            setImageDrawable(backArrowDrawable)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(backBtnBgColor)
            }
            contentDescription = "Back"
            isClickable = true
            isFocusable = true
            layoutParams = FrameLayout.LayoutParams(dpToPx(48f), dpToPx(48f), Gravity.START or Gravity.CENTER_VERTICAL)
            setOnClickListener { finish() }
            setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.85f).setDuration(120).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(350).start()
                    }
                }
                false
            }
        }

        topBarLayout.addView(topBarTitle)
        topBarLayout.addView(backBtn)
        rootFrameLayout.addView(topBarLayout)

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val alpha = (scrollY / dpToPx(40f).toFloat()).coerceIn(0f, 1f)
            topBarTitle.alpha = alpha
        }

        rootFrameLayout.setOnApplyWindowInsetsListener { _, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetTop
            }
            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars() or WindowInsets.Type.ime()).bottom
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            }
            val effectiveTop = if (topInset > 0) topInset else statusBarHeight

            topBarLayout.setPadding(dpToPx(16f), effectiveTop + dpToPx(12f), dpToPx(16f), dpToPx(12f))
            scrollView.setPadding(dpToPx(16f), effectiveTop + dpToPx(68f), dpToPx(16f), dpToPx(140f))
            insets
        }

        setContentView(rootFrameLayout)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else dpToPx(36f)
    }

    private fun dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

    private fun openTelegram(username: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$username"))
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username"))
                startActivity(intent)
            } catch (e2: Exception) {
                // ignore
            }
        }
    }
}