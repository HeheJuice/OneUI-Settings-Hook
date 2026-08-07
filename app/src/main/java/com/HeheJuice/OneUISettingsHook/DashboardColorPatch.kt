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
    private const val GLOBAL_KEY = "oneui_hook_monet"

    fun applyPatch(classLoader: ClassLoader) {
        try {
            Log.d(TAG, "=== DashboardColorPatch.applyPatch() START ===")

            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            
            // Get the current ActivityThread instance
            val activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
            if (activityThread == null) {
                Log.e(TAG, "currentActivityThread returned null – aborting.")
                return
            }

            // Get system context from ActivityThread (always available)
            val systemContext = XposedHelpers.callMethod(activityThread, "getSystemContext") as? Context
            if (systemContext == null) {
                Log.e(TAG, "getSystemContext returned null – aborting.")
                return
            }
            Log.d(TAG, "Got system context: $systemContext")

            val contentResolver = systemContext.contentResolver

            // Read from Settings.Global
            val enabled = Settings.Global.getInt(contentResolver, GLOBAL_KEY, 0) == 1
            Log.d(TAG, "enable_monet_dashboard (from Settings.Global) = $enabled")

            if (!enabled) {
                Log.d(TAG, "Dashboard drawable replacement is disabled.")
                return
            }

            // Get module resources
            val moduleContext = systemContext.createPackageContext(
                MODULE_PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val moduleResources = moduleContext.resources
            val resourcesClass = Resources::class.java

            // Hook all drawable methods
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

    private fun replaceDrawable(
        param: XC_MethodHook.MethodHookParam,
        moduleResources: Resources,
        theme: Resources.Theme?,
        density: Int
    ) {
        try {
            val res = param.thisObject as Resources
            val id = param.args[0] as Int

            val packageName = try { res.getResourcePackageName(id) } catch (_: Throwable) { "unknown" }
            val entryName = try { res.getResourceEntryName(id) } catch (_: Throwable) { "unknown" }

            if (packageName != "com.android.settings") return

            Log.v(TAG, "Settings drawable requested: $entryName (id=$id)")

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