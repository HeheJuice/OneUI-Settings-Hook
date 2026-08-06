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
            // Read from device-protected SharedPreferences
            val prefs = XSharedPreferences(MODULE_PACKAGE_NAME, "mod_settings")
            prefs.makeWorldReadable()
            
            if (!prefs.getBoolean("enable_monet_dashboard", false)) {
                Log.d(TAG, "DashboardColorPatch is disabled in settings.")
                return
            }

            val excludedNames = setOf(
                "sec_dashboard_layer_color",
                "sec_dashboard_simplified_summary_text",
                "sec_dashboard_simplified_summary_text_color",
                "sec_dashboard_simplified_title_text",
                "sec_dashboard_simplified_title_text_color"
            )

            val targetIds = mutableSetOf<Int>()
            val colorClass = XposedHelpers.findClassIfExists("com.android.settings.R\$color", classLoader)
            
            if (colorClass != null) {
                for (field in colorClass.declaredFields) {
                    val name = field.name
                    if (name.startsWith("sec_dashboard_") && !excludedNames.contains(name)) {
                        field.isAccessible = true
                        targetIds.add(field.getInt(null))
                    }
                }
            }

            if (targetIds.isEmpty()) return
            Log.d(TAG, "DashboardColorPatch: Found ${targetIds.size} dashboard colors to patch to Monet.")

            // Hook getColor(int, Theme)
            XposedHelpers.findAndHookMethod(
                Resources::class.java,
                "getColor",
                Int::class.java,
                Resources.Theme::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val id = param.args[0] as Int
                        if (targetIds.contains(id)) {
                            val res = param.thisObject as Resources
                            val theme = param.args[1] as Resources.Theme?
                            // Redirect to Android 12+ System Monet Accent
                            val monetId = android.R.color.system_accent1_500
                            
                            try {
                                param.result = res.getColor(monetId, theme)
                            } catch (e: Throwable) {
                                // Fallback if system_accent1_500 doesn't resolve
                            }
                        }
                    }
                }
            )

            // Hook getColorStateList(int, Theme) to ensure full coverage
            XposedHelpers.findAndHookMethod(
                Resources::class.java,
                "getColorStateList",
                Int::class.java,
                Resources.Theme::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val id = param.args[0] as Int
                        if (targetIds.contains(id)) {
                            val res = param.thisObject as Resources
                            val theme = param.args[1] as Resources.Theme?
                            val monetId = android.R.color.system_accent1_500
                            
                            try {
                                param.result = res.getColorStateList(monetId, theme)
                            } catch (e: Throwable) {
                                // Fallback
                            }
                        }
                    }
                }
            )

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply DashboardColorPatch", e)
        }
    }
}
