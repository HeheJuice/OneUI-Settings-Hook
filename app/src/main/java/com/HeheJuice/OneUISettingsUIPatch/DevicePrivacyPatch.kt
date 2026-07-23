package com.HeheJuice.OneUISettingsUIPatch

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.WeakHashMap

object DevicePrivacyPatch {
    private const val TAG = "DevicePrivacyPatch"
    private val realValues = WeakHashMap<Any, CharSequence>()
    private val isRevealedState = WeakHashMap<Any, Boolean>()
    private const val MASK_TEXT = "••••••••••••••••"

    // Restricted strictly to IMEI and Series/Serial Number preference keys
    private val targetKeys = setOf(
        "imei",
        "serial_number"
    )

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val statusInfoClass = XposedHelpers.findClass(
                "com.samsung.android.settings.deviceinfo.statusinfo.StatusInfoSettings",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                statusInfoClass,
                "addPreferencesFromResource",
                Int::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val fragment = param.thisObject
                            val preferenceScreen = XposedHelpers.callMethod(fragment, "getPreferenceScreen") ?: return

                            for (key in targetKeys) {
                                val pref = XposedHelpers.callMethod(preferenceScreen, "findPreference", key) ?: continue
                                setupPrivacyPreference(pref)
                            }
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error applying privacy mask in StatusInfoSettings", e)
                        }
                    }
                }
            )

            Log.d(TAG, "Successfully initialized DevicePrivacyPatch for IMEI and Serial Number.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize DevicePrivacyPatch", e)
        }
    }

    private fun setupPrivacyPreference(preference: Any) {
        try {
            val preferenceClass = preference.javaClass

            val summary = XposedHelpers.callMethod(preference, "getSummary") as? CharSequence
            if (summary != null && summary.isNotEmpty() && summary != MASK_TEXT) {
                if (!realValues.containsKey(preference)) {
                    realValues[preference] = summary
                    isRevealedState[preference] = false
                }
                XposedHelpers.callMethod(preference, "setSummary", MASK_TEXT)
            }

            XposedHelpers.findAndHookMethod(
                preferenceClass,
                "performClick",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val pref = param.thisObject
                            if (realValues.containsKey(pref)) {
                                val currentRevealed = isRevealedState[pref] == true
                                val newRevealed = !currentRevealed
                                isRevealedState[pref] = newRevealed

                                val textToSet = if (newRevealed) realValues[pref] else MASK_TEXT
                                XposedHelpers.callMethod(pref, "setSummary", textToSet)
                                param.result = true
                            }
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error toggling privacy preference", e)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting up privacy preference item", e)
        }
    }
}
