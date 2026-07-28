package com.HeheJuice.OneUISettingsHook

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
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
import android.os.Build
import android.os.Bundle
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

class SettingsActivity : Activity() {

    private var activeTab = 0 // 0 = Main, 1 = Advanced

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
        val secondaryBtnColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#E5E5EA")
        val backBtnBgColor = if (isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA")
        val inputBgColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7")
        val navBgColor = if (isDark) Color.parseColor("#1C1C1E") else Color.parseColor("#FFFFFF")

        val buttonHeightPx = dpToPx(54f)

        val deviceProtectedContext = createDeviceProtectedStorageContext()
        val prefs = deviceProtectedContext.getSharedPreferences("mod_settings", Context.MODE_PRIVATE)

        val rootFrameLayout = FrameLayout(this).apply { setBackgroundColor(bgColor) }
        val statusBarHeight = getStatusBarHeight()
        val navBarHeight = getNavigationBarHeight()

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_ALWAYS
            clipToPadding = false
            setPadding(dpToPx(16f), statusBarHeight + dpToPx(68f), dpToPx(16f), navBarHeight + dpToPx(80f))
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val advancedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            visibility = View.GONE
        }
        // --- MAIN TAB CARDS ---

        // Card 1: Custom Software Banner Text
        val bannerTextCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val bannerTitle = TextView(this).apply {
            text = getString(R.string.custom_banner_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val bannerSub = TextView(this).apply {
            text = getString(R.string.custom_banner_subtitle)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, dpToPx(4f), 0, dpToPx(16f))
        }

        val bannerInputEt = EditText(this).apply {
            setText(prefs.getString("custom_software_banner_text", ""))
            hint = getString(R.string.custom_banner_hint)
            setHintTextColor(secondaryTextColor)
            setTextColor(primaryTextColor)
            textSize = 15f
            background = GradientDrawable().apply {
                setColor(inputBgColor)
                cornerRadius = dpToPx(16f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(16f), dpToPx(14f), dpToPx(16f), dpToPx(14f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val saveBannerBtn = createAnimatedButton(getString(R.string.btn_save), Color.WHITE, accentColor, buttonHeightPx) {
            val enteredText = bannerInputEt.text.toString().trim()
            prefs.edit().putString("custom_software_banner_text", enteredText).apply()
            
            Thread {
                val safeText = enteredText.replace("'", "'\\''")
                val success = runRootCommands(listOf(
                    "settings put global custom_software_banner_text '$safeText'",
                    "am force-stop com.android.settings"
                ))
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_applied), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_root_permission_required), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(16f) }

        val resetBannerBtn = createAnimatedButton(getString(R.string.btn_reset_default), primaryTextColor, secondaryBtnColor, buttonHeightPx) {
            bannerInputEt.setText("")
            prefs.edit().remove("custom_software_banner_text").apply()
            
            Thread {
                val success = runRootCommands(listOf(
                    "settings delete global custom_software_banner_text",
                    "am force-stop com.android.settings"
                ))
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_reset_success), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(10f) }

        bannerTextCard.addView(bannerTitle)
        bannerTextCard.addView(bannerSub)
        bannerTextCard.addView(bannerInputEt)
        bannerTextCard.addView(saveBannerBtn)
        bannerTextCard.addView(resetBannerBtn)

        mainLayout.addView(bannerTextCard)

        val mainSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f)) }
        mainLayout.addView(mainSpacer)
        // --- ADVANCED TAB CARDS ---

        // Card 1: Lab Entrance Card (at Index 0)
        val labCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val labTitleTv = TextView(this).apply {
            text = getString(R.string.lab_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val labSubTv = TextView(this).apply {
            text = getString(R.string.lab_subtitle)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, dpToPx(4f), 0, dpToPx(16f))
        }

        val openLabBtn = createAnimatedButton(
            getString(R.string.btn_open_lab), 
            Color.WHITE, 
            accentColor, 
            buttonHeightPx
        ) {
            startActivity(Intent(this@SettingsActivity, LabActivity::class.java))
        }

        labCardLayout.addView(labTitleTv)
        labCardLayout.addView(labSubTv)
        labCardLayout.addView(openLabBtn)

        advancedLayout.addView(labCardLayout, 0)

        val labSpacer = View(this).apply { 
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f)) 
        }
        advancedLayout.addView(labSpacer, 1)

