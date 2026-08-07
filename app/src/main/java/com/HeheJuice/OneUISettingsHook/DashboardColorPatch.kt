package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.content.res.Resources
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object DashboardColorPatch {
    private const val TAG = "OneUISettingsHook"
    private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsHook"

    fun applyPatch(classLoader: ClassLoader) {
        try {
            Log.d(TAG, "=== DashboardColorPatch.applyPatch() START ===")

            // ---- Get a valid Context ----
            val context = getModuleContext(classLoader)
            if (context == null) {
                Log.e(TAG, "Failed to get module context – aborting.")
                return
            }
            Log.d(TAG, "Got module context: $context")

            val prefs = context.getSharedPreferences("mod_settings", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("enable_monet_dashboard", false)
            Log.d(TAG, "enable_monet_dashboard = $enabled")

            if (!enabled) {
                Log.d(TAG, "Dashboard drawable replacement is disabled.")
                return
            }

            val moduleResources = context.resources
            val resourcesClass = Resources::class.java

            // ---- Hook all variants ----
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
            Log.d(TAG, "Hooked Resources.getDrawable(int)")

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
            Log.d(TAG, "Hooked Resources.getDrawable(int, Theme)")

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
            Log.d(TAG, "Hooked Resources.getDrawableForDensity(int, int)")

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
            Log.d(TAG, "Hooked Resources.getDrawableForDensity(int, int, Theme)")

            Log.d(TAG, "=== DashboardColorPatch.applyPatch() END (success) ===")

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply Dashboard drawable patch", e)
        }
    }

    // ---- Helper to obtain a Context with multiple fallbacks ----
    private fun getModuleContext(classLoader: ClassLoader): Context? {
        try {
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)

            // Attempt 1: getSystemContext()
            try {
                val sysCtx = XposedHelpers.callStaticMethod(activityThreadClass, "getSystemContext") as? Context
                if (sysCtx != null) {
                    Log.d(TAG, "Using getSystemContext()")
                    return sysCtx.createPackageContext(MODULE_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "getSystemContext() failed", e)
            }

            // Attempt 2: currentApplication()
            try {
                val appCtx = XposedHelpers.callStaticMethod(activityThreadClass, "currentApplication") as? Context
                if (appCtx != null) {
                    Log.d(TAG, "Using currentApplication()")
                    return appCtx.createPackageContext(MODULE_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "currentApplication() failed", e)
            }

            // Attempt 3: getPackageManager() and getApplicationInfo? Not needed here.

            Log.e(TAG, "All attempts to get context failed.")
            return null
        } catch (e: Throwable) {
            Log.e(TAG, "Exception in getModuleContext", e)
            return null
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

            // Get resource info
            val packageName = try { res.getResourcePackageName(id) } catch (_: Throwable) { "unknown" }
            val entryName = try { res.getResourceEntryName(id) } catch (_: Throwable) { "unknown" }

            // Only replace drawables from Settings
            if (packageName != "com.android.settings") return

            Log.v(TAG, "Settings drawable requested: $entryName (id=$id)")

            // Look for a replacement with same name
            val moduleId = moduleResources.getIdentifier(entryName, "drawable", MODULE_PACKAGE_NAME)
            if (moduleId == 0) {
                Log.v(TAG, "No replacement found for drawable: $entryName")
                return
            }

            Log.d(TAG, "Found replacement for $entryName (moduleId=$moduleId)")

            val replacement = when {
                theme != null && density > 0 -> moduleResources.getDrawableForDensity(moduleId, density, theme)
                theme != null -> moduleResources.getDrawable(moduleId, theme)
                density > 0 -> moduleResources.getDrawableForDensity(moduleId, density)
                else -> moduleResources.getDrawable(moduleId)
            }

            if (replacement != null) {
                param.result = replacement
                Log.d(TAG, "✅ REPLACED drawable: $entryName")
            } else {
                Log.w(TAG, "Failed to load replacement for $entryName")
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Error replacing drawable", e)
        }
    }
}