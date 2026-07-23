package com.HeheJuice.OneUISettingsUIPatch

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SoftwareInfoBannerPatch {
    private const val TAG = "SoftwareInfoBannerPatch"
    private var isHookInitialized = false
    private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsUIPatch"

    fun injectBanner(preferenceScreen: Any, context: Context, classLoader: ClassLoader) {
        try {
            if (!isHookInitialized) {
                initializeHook(classLoader)
                isHookInitialized = true
            }

            val existingBanner = XposedHelpers.callMethod(preferenceScreen, "findPreference", "custom_wallpaper_banner")
            if (existingBanner != null) return

            val preferenceClass = XposedHelpers.findClass("androidx.preference.Preference", classLoader)
            val bannerPref = XposedHelpers.newInstance(preferenceClass, context)

            XposedHelpers.callMethod(bannerPref, "setKey", "custom_wallpaper_banner")
            XposedHelpers.callMethod(bannerPref, "setOrder", -999)
            XposedHelpers.callMethod(bannerPref, "setSelectable", false)
            
            // Assign a unique public layout so RecyclerView gives it an exclusive ViewType.
            // This prevents view recycling conflicts with normal preferences.
            XposedHelpers.callMethod(bannerPref, "setLayoutResource", android.R.layout.two_line_list_item)

            try {
                XposedHelpers.callMethod(bannerPref, "setDividerAllowedAbove", false)
                XposedHelpers.callMethod(bannerPref, "setDividerAllowedBelow", false)
            } catch (ignored: Throwable) {}

            XposedHelpers.callMethod(preferenceScreen, "addPreference", bannerPref)
            Log.d(TAG, "Successfully injected custom wallpaper banner preference at top.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to inject custom wallpaper banner", e)
        }
    }

    private fun initializeHook(classLoader: ClassLoader) {
        try {
            val preferenceClass = XposedHelpers.findClass("androidx.preference.Preference", classLoader)
            XposedHelpers.findAndHookMethod(
                preferenceClass,
                "onBindViewHolder",
                XposedHelpers.findClass("androidx.preference.PreferenceViewHolder", classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val preference = param.thisObject
                            val key = XposedHelpers.callMethod(preference, "getKey") as? String
                            if (key != "custom_wallpaper_banner") return

                            val holder = param.args[0]
                            val itemView = XposedHelpers.getObjectField(holder, "itemView") as? ViewGroup ?: return
                            val ctx = itemView.context

                            // Skip rebuilding if already initialized inside this view
                            if (itemView.findViewById<View>(android.R.id.custom) != null) {
                                return
                            }

                            val modContext = try {
                                ctx.createPackageContext(MODULE_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
                            } catch (e: Throwable) {
                                null
                            }

                            itemView.removeAllViews()
                            itemView.setPadding(0, 0, 0, 0)
                            itemView.background = null

                            // Update LayoutParams safely without breaking RecyclerView layout managers
                            val bannerHeightPx = dpToPx(ctx, 160f)
                            val lp = itemView.layoutParams
                            if (lp != null) {
                                lp.height = bannerHeightPx
                                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                                itemView.layoutParams = lp
                            }

                            val rootLayout = FrameLayout(ctx).apply {
                                id = android.R.id.custom
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    bannerHeightPx
                                )
                                clipToOutline = true
                                outlineProvider = object : ViewOutlineProvider() {
                                    val radius = dpToPx(ctx, 26f).toFloat()
                                    override fun getOutline(view: View, outline: Outline) {
                                        outline.setRoundRect(0, 0, view.width, view.height, radius)
                                    }
                                }
                            }

                            val imageView = ImageView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }

                            try {
                                val wallpaperManager = WallpaperManager.getInstance(ctx)
                                val wallpaperDrawable = wallpaperManager.drawable
                                if (wallpaperDrawable != null) {
                                    imageView.setImageDrawable(wallpaperDrawable)
                                } else {
                                    imageView.setBackgroundColor(Color.parseColor("#222222"))
                                }
                            } catch (e: Throwable) {
                                Log.e(TAG, "Failed to load wallpaper drawable", e)
                                imageView.setBackgroundColor(Color.parseColor("#222222"))
                            }

                            val overlayView = View(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                background = GradientDrawable(
                                    GradientDrawable.Orientation.TOP_BOTTOM,
                                    intArrayOf(Color.parseColor("#33000000"), Color.parseColor("#77000000"))
                                )
                            }

                            val textView = TextView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = Gravity.CENTER
                                }
                                textSize = 38f
                                
                                typeface = try {
                                    Typeface.createFromAsset(modContext?.assets, "SamsungSharpSans-Bold.ttf")
                                } catch (e: Throwable) {
                                    Typeface.DEFAULT_BOLD
                                }

                                setTextColor(Color.WHITE)
                                setShadowLayer(10f, 0f, 4f, Color.parseColor("#C0000000"))
                            }

                            rootLayout.addView(imageView)
                            rootLayout.addView(overlayView)
                            rootLayout.addView(textView)

                            itemView.addView(rootLayout)

                            // Run Typewriter reveal and activate continuous shimmer sweep
                            val targetText = getOneUiVersionDisplay(modContext)
                            applyTypewriterWithShimmer(textView, targetText)

                        } catch (e: Throwable) {
                            Log.e(TAG, "Error binding custom wallpaper banner view", e)
                        }
                    }
                }
            )
            Log.d(TAG, "Successfully initialized global Preference.onBindViewHolder hook for banner.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize global banner hook", e)
        }
    }

    private fun applyTypewriterWithShimmer(textView: TextView, fullText: String, charDelayMs: Long = 65L) {
        val handler = Handler(Looper.getMainLooper())
        var index = 0
        textView.text = ""

        val typewriterRunnable = object : Runnable {
            override fun run() {
                if (index <= fullText.length) {
                    textView.text = fullText.substring(0, index++)
                    handler.postDelayed(this, charDelayMs)
                } else {
                    startShimmerSweep(textView)
                }
            }
        }
        handler.post(typewriterRunnable)
    }

    private fun startShimmerSweep(textView: TextView) {
        val text = textView.text.toString()
        if (text.isEmpty()) return

        val textWidth = textView.paint.measureText(text)
        if (textWidth <= 0f) return

        val baseColor = textView.currentTextColor

        val shimmerShader = LinearGradient(
            0f, 0f, textWidth, 0f,
            intArrayOf(baseColor, Color.WHITE, baseColor),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        textView.paint.shader = shimmerShader
        val matrix = Matrix()

        val animator = ValueAnimator.ofFloat(-textWidth, textWidth * 2f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animation ->
                val translate = animation.animatedValue as Float
                matrix.setTranslate(translate, 0f)
                shimmerShader.setLocalMatrix(matrix)
                textView.invalidate()
            }
        }
        animator.start()
    }

    private fun getOneUiVersionDisplay(modContext: Context?): String {
        try {
            val c = Class.forName("android.os.SystemProperties")
            val rawValue = c.getMethod("get", String::class.java, String::class.java).invoke(null, "ro.build.version.oneui", "") as? String ?: ""
            
            val intVal = rawValue.toIntOrNull()
            val versionStr = if (intVal != null) {
                val major = intVal / 10000
                val minor = (intVal % 10000) / 100
                if (minor > 0) "$major.$minor" else "$major"
            } else if (rawValue.isNotBlank()) {
                rawValue
            } else {
                ""
            }

            if (modContext != null && versionStr.isNotEmpty()) {
                val resId = modContext.resources.getIdentifier("oneui_version_format", "string", MODULE_PACKAGE_NAME)
                if (resId != 0) {
                    return String.format(modContext.getString(resId), versionStr)
                }
            } else if (modContext != null) {
                val resId = modContext.resources.getIdentifier("oneui_default", "string", MODULE_PACKAGE_NAME)
                if (resId != 0) {
                    return modContext.getString(resId)
                }
            }

            return if (versionStr.isNotEmpty()) "OneUI $versionStr" else "OneUI"
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to read ro.build.version.oneui property", e)
        }
        return "OneUI"
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return Math.round(dp * context.resources.displayMetrics.density)
    }
}
