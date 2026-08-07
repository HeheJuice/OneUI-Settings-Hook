package com.HeheJuice.OneUISettingsHook

import android.content.res.Resources
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers

object DashboardColorPatch {
    private const val TAG = "OneUISettingsHook"
    private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsHook"

    fun applyPatch(classLoader: ClassLoader) {
        try {
            Log.d(TAG, "=== DashboardColorPatch.applyPatch() START ===")

            // Use XSharedPreferences to read the module's own preferences
            val prefs = XSharedPreferences(MODULE_PACKAGE_NAME, "mod_settings")
            prefs.makeWorldReadable()   // Ensure the file is readable by the system process
            val enabled = prefs.getBoolean("enable_monet_dashboard", false)
            Log.d(TAG, "enable_monet_dashboard = $enabled")

            if (!enabled) {
                Log.d(TAG, "Dashboard drawable replacement is disabled.")
                return
            }

            // We still need a Resources object for our module to load drawables.
            // Use the system context to create a package context for our module.
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val systemContext = XposedHelpers.callStaticMethod(activityThreadClass, "getSystemContext") as android.content.Context
            val moduleContext = systemContext.createPackageContext(
                MODULE_PACKAGE_NAME,
                android.content.Context.CONTEXT_IGNORE_SECURITY
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