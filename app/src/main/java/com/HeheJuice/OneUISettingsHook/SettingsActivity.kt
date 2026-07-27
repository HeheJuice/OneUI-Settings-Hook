package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

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
        val starBtnColor = if (isDark) Color.parseColor("#FF9F0A") else Color.parseColor("#FF9500")
        val redBtnColor = if (isDark) Color.parseColor("#FF453A") else Color.parseColor("#FF3B30")
        val backBtnBgColor = if (isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#E5E5EA")
        val inputBgColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7")

        val deviceProtectedContext = createDeviceProtectedStorageContext()
        val prefs = deviceProtectedContext.getSharedPreferences("mod_settings", Context.MODE_PRIVATE)

        val rawVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
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

        val buttonHeightPx = dpToPx(54f)

        // PAGE 1: MODULE INFO
        val moduleInfoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val headerCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val headerTitle = TextView(this).apply { text = getString(R.string.app_name); textSize = 28f; setTextColor(primaryTextColor) }
        val headerSub = TextView(this).apply { text = getString(R.string.header_subtitle); textSize = 15f; setTextColor(secondaryTextColor); setPadding(0, dpToPx(4f), 0, 0) }
        headerCardLayout.addView(headerTitle)
        headerCardLayout.addView(headerSub)
        moduleInfoLayout.addView(headerCardLayout)

        val headerSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f)) }
        moduleInfoLayout.addView(headerSpacer)

        val card1Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val versionTv = TextView(this).apply { text = getString(R.string.module_version, rawVersion); textSize = 17f; setTextColor(primaryTextColor); setPadding(0, 0, 0, dpToPx(18f)) }
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

        val card2Spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f)) }
        moduleInfoLayout.addView(card2Spacer)

        val card2Layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val aboutTitle = TextView(this).apply { text = getString(R.string.card_about_title); textSize = 18f; setTextColor(primaryTextColor); setPadding(0, 0, 0, dpToPx(18f)) }
        val makerBtn = createAnimatedButton(getString(R.string.btn_developer), primaryTextColor, secondaryBtnColor, buttonHeightPx) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice")))
        }

        val tgRowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, buttonHeightPx).apply { topMargin = dpToPx(12f) }
        }

        val tgChannelBtn = createAnimatedButton(getString(R.string.btn_tg_channel), primaryTextColor, secondaryBtnColor, LinearLayout.LayoutParams.MATCH_PARENT) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/channelhehejuice")))
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = dpToPx(6f) } }

        val tgChatBtn = createAnimatedButton(getString(R.string.btn_tg_chat), primaryTextColor, secondaryBtnColor, LinearLayout.LayoutParams.MATCH_PARENT) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sechehe")))
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginStart = dpToPx(6f) } }

        tgRowLayout.addView(tgChannelBtn)
        tgRowLayout.addView(tgChatBtn)
        card2Layout.addView(aboutTitle)
        card2Layout.addView(makerBtn)
        card2Layout.addView(tgRowLayout)
        moduleInfoLayout.addView(card2Layout)

        // PAGE 2: ADVANCED CONTAINER
        val advancedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Custom Banner Text Card
        val customTextCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val customTextTitle = TextView(this).apply {
            text = getString(R.string.custom_banner_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val customTextSub = TextView(this).apply {
            text = getString(R.string.custom_banner_subtitle)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, dpToPx(4f), 0, dpToPx(16f))
        }

        val bannerInputEt = EditText(this).apply {
            setText(prefs.getString("custom_banner_text", ""))
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

        val saveTextBtn = createAnimatedButton(getString(R.string.btn_save_banner_text), Color.WHITE, accentColor, buttonHeightPx) {
    AlertDialog.Builder(this@SettingsActivity)
        .setTitle(getString(R.string.notice_title))
        .setMessage(getString(R.string.notice_message))
        .setPositiveButton(getString(R.string.btn_continue)) { dialog, _ ->
            dialog.dismiss()
            
            val enteredText = bannerInputEt.text.toString().trim()
            prefs.edit().putString("custom_banner_text", enteredText).apply()
            
            Thread {
                val safeText = enteredText.replace("'", "'\\''")
                val success = runRootCommands(listOf(
                    "settings put global custom_oneui_banner '$safeText'",
                    "am force-stop com.android.settings"
                ))
                
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_banner_text_applied), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_root_permission_required), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
        .setNegativeButton(getString(R.string.btn_leave)) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(12f) }


        val resetTextBtn = createAnimatedButton(getString(R.string.btn_reset_default), primaryTextColor, secondaryBtnColor, buttonHeightPx) {
            bannerInputEt.setText("")
            prefs.edit().remove("custom_banner_text").apply()
            
            Thread {
                val success = runRootCommands(listOf(
                    "settings delete global custom_oneui_banner",
                    "am force-stop com.android.settings"
                ))
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@SettingsActivity, getString(R.string.msg_banner_text_reset_restarted), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(10f) }

        customTextCardLayout.addView(customTextTitle)
        customTextCardLayout.addView(customTextSub)
        customTextCardLayout.addView(bannerInputEt)
        customTextCardLayout.addView(saveTextBtn)
        customTextCardLayout.addView(resetTextBtn)
        advancedLayout.addView(customTextCardLayout)

        val textCardSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f)) }
        advancedLayout.addView(textCardSpacer)

        // Disclaimer Card Layout
        val disclaimerCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val disclaimerTitle = TextView(this).apply {
            text = getString(R.string.disclaimer_title)
            textSize = 18f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val disclaimerContent = TextView(this).apply {
            text = getString(R.string.disclaimer_content)
            textSize = 13.5f
            setTextColor(secondaryTextColor)
            setLineSpacing(4f, 1.1f)
            setPadding(0, dpToPx(8f), 0, 0)
        }

        disclaimerCardLayout.addView(disclaimerTitle)
        disclaimerCardLayout.addView(disclaimerContent)
        advancedLayout.addView(disclaimerCardLayout)

        val disclaimerSpacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(16f)) }
        advancedLayout.addView(disclaimerSpacer)

        val forceStopCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val advancedTitle = TextView(this).apply { text = getString(R.string.advanced_controls); textSize = 22f; setTextColor(primaryTextColor); setTypeface(null, Typeface.BOLD) }
        val advancedSub = TextView(this).apply { text = getString(R.string.advanced_subtitle); textSize = 14f; setTextColor(secondaryTextColor); setPadding(0, dpToPx(4f), 0, dpToPx(20f)) }
        val forceStopBtn = createAnimatedButton(getString(R.string.btn_force_stop_settings), Color.WHITE, redBtnColor, buttonHeightPx) { forceStopSettings() }

        forceStopCardLayout.addView(advancedTitle)
        forceStopCardLayout.addView(advancedSub)
        forceStopCardLayout.addView(forceStopBtn)
        advancedLayout.addView(forceStopCardLayout)

        scrollContent.addView(moduleInfoLayout)
        scrollContent.addView(advancedLayout)
        scrollView.addView(scrollContent)
        rootFrameLayout.addView(scrollView)

        // FLOATING TOP BAR
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
            alpha = 0f
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

        // BOTTOM BAR
        val bottomBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dpToPx(16f) }
        }

        val tabPillContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4f), dpToPx(4f), dpToPx(4f), dpToPx(4f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(100f).toFloat()
                setColor(cardBgColor)
                setStroke(dpToPx(1f), cardBorderColor)
            }
        }

        val activeTabBg = GradientDrawable().apply { setColor(secondaryBtnColor); cornerRadius = dpToPx(100f).toFloat() }

        val moduleInfoPillBtn = TextView(this).apply {
            text = getString(R.string.module_info_category)
            textSize = 14f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = activeTabBg
            setPadding(dpToPx(16f), 0, dpToPx(16f), 0)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(44f))
            setOnTouchListener(pressScaleTouchListener)
        }

        val advancedPillBtn = TextView(this).apply {
            text = getString(R.string.tab_advanced)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = null
            setPadding(dpToPx(16f), 0, dpToPx(16f), 0)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(44f))
            setOnTouchListener(pressScaleTouchListener)
        }

        val switchTab = { isModuleTab: Boolean ->
            if (isModuleTab) {
                moduleInfoLayout.visibility = View.VISIBLE
                advancedLayout.visibility = View.GONE
                moduleInfoPillBtn.background = activeTabBg
                moduleInfoPillBtn.setTextColor(primaryTextColor)
                advancedPillBtn.background = null
                advancedPillBtn.setTextColor(secondaryTextColor)
                applyEntranceAnimations(listOf(headerCardLayout, card1Layout, card2Layout))
            } else {
                moduleInfoLayout.visibility = View.GONE
                advancedLayout.visibility = View.VISIBLE
                advancedPillBtn.background = activeTabBg
                advancedPillBtn.setTextColor(primaryTextColor)
                moduleInfoPillBtn.background = null
                moduleInfoPillBtn.setTextColor(secondaryTextColor)
                applyEntranceAnimations(listOf(customTextCardLayout, disclaimerCardLayout, forceStopCardLayout))
            }
            scrollView.scrollTo(0, 0)
        }

        moduleInfoPillBtn.setOnClickListener { switchTab(true) }
        advancedPillBtn.setOnClickListener { switchTab(false) }
        tabPillContainer.addView(moduleInfoPillBtn)
        tabPillContainer.addView(advancedPillBtn)

        val searchIconDrawable = object : Drawable() {
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
                canvas.drawCircle(cx - dpToPx(1.5f).toFloat(), cy - dpToPx(1.5f).toFloat(), dpToPx(4.5f).toFloat(), paint)
                val handleOffset = dpToPx(3.2f).toFloat()
                val handleEnd = dpToPx(7f).toFloat()
                canvas.drawLine(cx + handleOffset, cy + handleOffset, cx + handleEnd, cy + handleEnd, paint)
            }
            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

        val searchCircleBtn = ImageView(this).apply {
            setImageDrawable(searchIconDrawable)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cardBgColor); setStroke(dpToPx(1f), cardBorderColor) }
            contentDescription = "Search"
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(dpToPx(52f), dpToPx(52f)).apply { marginStart = dpToPx(8f) }
            setOnClickListener {
                val intent = Intent().apply {
                    component = ComponentName("com.android.settings.intelligence", "com.android.settings.intelligence.search.SearchActivity")
                }
                startActivity(intent)
            }
            setOnTouchListener(pressScaleTouchListener)
        }

        bottomBarLayout.addView(tabPillContainer)
        bottomBarLayout.addView(searchCircleBtn)
        rootFrameLayout.addView(bottomBarLayout)

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
            (bottomBarLayout.layoutParams as FrameLayout.LayoutParams).bottomMargin = dpToPx(16f) + bottomInset
            insets
        }

        setContentView(rootFrameLayout)
        applyEntranceAnimations(listOf(headerCardLayout, card1Layout, card2Layout))
    }

    private fun forceStopSettings() {
        Thread {
            val success = runRootCommands(listOf("am force-stop com.android.settings"))
            runOnUiThread {
                if (success) {
                    Toast.makeText(this@SettingsActivity, getString(R.string.msg_settings_force_stopped), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, getString(R.string.msg_root_permission_required), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
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
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                .start()
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else dpToPx(36f)
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return Math.round(dp * context.resources.displayMetrics.density)
    }

    private fun dpToPx(dp: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    private fun dpToPx(dp: Int): Int = dpToPx(dp.toFloat())
}