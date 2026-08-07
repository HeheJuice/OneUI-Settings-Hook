package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.content.res.Resources
import android.provider.Settings
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object DashboardColorPatch {
    private const val TAG = "OneUISettingsHook"
    private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsHook"

    // Style constants (must match SettingsActivity)
    private const val STYLE_DEFAULT = 0
    private const val STYLE_MONET = 1
    private const val STYLE_ONEUI6 = 2

    fun applyPatch(classLoader: ClassLoader) {
        try {
            Log.d(TAG, "=== DashboardColorPatch.applyPatch() START ===")

            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            if (activityThread == null) {
                Log.e(TAG, "currentActivityThread returned null – aborting.")
                return
            }

            val systemContext = XposedHelpers.callMethod(activityThread, "getSystemContext") as? Context
            if (systemContext == null) {
                Log.e(TAG, "getSystemContext returned null – aborting.")
                return
            }
            Log.d(TAG, "Got system context: $systemContext")

            val contentResolver = systemContext.contentResolver

            // Read the style from Settings.Global
            val style = Settings.Global.getInt(contentResolver, "oneui_hook_dashboard_style", STYLE_DEFAULT)
            Log.d(TAG, "Dashboard style = $style")

            // If Default, do nothing
            if (style == STYLE_DEFAULT) {
                Log.d(TAG, "Default style – drawable replacement disabled.")
                return
            }

            // Determine suffix based on style
            val suffix = when (style) {
                STYLE_MONET -> "_monet"
                STYLE_ONEUI6 -> "_oneui6"
                else -> "" // fallback
            }
            Log.d(TAG, "Using suffix: $suffix")

            // Get module resources
            val moduleContext = systemContext.createPackageContext(
                MODULE_PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val moduleResources = moduleContext.resources
            val resourcesClass = Resources::class.java

            // Hook all drawable methods (passing the suffix)
            XposedHelpers.findAndHookMethod(
                resourcesClass,
                "getDrawable",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        replaceDrawable(param, moduleResources, suffix, theme = null, density = 0)
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
                        replaceDrawable(param, moduleResources, suffix, theme = theme, density = 0)
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
                        replaceDrawable(param, moduleResources, suffix, theme = null, density = density)
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
                        replaceDrawable(param, moduleResources, suffix, theme = theme, density = density)
                    }
                }
            )
            Log.d(TAG, "Hooked Resources.getDrawableForDensity(int, int, Theme)")

            Log.d(TAG, "=== DashboardColorPatch.applyPatch() END (success) ===")

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply Dashboard drawable patch", e)
        }
    }

    private fun replaceDrawable(
        param: XC_MethodHook.MethodHookParam,
        moduleResources: Resources,
        suffix: String,
        theme: Resources.Theme?,
        density: Int
    ) {
        try {
            val res = param.thisObject as Resources
            val id = param.args[0] as Int

            val packageName = try { res.getResourcePackageName(id) } catch (_: Throwable) { "unknown" }
            val entryName = try { res.getResourceEntryName(id) } catch (_: Throwable) { "unknown" }

            // Only replace drawables from Settings
            if (packageName != "com.android.settings") return

            Log.v(TAG, "Settings drawable requested: $entryName (id=$id)")

            // Try with suffix first
            val suffixedName = entryName + suffix
            var moduleId = moduleResources.getIdentifier(suffixedName, "drawable", MODULE_PACKAGE_NAME)
            if (moduleId == 0) {
                // Fallback: try without suffix (original name) – this allows partial replacement
                moduleId = moduleResources.getIdentifier(entryName, "drawable", MODULE_PACKAGE_NAME)
                if (moduleId == 0) {
                    Log.v(TAG, "No replacement found for $entryName (with or without suffix)")
                    return
                }
                Log.d(TAG, "Using fallback (no suffix) for $entryName")
            } else {
                Log.d(TAG, "Found suffixed replacement: $suffixedName (moduleId=$moduleId)")
            }

            // Load the replacement drawable with the correct parameters
            val replacement = when {
                theme != null && density > 0 -> moduleResources.getDrawableForDensity(moduleId, density, theme)
                theme != null -> moduleResources.getDrawable(moduleId, theme)
                density > 0 -> moduleResources.getDrawableForDensity(moduleId, density)
                else -> moduleResources.getDrawable(moduleId)
            }

            if (replacement != null) {
                param.result = replacement
                Log.d(TAG, "✅ REPLACED drawable: $entryName (using ${if (moduleId == 0) "fallback" else "suffix"})")
            } else {
                Log.w(TAG, "Failed to load replacement for $entryName")
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Error replacing drawable", e)
        }
    }
}