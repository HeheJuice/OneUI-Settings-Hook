package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object DashboardColorPatch {
    private const val TAG = "OneUISettingsHook"
    private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsHook"

    fun applyPatch(classLoader: ClassLoader) {
        try {
            // Get module context and read preference
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val appContext = XposedHelpers.callStaticMethod(activityThreadClass, "currentApplication") as Context
            val moduleContext = appContext.createPackageContext(
                MODULE_PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val prefs = moduleContext.getSharedPreferences("mod_settings", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("enable_monet_dashboard", false)) {
                Log.d(TAG, "Dashboard drawable replacement is disabled.")
                return
            }

            val moduleResources = moduleContext.resources
            val resourcesClass = Resources::class.java

            // ---- Hook Resources.getDrawable(int) ----
            XposedHelpers.findAndHookMethod(
                resourcesClass,
                "getDrawable",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        replaceDrawable(param, moduleResources, theme = null, density = 0)
                    }
                }
            )

            // ---- Hook Resources.getDrawable(int, Theme) ----
            XposedHelpers.findAndHookMethod(
                resourcesClass,
                "getDrawable",
                Int::class.java,
                Resources.Theme::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val theme = param.args[1] as? Resources.Theme
                        replaceDrawable(param, moduleResources, theme = theme, density = 0)
                    }
                }
            )

            // ---- Hook Resources.getDrawableForDensity(int, int) ----
            XposedHelpers.findAndHookMethod(
                resourcesClass,
                "getDrawableForDensity",
                Int::class.java,
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val density = param.args[1] as Int
                        replaceDrawable(param, moduleResources, theme = null, density = density)
                    }
                }
            )

            // ---- Hook Resources.getDrawableForDensity(int, int, Theme) ----
            XposedHelpers.findAndHookMethod(
                resourcesClass,
                "getDrawableForDensity",
                Int::class.java,
                Int::class.java,
                Resources.Theme::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val density = param.args[1] as Int
                        val theme = param.args[2] as? Resources.Theme
                        replaceDrawable(param, moduleResources, theme = theme, density = density)
                    }
                }
            )

            // ---- Hook Context.getDrawable(int) ----
            XposedHelpers.findAndHookMethod(
                Context::class.java,
                "getDrawable",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // This will eventually call Resources methods; we can also handle it directly
                        // But our Resources hooks should catch it anyway.
                    }
                }
            )

            // ---- Hook AppCompatResources.getDrawable(Context, int) ----
            try {
                val appCompatResourcesClass = XposedHelpers.findClass(
                    "androidx.appcompat.content.res.AppCompatResources",
                    classLoader
                )
                XposedHelpers.findAndHookMethod(
                    appCompatResourcesClass,
                    "getDrawable",
                    Context::class.java,
                    Int::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // We can't easily replace the drawable here because we don't have the Resources object
                            // But we can let Resources hooks handle it.
                        }
                    }
                )
            } catch (_: Throwable) {}

            Log.d(TAG, "Dashboard drawable replacement patch applied successfully.")

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply Dashboard drawable patch", e)
        }
    }

    private fun replaceDrawable(
        param: XC_MethodHook.MethodHookParam,
        moduleResources: Resources,
        theme: Resources.Theme?,
        density: Int
    ) {
        try {
            val res = param.thisObject as Resources
            val id = param.args[0] as Int

            // Only replace drawables that belong to Settings
            val packageName = res.getResourcePackageName(id)
            if (packageName != "com.android.settings") {
                // Log if you want to see what's being called
                // Log.v(TAG, "Drawable from $packageName: ${res.getResourceEntryName(id)}")
                return
            }

            val entryName = res.getResourceEntryName(id)

            // Check if we have a drawable with the same name in our module
            val moduleId = moduleResources.getIdentifier(entryName, "drawable", MODULE_PACKAGE_NAME)
            if (moduleId == 0) {
                Log.v(TAG, "No replacement found for drawable: $entryName")
                return
            }

            // Load the replacement drawable with the same parameters
            val replacement = when {
                theme != null && density > 0 -> {
                    moduleResources.getDrawableForDensity(moduleId, density, theme)
                }
                theme != null -> {
                    moduleResources.getDrawable(moduleId, theme)
                }
                density > 0 -> {
                    moduleResources.getDrawableForDensity(moduleId, density)
                }
                else -> {
                    moduleResources.getDrawable(moduleId)
                }
            }

            param.result = replacement
            Log.d(TAG, "✅ Replaced drawable: $entryName (ID: $id) with module drawable")

        } catch (e: Throwable) {
            Log.e(TAG, "Error replacing drawable", e)
        }
    }
}