package com.HeheJuice.OneUISettingsHook

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
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

class LabActivity : Activity() {

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
        val warningBgColor = if (isDark) Color.parseColor("#2C2000") else Color.parseColor("#FFF9E6")
        val warningBorderColor = if (isDark) Color.parseColor("#5C4300") else Color.parseColor("#FFE082")
        val warningTextColor = if (isDark) Color.parseColor("#FFCC00") else Color.parseColor("#B87700")

        val buttonHeightPx = dpToPx(54f)

        val deviceProtectedContext = createDeviceProtectedStorageContext()
        val prefs = deviceProtectedContext.getSharedPreferences("mod_settings", Context.MODE_PRIVATE)

        val rootFrameLayout = FrameLayout(this).apply { setBackgroundColor(bgColor) }
        val statusBarHeight = getStatusBarHeight()

        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_ALWAYS
            clipToPadding = false
            setPadding(dpToPx(16f), statusBarHeight + dpToPx(68f), dpToPx(16f), dpToPx(40f))
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }

        // --- TOP NOTICE CARD ---
        val noticeCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(warningBgColor)
                cornerRadius = dpToPx(24f).toFloat()
                setStroke(dpToPx(1f), warningBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(20f), dpToPx(20f), dpToPx(20f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(16f)
            }
        }

        val noticeTitleTv = TextView(this).apply {
            text = "⚠️ ${getString(R.string.lab_warning_title)}"
            textSize = 16f
            setTextColor(warningTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val noticeMessageTv = TextView(this).apply {
            text = getString(R.string.lab_warning_message)
            textSize = 14f
            setTextColor(primaryTextColor)
            setLineSpacing(4f, 1.1f)
            setPadding(0, dpToPx(6f), 0, 0)
        }

        noticeCardLayout.addView(noticeTitleTv)
        noticeCardLayout.addView(noticeMessageTv)
        scrollContent.addView(noticeCardLayout)

        // --- CUSTOM PRODUCT NAME CARD (EXPANDABLE) ---
        val customProductNameCardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dpToPx(28f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(20f), dpToPx(24f), dpToPx(20f), dpToPx(24f))
        }

        val productNameHeaderLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val productNameTitleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val productNameTitle = TextView(this).apply {
            text = getString(R.string.custom_product_name_title)
            textSize = 20f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val productNameSub = TextView(this).apply {
            text = getString(R.string.custom_product_name_subtitle)
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, dpToPx(4f), 0, dpToPx(8f))
        }

        productNameTitleContainer.addView(productNameTitle)
        productNameTitleContainer.addView(productNameSub)

        val productNameExpandIconDrawable = object : Drawable() {
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
                val size = dpToPx(6f).toFloat()
                val path = Path().apply {
                    moveTo(cx - size, cy - size / 3f)
                    lineTo(cx, cy + size / 1.5f)
                    lineTo(cx + size, cy - size / 3f)
                }
                canvas.drawPath(path, paint)
            }
            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

        val productNameExpandBtn = ImageView(this).apply {
            setImageDrawable(productNameExpandIconDrawable)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(inputBgColor) 
            }
            isClickable = true
            isFocusable = true
            setPadding(dpToPx(8f), dpToPx(8f), dpToPx(8f), dpToPx(8f))
            layoutParams = LinearLayout.LayoutParams(dpToPx(36f), dpToPx(36f)).apply {
                marginStart = dpToPx(12f)
            }
        }

        productNameHeaderLayout.addView(productNameTitleContainer)
        productNameHeaderLayout.addView(productNameExpandBtn)
        customProductNameCardLayout.addView(productNameHeaderLayout)

        val productNameExpandableContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            visibility = View.GONE
        }

        val productNameDisclaimerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(inputBgColor)
                cornerRadius = dpToPx(18f).toFloat()
                setStroke(dpToPx(1f), cardBorderColor)
            }
            setPadding(dpToPx(16f), dpToPx(16f), dpToPx(16f), dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(8f)
                bottomMargin = dpToPx(16f)
            }
        }

        val productNameDisclaimerTitle = TextView(this).apply {
            text = "⚠️ ${getString(R.string.disclaimer_title)}"
            textSize = 15f
            setTextColor(primaryTextColor)
            setTypeface(null, Typeface.BOLD)
        }

        val productNameDisclaimerContent = TextView(this).apply {
            text = getString(R.string.disclaimer_content)
            textSize = 13f
            setTextColor(secondaryTextColor)
            setLineSpacing(4f, 1.1f)
            setPadding(0, dpToPx(6f), 0, 0)
        }

        productNameDisclaimerCard.addView(productNameDisclaimerTitle)
        productNameDisclaimerCard.addView(productNameDisclaimerContent)

        val productNameInputEt = EditText(this).apply {
            setText(prefs.getString("custom_product_name", ""))
            hint = getString(R.string.custom_product_name_hint)
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

        val saveProductNameBtn = createAnimatedButton(getString(R.string.btn_save_product_name), Color.WHITE, accentColor, buttonHeightPx) {
            showNoticeDialog(
                cardBgColor, cardBorderColor, primaryTextColor, secondaryTextColor,
                accentColor, secondaryBtnColor, buttonHeightPx
            ) {
                val enteredText = productNameInputEt.text.toString().trim()
                prefs.edit().putString("custom_product_name", enteredText).apply()
                
                Thread {
                    val safeText = enteredText.replace("'", "'\\''")
                    val success = runRootCommands(listOf(
                        "settings put global default_device_name '$safeText'",
                        "setprop persist.sys.device_name '$safeText'",
                        "am force-stop com.android.settings"
                    ))
                    
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this@LabActivity, getString(R.string.msg_product_name_applied), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@LabActivity, getString(R.string.msg_root_permission_required), Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(12f) }

        val resetProductNameBtn = createAnimatedButton(getString(R.string.btn_reset_default), primaryTextColor, secondaryBtnColor, buttonHeightPx) {
            productNameInputEt.setText("")
            prefs.edit().remove("custom_product_name").apply()
            
            Thread {
                val success = runRootCommands(listOf(
                    "settings delete global default_device_name",
                    "setprop persist.sys.device_name ''",
                    "am force-stop com.android.settings"
                ))
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this@LabActivity, getString(R.string.msg_product_name_reset), Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(10f) }

        productNameExpandableContent.addView(productNameDisclaimerCard)
        productNameExpandableContent.addView(productNameInputEt)
        productNameExpandableContent.addView(saveProductNameBtn)
        productNameExpandableContent.addView(resetProductNameBtn)
        
        customProductNameCardLayout.addView(productNameExpandableContent)

        val toggleProductNameExpansion = {
            val isExpanded = productNameExpandableContent.visibility == View.VISIBLE
            if (isExpanded) {
                collapseView(productNameExpandableContent, productNameExpandBtn)
            } else {
                expandView(productNameExpandableContent, productNameExpandBtn)
            }
        }

        productNameExpandBtn.setOnClickListener { toggleProductNameExpansion() }
        productNameHeaderLayout.setOnClickListener { toggleProductNameExpansion() }
        productNameHeaderLayout.isClickable = true
        
        scrollContent.addView(customProductNameCardLayout)
        scrollView.addView(scrollContent)
        rootFrameLayout.addView(scrollView)

        // --- FLOATING TOP BAR ---
        val topBarLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dpToPx(16f), statusBarHeight + dpToPx(12f), dpToPx(16f), dpToPx(12f))
        }

        val topBarTitle = TextView(this).apply {
            text = getString(R.string.lab_title)
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

        rootFrameLayout.setOnApplyWindowInsetsListener { _, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetTop
            }
            val effectiveTop = if (topInset > 0) topInset else statusBarHeight

            topBarLayout.setPadding(dpToPx(16f), effectiveTop + dpToPx(12f), dpToPx(16f), dpToPx(12f))
            scrollView.setPadding(dpToPx(16f), effectiveTop + dpToPx(68f), dpToPx(16f), dpToPx(40f))
            insets
        }

        setContentView(rootFrameLayout)
        applyEntranceAnimations(listOf(noticeCardLayout, customProductNameCardLayout))
    }

    // --- HELPER METHODS ---

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

    private fun expandView(view: View, expandBtn: View) {
        val parent = view.parent as? View ?: return
        val matchParentSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val wrapContentSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(matchParentSpec, wrapContentSpec)
        val targetHeight = view.measuredHeight

        view.layoutParams.height = 1
        view.visibility = View.VISIBLE
        view.alpha = 0f

        val heightAnim = ValueAnimator.ofInt(1, targetHeight).apply {
            duration = 320L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                view.layoutParams.height = anim.animatedValue as Int
                view.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    view.requestLayout()
                }
            })
        }

        view.animate().alpha(1f).setDuration(220L).start()
        expandBtn.animate().rotation(180f).setDuration(320L).setInterpolator(AccelerateDecelerateInterpolator()).start()
        heightAnim.start()
    }

    private fun collapseView(view: View, expandBtn: View) {
        val initialHeight = view.height
        val heightAnim = ValueAnimator.ofInt(initialHeight, 0).apply {
            duration = 280L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                view.layoutParams.height = anim.animatedValue as Int
                view.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                }
            })
        }

        view.animate().alpha(0f).setDuration(180L).start()
        expandBtn.animate().rotation(0f).setDuration(280L).setInterpolator(AccelerateDecelerateInterpolator()).start()
        heightAnim.start()
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

    private fun dpToPx(dp: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}
