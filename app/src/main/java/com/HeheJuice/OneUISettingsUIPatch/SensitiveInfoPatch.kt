package com.HeheJuice.OneUISettingsUIPatch

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SensitiveInfoPatch {
    private const val TAG = "SensitiveInfoPatch"
    private var isHooked = false

    // Common preference keys used for IMEI and Serial Number in OneUI / Android Settings
    private val SENSITIVE_KEYS = setOf(
        "status_imei",
        "status_imei_sv",
        "status_serial_number",
        "imei_info",
        "serial_number",
        "device_info_imei",
        "device_info_serial",
        "status_imei_slot1",
        "status_imei_slot2",
        "status_iccid",
        "min_number"
    )

    fun applyPatch(classLoader: ClassLoader) {
        if (isHooked) return
        try {
            val preferenceClass = XposedHelpers.findClass("androidx.preference.Preference", classLoader)
            
            // Intercept setSummary so any dynamic text updates get replaced with masked bullets
            XposedHelpers.findAndHookMethod(
                preferenceClass,
                "setSummary",
                CharSequence::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val preference = param.thisObject
                        val key = XposedHelpers.callMethod(preference, "getKey") as? String ?: return

                        if (SENSITIVE_KEYS.contains(key)) {
                            param.args[0] = "•••••••••••••••" // Replaces real IMEI/SN text
                        }
                    }
                }
            )

            isHooked = true
            Log.d(TAG, "Successfully initialized SensitiveInfoPatch hooks.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply SensitiveInfoPatch hooks", e)
        }
    }

    /**
     * Recursively searches through preference groups and removes the tiles completely.
     * Call this inside addPreferencesFromResource if you want them totally hidden instead of masked.
     */
    fun removeSensitivePreferences(group: Any) {
        try {
            val count = XposedHelpers.callMethod(group, "getPreferenceCount") as? Int ?: return
            for (i in count - 1 downTo 0) {
                val pref = XposedHelpers.callMethod(group, "getPreference", i) ?: continue
                val key = XposedHelpers.callMethod(pref, "getKey") as? String

                if (key != null && SENSITIVE_KEYS.contains(key)) {
                    XposedHelpers.callMethod(group, "removePreference", pref)
                    Log.d(TAG, "Removed sensitive preference tile: $key")
                } else if (pref.javaClass.name.contains("PreferenceGroup") || 
                           pref.javaClass.name.contains("PreferenceCategory") || 
                           pref.javaClass.name.contains("PreferenceScreen")) {
                    removeSensitivePreferences(pref)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error removing sensitive preferences", e)
        }
    }
}
