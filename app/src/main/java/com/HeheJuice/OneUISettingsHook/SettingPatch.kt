package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SettingPatch : IXposedHookLoadPackage {
    
    companion object {
        private const val TAG = "OneUISettingsHook"
        private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsHook"
        private val processedFragments = mutableSetOf<Int>()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.settings") return

        // 1. Initialize IMEI, Serial Number & Phone Number tap-to-toggle masking
        SensitiveInfoPatch.applyPatch(lpparam.classLoader)

        // 2. Initialize Custom Font validity check patch
        FontValidityPatch.applyPatch(lpparam.classLoader)

        // 3. Initialize Extra Battery Info patch
        ExtraBatteryInfo.applyPatch(lpparam.classLoader)

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
                            
                            val preferenceScreen = XposedHelpers.callMethod(fragment, "getPreferenceScreen") ?: return

                            if (processedFragments.contains(fragmentHash)) return

                            val context = XposedHelpers.callMethod(fragment, "getContext") as? Context ?: return

                            val oneUiPref = XposedHelpers.callMethod(preferenceScreen, "findPreference", "one_ui_version")
                            val firmwarePref = XposedHelpers.callMethod(preferenceScreen, "findPreference", "android_firmware_version")
                            
                            if (oneUiPref == null && firmwarePref == null) return

                            // Strict run-once check
                            val existingBanner = XposedHelpers.callMethod(preferenceScreen, "findPreference", "custom_wallpaper_banner")
                            if (existingBanner != null) {
                                processedFragments.add(fragmentHash)
                                return
                            }

                            processedFragments.add(fragmentHash)
                            Log.d(TAG, "Software Info screen detected! Restructuring layout & injecting banner at top...")

                            val modContext = try {
                                context.createPackageContext(MODULE_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY)
                            } catch (e: Throwable) {
                                null
                            }

                            val preferenceCategoryClass = XposedHelpers.findClass("androidx.preference.PreferenceCategory", lpparam.classLoader)
                            val preferenceClass = XposedHelpers.findClass("androidx.preference.Preference", lpparam.classLoader)

                            // 1. Inject banner at the top
                            SoftwareInfoBannerPatch.injectBanner(preferenceScreen, context, lpparam.classLoader)

                            // 2. Category: "About Your Galaxy"
                            val galaxyCategory = XposedHelpers.newInstance(preferenceCategoryClass, context)
                            XposedHelpers.callMethod(galaxyCategory, "setTitle", getLocalizedString(modContext, "about_your_galaxy", "About Your Galaxy"))
                            XposedHelpers.callMethod(galaxyCategory, "setKey", "GalaxyInfo")
                            XposedHelpers.callMethod(preferenceScreen, "addPreference", galaxyCategory)

                            val galaxyKeys = listOf("one_ui_version", "android_firmware_version", "kernel_version", "build_number")
                            for (key in galaxyKeys) {
                                val pref = XposedHelpers.callMethod(preferenceScreen, "findPreference", key)
                                if (pref != null) {
                                    XposedHelpers.callMethod(preferenceScreen, "removePreference", pref)
                                    XposedHelpers.callMethod(galaxyCategory, "addPreference", pref)
                                }
                            }

                            // 3. Category: "Module Information"
                            val moduleCategory = XposedHelpers.newInstance(preferenceCategoryClass, context)
                            XposedHelpers.callMethod(moduleCategory, "setTitle", getLocalizedString(modContext, "module_info_category", "Module Information"))
                            XposedHelpers.callMethod(moduleCategory, "setKey", "module_info_category")
                            XposedHelpers.callMethod(preferenceScreen, "addPreference", moduleCategory)

                            val namePref = XposedHelpers.newInstance(preferenceClass, context)
                            XposedHelpers.callMethod(namePref, "setTitle", getLocalizedString(modContext, "app_name", "OneUI Settings Hook"))
                            XposedHelpers.callMethod(namePref, "setSummary", getLocalizedString(modContext, "module_summary", "Modifying OneUI Settings UI"))
                            XposedHelpers.callMethod(namePref, "setSelectable", false)
                            XposedHelpers.callMethod(moduleCategory, "addPreference", namePref)

                            val makerPref = XposedHelpers.newInstance(preferenceClass, context)
                            XposedHelpers.callMethod(makerPref, "setTitle", getLocalizedString(modContext, "module_maker_title", "Module Maker"))
                            XposedHelpers.callMethod(makerPref, "setSummary", "HeheJuice")
                            val makerIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice"))
                            XposedHelpers.callMethod(makerPref, "setIntent", makerIntent)
                            XposedHelpers.callMethod(moduleCategory, "addPreference", makerPref)

                            val githubPref = XposedHelpers.newInstance(preferenceClass, context)
                            XposedHelpers.callMethod(githubPref, "setTitle", getLocalizedString(modContext, "module_github_title", "GitHub Repository"))
                            XposedHelpers.callMethod(githubPref, "setSummary", "https://github.com/HeheJuice/OneUI-Settings-Patch")
                            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeheJuice/OneUI-Settings-Patch"))
                            XposedHelpers.callMethod(githubPref, "setIntent", viewIntent)
                            XposedHelpers.callMethod(moduleCategory, "addPreference", githubPref)

                            // 4. Category: "Software Details"
                            val softwareCategory = XposedHelpers.newInstance(preferenceCategoryClass, context)
                            XposedHelpers.callMethod(softwareCategory, "setTitle", getLocalizedString(modContext, "software_details", "Software Details"))
                            XposedHelpers.callMethod(softwareCategory, "setKey", "extra_info")
                            XposedHelpers.callMethod(preferenceScreen, "addPreference", softwareCategory)

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

                            Log.d(TAG, "Successfully restructured Software Info layout with banner at the top.")
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