        // Card 2: Disable System Updates Card
        val disableUpdateCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val disableUpdateTitle = TextView(this).apply {
            text = getString(R.string.disable_ota_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val disableUpdateSub = TextView(this).apply {
            text = getString(R.string.disable_ota_subtitle)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, dpToPx(4f), 0, dpToPx(16f))
        }

        val disableUpdateBtn = createAnimatedButton(getString(R.string.btn_disable_ota), Color.WHITE, accentColor, buttonHeightPx) {
            showNoticeDialog(
                cardBgColor, cardBorderColor, primaryTextColor, secondaryTextColor,
                accentColor, secondaryBtnColor, buttonHeightPx
            ) {
                Thread {
                    val success = runRootCommands(listOf(
                        "pm disable com.sec.android.soagent",
                        "pm disable com.wssyncmldm"
                    ))
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this@SettingsActivity, getString(R.string.msg_ota_disabled), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SettingsActivity, getString(R.string.msg_root_permission_required), Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }

        disableUpdateCardLayout.addView(disableUpdateTitle)
        disableUpdateCardLayout.addView(disableUpdateSub)
        disableUpdateCardLayout.addView(disableUpdateBtn)

        advancedLayout.addView(disableUpdateCardLayout)

        val advSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f)) }
        advancedLayout.addView(advSpacer)

