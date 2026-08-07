package com.HeheJuice.OneUISettingsHook

import android.content.Context
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
            val prefs = XSharedPreferences(MODULE_PACKAGE_NAME, "mod_settings")
            prefs.makeWorldReadable()

            if (!prefs.getBoolean("enable_monet_dashboard", false)) {
                Log.d(TAG, "Dashboard drawable replacement is disabled.")
                return
            }

            // Get a Resources object for your module
            val activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader)
            val appContext = XposedHelpers.callStaticMethod(activityThreadClass, "currentApplication") as Context
            val moduleContext = appContext.createPackageContext(
                MODULE_PACKAGE_NAME,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val moduleResources = moduleContext.resources

            val resourcesClass = Resources::class.java

            // ---- Hook getDrawable(int) ----
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

            // ---- Hook getDrawable(int, Theme) ----
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

            // ---- Hook getDrawableForDensity(int, int) ----
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

            // ---- Hook getDrawableForDensity(int, int, Theme) ----
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

            Log.d(TAG, "Dashboard drawable replacement patch applied.")

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
            if (packageName != "com.android.settings") return

            val entryName = res.getResourceEntryName(id)

            // Check if we have a drawable with the same name in our module
            val moduleId = moduleResources.getIdentifier(entryName, "drawable", MODULE_PACKAGE_NAME)
            if (moduleId == 0) return

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
            Log.d(TAG, "Replaced drawable: $entryName")

        } catch (e: Throwable) {
            Log.e(TAG, "Error replacing drawable", e)
        }
    }
}