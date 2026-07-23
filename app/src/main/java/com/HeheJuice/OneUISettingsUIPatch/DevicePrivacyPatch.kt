package com.HeheJuice.OneUISettingsUIPatch

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.util.WeakHashMap

object DevicePrivacyPatch {
    private const val TAG = "DevicePrivacyPatch"
    private val realValues = WeakHashMap<Any, CharSequence>()
    private val isRevealedState = WeakHashMap<Any, Boolean>()
    private const val MASK_TEXT = "••••••••••••••••"

    fun init(classLoader: ClassLoader) {
        try {
            val preferenceClass = XposedHelpers.findClass("androidx.preference.Preference", classLoader)

            // 1. Hook binding to mask IMEI / Serial numbers by default
            XposedHelpers.findAndHookMethod(
                preferenceClass,
                "onBindViewHolder",
                XposedHelpers.findClass("androidx.preference.PreferenceViewHolder", classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val preference = param.thisObject
                            val key = (XposedHelpers.callMethod(preference, "getKey") as? String)?.lowercase() ?: return

                            // Target Samsung's IMEI, Serial, and MEID preference keys
                            if (key.contains("imei") || key.contains("serial") || key.contains("meid")) {
                                val summary = XposedHelpers.callMethod(preference, "getSummary") as? CharSequence
                                if (summary != null && summary.isNotEmpty() && summary != MASK_TEXT) {
                                    
                                    // Save real value if not already stored
                                    if (!realValues.containsKey(preference)) {
                                        realValues[preference] = summary
                                        isRevealedState[preference] = false
                                    }

                                    // Apply mask if currently hidden
                                    val isRevealed = isRevealedState[preference] == true
                                    if (!isRevealed) {
                                        XposedHelpers.callMethod(preference, "setSummary", MASK_TEXT)
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error masking sensitive preference", e)
                        }
                    }
                }
            )

            // 2. Hook click action to toggle between masked and real text
            XposedHelpers.findAndHookMethod(
                preferenceClass,
                "performClick",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val preference = param.thisObject
                            val key = (XposedHelpers.callMethod(preference, "getKey") as? String)?.lowercase() ?: return

                            if (key.contains("imei") || key.contains("serial") || key.contains("meid")) {
                                if (realValues.containsKey(preference)) {
                                    val currentRevealed = isRevealedState[preference] == true
                                    val newRevealed = !currentRevealed
                                    isRevealedState[preference] = newRevealed

                                    // Update summary text dynamically on tap
                                    val textToSet = if (newRevealed) realValues[preference] else MASK_TEXT
                                    XposedHelpers.callMethod(preference, "setSummary", textToSet)

                                    // Consume click event so it toggles securely in-place without opening default popups
                                    param.result = true
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error handling preference click toggle", e)
                        }
                    }
                }
            )

            Log.d(TAG, "Successfully initialized DevicePrivacyPatch hooks.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize DevicePrivacyPatch", e)
        }
    }
}