        // Card 3: Force Stop Settings App Card
        val forceStopCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val forceStopTitle = TextView(this).apply {
            text = getString(R.string.force_stop_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val forceStopSub = TextView(this).apply {
            text = getString(R.string.force_stop_subtitle)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, dpToPx(4f), 0, dpToPx(16f))
        }

        val forceStopBtn = createAnimatedButton(getString(R.string.btn_force_stop), primaryTextColor, secondaryBtnColor, buttonHeightPx) {
            Thread {
                val success = runRootCommands(listOf("am force-stop com.android.settings"))
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_settings_stopped), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_root_permission_required), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        forceStopCardLayout.addView(forceStopTitle)
        forceStopCardLayout.addView(forceStopSub)
        forceStopCardLayout.addView(forceStopBtn)

        advancedLayout.addView(forceStopCardLayout)

        scrollContent.addView(mainLayout)
        scrollContent.addView(advancedLayout)
        scrollView.addView(scrollContent)
        rootFrameLayout.addView(scrollView)
        // --- FLOATING TOP BAR ---
        val topBarLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16f), statusBarHeight + dpToPx(12f), dpToPx(16f), dpToPx(12f))
        }

        val topBarTitle = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 16f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(backBtnBgColor); cornerRadius = dpToPx(100f).toFloat() }
            setPadding(dpToPx(20f), 0, dpToPx(20f), 0)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dpToPx(48f), Gravity.CENTER)
        }

        val backArrowDrawable = object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryTextColor
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(2.5f).toFloat()
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val size = dpToPx(6.5f)
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

        val backBtn = ImageView(this).apply {
            setImageDrawable(backArrowDrawable)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(backBtnBgColor) }
            contentDescription = getString(R.string.btn_back)
            isClickable = true
            isFocusable = true
            layoutParams = FrameLayout.LayoutParams(dpToPx(48f), dpToPx(48f), Gravity.START or Gravity.CENTER_VERTICAL)
            setOnClickListener { finish() }
            setOnTouchListener(pressScaleTouchListener)
        }

        topBarLayout.addView(topBarTitle)
        topBarLayout.addView(backBtn)
        rootFrameLayout.addView(topBarLayout)

        // --- BOTTOM NAVIGATION BAR ---
        val bottomNavContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(navBgColor)
                cornerRadius = dpToPx(100f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(6f), dpToPx(6f), dpToPx(6f), dpToPx(6f))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(64f),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = navBarHeight + dpToPx(12f)
                marginStart = dpToPx(24f)
                marginEnd = dpToPx(24f)
            }
        }

        val mainTabBtn = TextView(this).apply {
            text = getString(R.string.tab_main)
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(accentColor); cornerRadius = dpToPx(100f).toFloat() }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            isClickable = true
            isFocusable = true
        }

        val advancedTabBtn = TextView(this).apply {
            text = getString(R.string.tab_advanced)
            textSize = 15f
            setTextColor(secondaryTextColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = null
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            isClickable = true
            isFocusable = true
        }

        fun switchTab(targetTab: Int) {
            if (activeTab == targetTab) return
            activeTab = targetTab

            if (targetTab == 0) {
                mainTabBtn.setTextColor(Color.WHITE)
                mainTabBtn.background = GradientDrawable().apply { setColor(accentColor); cornerRadius = dpToPx(100f).toFloat() }
                advancedTabBtn.setTextColor(secondaryTextColor)
                advancedTabBtn.background = null

                mainLayout.visibility = View.VISIBLE
                advancedLayout.visibility = View.GONE
                applyEntranceAnimations(listOf(bannerTextCard))
            } else {
                advancedTabBtn.setTextColor(Color.WHITE)
                advancedTabBtn.background = GradientDrawable().apply { setColor(accentColor); cornerRadius = dpToPx(100f).toFloat() }
                mainTabBtn.setTextColor(secondaryTextColor)
                mainTabBtn.background = null

                mainLayout.visibility = View.GONE
                advancedLayout.visibility = View.VISIBLE
                applyEntranceAnimations(listOf(labCardLayout, disableUpdateCardLayout, forceStopCardLayout))
            }
        }

        mainTabBtn.setOnClickListener { switchTab(0) }
        advancedTabBtn.setOnClickListener { switchTab(1) }

        bottomNavContainer.addView(mainTabBtn)
        bottomNavContainer.addView(advancedTabBtn)
        rootFrameLayout.addView(bottomNavContainer)

        rootFrameLayout.setOnApplyWindowInsetsListener { _, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetTop
            }
            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            }

            val effectiveTop = if (topInset > 0) topInset else statusBarHeight
            val effectiveBottom = if (bottomInset > 0) bottomInset else navBarHeight

            topBarLayout.setPadding(dpToPx(16f), effectiveTop + dpToPx(12f), dpToPx(16f), dpToPx(12f))
            (bottomNavContainer.layoutParams as FrameLayout.LayoutParams).bottomMargin = effectiveBottom + dpToPx(12f)
            scrollView.setPadding(dpToPx(16f), effectiveTop + dpToPx(68f), dpToPx(16f), effectiveBottom + dpToPx(88f))
            insets
        }

        setContentView(rootFrameLayout)
        applyEntranceAnimations(listOf(bannerTextCard))
    }

    // --- UTILITY METHODS ---

    private fun showNoticeDialog(
        cardBgColor: Int, cardBorderColor: Int, primaryTextColor: Int,
        secondaryTextColor: Int, accentColor: Int, secondaryBtnColor: Int,
        buttonHeightPx: Int, onConfirm: () -> Unit
    ) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(20f))
        }

        val titleTv = TextView(this).apply {
            text = getString(R.string.notice_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val messageTv = TextView(this).apply {
            text = getString(R.string.notice_message)
            textSize = 14f
            setTextColor(secondaryTextColor)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(10f), 0, dpToPx(20f))
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, buttonHeightPx)
        }

        val leaveBtn = createAnimatedButton(getString(R.string.btn_leave), primaryTextColor, secondaryBtnColor, LinearLayout.LayoutParams.MATCH_PARENT) {
            dialog.dismiss()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = dpToPx(6f) }
        }

        val continueBtn = createAnimatedButton(getString(R.string.btn_continue), Color.WHITE, accentColor, LinearLayout.LayoutParams.MATCH_PARENT) {
            dialog.dismiss()
            onConfirm()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dpToPx(6f) }
        }

        btnRow.addView(leaveBtn)
        btnRow.addView(continueBtn)
        cardLayout.addView(titleTv)
        cardLayout.addView(messageTv)
        cardLayout.addView(btnRow)

        dialog.setContentView(cardLayout)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.88).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun runRootCommands(commands: List<String>): Boolean {
        return try {
            val script = commands.joinToString(" ; ")
            val process = ProcessBuilder("su", "-c", script).start()
            Thread { process.inputStream.bufferedReader().use { it.readText() } }.start()
            Thread { process.errorStream.bufferedReader().use { it.readText() } }.start()
            process.waitFor() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private val pressScaleTouchListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).duration = 100
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).duration = 150
        }
        false
    }

    private fun createAnimatedButton(textStr: String, textColor: Int, bgColor: Int, height: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = textStr
            textSize = 15f
            setTextColor(textColor)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(bgColor); cornerRadius = dpToPx(100f).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            setOnTouchListener(pressScaleTouchListener)
        }
    }

    private fun applyEntranceAnimations(views: List<View>) {
        views.forEachIndexed { index, view ->
            view.translationY = dpToPx(40f).toFloat()
            view.alpha = 0f
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay((index * 60).toLong())
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else dpToPx(36f)
    }

    private fun getNavigationBarHeight(): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else dpToPx(48f)
    }

    private fun dpToPx(dp: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}
