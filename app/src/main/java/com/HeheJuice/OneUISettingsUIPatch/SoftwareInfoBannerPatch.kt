package com.HeheJuice.OneUISettingsUIPatch

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
            // Force absolute top placement using Int.MIN_VALUE
            XposedHelpers.callMethod(bannerPref, "setOrder", Int.MIN_VALUE)
            XposedHelpers.callMethod(bannerPref, "setSelectable", false)

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

                            val modContext = try {
                                ctx.createPackageContext(MODULE_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
                            } catch (e: Throwable) {
                                null
                            }

                            itemView.removeAllViews()
                            itemView.setPadding(0, 0, 0, 0)
                            itemView.background = null

                            val rootLayout = FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.MarginLayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    dpToPx(ctx, 160f)
                                ).apply {
                                    setMargins(0, 0, 0, 0)
                                }

                                clipToOutline = true
                                outlineProvider = object : ViewOutlineProvider() {
                                    override fun getOutline(view: View, outline: Outline) {
                                        outline.setRoundRect(0, 0, view.width, view.height, dpToPx(ctx, 26f).toFloat())
                                    }
                                }
                            }

                            val imageView = ImageView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                scaleType = ImageView.ScaleType.FIT_XY

                                post {
                                    try {
                                        val wallpaperManager = WallpaperManager.getInstance(ctx)
                                        val wallpaperDrawable = wallpaperManager.drawable
                                        val vWidth = width
                                        val vHeight = height

                                        if (wallpaperDrawable != null && vWidth > 0 && vHeight > 0) {
                                            val rawBitmap = drawableToBitmap(wallpaperDrawable)
                                            if (rawBitmap != null) {
                                                val centerCroppedBitmap = centerCropBitmap(rawBitmap, vWidth, vHeight)
                                                setImageBitmap(centerCroppedBitmap)
                                            } else {
                                                setImageDrawable(wallpaperDrawable)
                                            }
                                        } else {
                                            setBackgroundColor(Color.parseColor("#333333"))
                                        }
                                    } catch (e: Throwable) {
                                        Log.e(TAG, "Error applying center-cropped wallpaper bitmap", e)
                                        setBackgroundColor(Color.parseColor("#333333"))
                                    }
                                }
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
                                text = getOneUiVersionDisplay(modContext)
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

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun centerCropBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val bWidth = bitmap.width
        val bHeight = bitmap.height

        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        val bitmapRatio = bWidth.toFloat() / bHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val cropX: Int
        val cropY: Int

        if (bitmapRatio > targetRatio) {
            cropHeight = bHeight
            cropWidth = (bHeight * targetRatio).toInt()
            cropX = (bWidth - cropWidth) / 2
            cropY = 0
        } else {
            cropWidth = bWidth
            cropHeight = (bWidth / targetRatio).toInt()
            cropX = 0
            cropY = (bHeight - cropHeight) / 2
        }

        return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
    }

    private fun dpToPx(context: Context, dp: Float): Int {
        return Math.round(dp * context.resources.displayMetrics.density)
    }
}
