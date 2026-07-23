package com.HeheJuice.OneUISettingsUIPatch

import android.content.Context
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SettingPatch : IXposedHookLoadPackage {
    
    companion object {
        private const val TAG = "OneUISettingsUIPatch"
        private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsUIPatch"
        // Track unique fragment instances to ensure the patch runs strictly once per page open
        private val processedFragments = mutableSetOf<Int>()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.settings") return

        val targetFragmentClass = "androidx.preference.PreferenceFragmentCompat"
        
        try {
            XposedHelpers.findAndHookMethod(
                targetFragmentClass,
                lpparam.classLoader,
                "addPreferencesFromResource",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val fragment = param.thisObject
                            val fragmentHash = fragment.hashCode()

                            // If this specific fragment instance was already patched, exit immediately
                            if (processedFragments.contains(fragmentHash)) return

                            val preferenceScreen = XposedHelpers.callMethod(fragment, "getPreferenceScreen") ?: return
                            val context = XposedHelpers.callMethod(fragment, "getContext") as? Context ?: return

                            // Verify if this specific screen is the Software Info page by checking core keys
                            val oneUiPref = XposedHelpers.callMethod(preferenceScreen, "findPreference", "one_ui_version")
                            val firmwarePref = XposedHelpers.callMethod(preferenceScreen, "findPreference", "android_firmware_version")
                            if (oneUiPref == null && firmwarePref == null) return

                            // Lock this fragment instance so it never runs twice
                            processedFragments.add(fragmentHash)

                            Log.d(TAG, "Software Info screen detected! Restructuring layout & injecting banner...")

                            // Obtain module context for localized strings
                            val modContext = try {
                                context.createPackageContext(MODULE_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
                            } catch (e: Throwable) {
                                null
                            }

                            // Inject the custom wallpaper banner with the dynamic text overlay at the top
                            SoftwareInfoBannerPatch.injectBanner(preferenceScreen, context, lpparam.classLoader)

                            val preferenceCategoryClass = XposedHelpers.findClass("androidx.preference.PreferenceCategory", lpparam.classLoader)

                            // 1. Create Category: "About Your Galaxy" (Key: GalaxyInfo)
                            val galaxyCategory = XposedHelpers.newInstance(preferenceCategoryClass, context)
                            XposedHelpers.callMethod(galaxyCategory, "setTitle", getLocalizedString(modContext, "about_your_galaxy", "About Your Galaxy"))
                            XposedHelpers.callMethod(galaxyCategory, "setKey", "GalaxyInfo")
                            XposedHelpers.callMethod(preferenceScreen, "addPreference", galaxyCategory)

                            // Move items into "About Your Galaxy" in the requested order
                            val galaxyKeys = listOf("one_ui_version", "android_firmware_version", "kernel_version", "build_number")
                            for (key in galaxyKeys) {
                                val pref = XposedHelpers.callMethod(preferenceScreen, "findPreference", key)
                                if (pref != null) {
                                    XposedHelpers.callMethod(preferenceScreen, "removePreference", pref)
                                    XposedHelpers.callMethod(galaxyCategory, "addPreference", pref)
                                }
                            }

                            // 2. Create Category: "Software Details" (Key: extra_info)
                            val softwareCategory = XposedHelpers.newInstance(preferenceCategoryClass, context)
                            XposedHelpers.callMethod(softwareCategory, "setTitle", getLocalizedString(modContext, "software_details", "Software Details"))
                            XposedHelpers.callMethod(softwareCategory, "setKey", "extra_info")
                            XposedHelpers.callMethod(preferenceScreen, "addPreference", softwareCategory)

                            // Move items into "Software Details" in the requested order
                            val softwareKeys = listOf(
                                "module_version", "base_band", "selinux_status", 
                                "knox_version", "omc_version", "carrier_config_ver", 
                                "cc_mode_status", "security_sw_version", "security_key"
                            )
                            for (key in softwareKeys) {
                                val pref = XposedHelpers.callMethod(preferenceScreen, "findPreference", key)
                                if (pref != null) {
                                    XposedHelpers.callMethod(preferenceScreen, "removePreference", pref)
                                    XposedHelpers.callMethod(softwareCategory, "addPreference", pref)
                                }
                            }

                            Log.d(TAG, "Successfully restructured Software Info layout cleanly.")
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error restructuring software info layout", e)
                        }
                    }
                }
            )
            Log.d(TAG, "Successfully hooked $targetFragmentClass")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to hook $targetFragmentClass", e)
        }
    }

    private fun getLocalizedString(modContext: Context?, key: String, fallback: String): String {
        if (modContext != null) {
            try {
                val resId = modContext.resources.getIdentifier(key, "string", MODULE_PACKAGE_NAME)
                if (resId != 0) {
                    return modContext.getString(resId)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load localized string for $key", e)
            }
        }
        return fallback
    }
}
