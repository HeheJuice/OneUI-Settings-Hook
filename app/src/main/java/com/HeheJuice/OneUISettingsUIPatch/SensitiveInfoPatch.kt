package com.HeheJuice.OneUISettingsUIPatch

import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object SensitiveInfoPatch {
    private const val TAG = "SensitiveInfoPatch"
    private var isHooked = false

    fun applyPatch(classLoader: ClassLoader) {
        if (isHooked) return
        try {
            val maskText = "•••••••••••••••"

            // 1. Hook Build.getSerial() (Standard Android API for Serial Number)
            try {
                XposedBridge.hookAllMethods(
                    Build::class.java,
                    "getSerial",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = maskText
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to hook Build.getSerial", e)
            }

            // 2. Hook TelephonyManager (Standard API for IMEI, MEID, etc.)
            val telephonyManagerClass = try {
                XposedHelpers.findClass("android.telephony.TelephonyManager", classLoader)
            } catch (e: Throwable) { null }

            if (telephonyManagerClass != null) {
                val tmMethods = listOf("getImei", "getMeid", "getDeviceId", "getSubscriberId", "getSimSerialNumber")
                for (methodName in tmMethods) {
                    try {
                        XposedBridge.hookAllMethods(
                            telephonyManagerClass,
                            methodName,
                            object : XC_MethodHook() {
                                override fun beforeHookedMethod(param: MethodHookParam) {
                                    param.result = maskText
                                }
                            }
                        )
                    } catch (e: Throwable) {
                        // Ignore missing methods safely across different Android versions
                    }
                }
            }

            // 3. Hook SystemProperties (Used by Samsung as a fallback property lookup)
            try {
                val sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", classLoader)
                XposedBridge.hookAllMethods(
                    sysPropClass,
                    "get",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val key = param.args[0] as? String ?: return
                            if (key == "ril.serialnumber" || key == "ro.serialno" || key == "ro.boot.serialno") {
                                param.result = maskText
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to hook SystemProperties", e)
            }

            // 4. Aggressive UI Fallback: Intercept Custom Samsung Grid Preferences
            try {
                val preferenceClass = XposedHelpers.findClass("androidx.preference.Preference", classLoader)
                XposedHelpers.findAndHookMethod(
                    preferenceClass,
                    "onBindViewHolder",
                    XposedHelpers.findClass("androidx.preference.PreferenceViewHolder", classLoader),
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val preference = param.thisObject
                            val key = XposedHelpers.callMethod(preference, "getKey") as? String ?: return
                            val lowerKey = key.lowercase()

                            // Check if it's the "About Phone" root card or an IMEI/SN item
                            if (lowerKey.contains("imei") || lowerKey.contains("serial") || 
                                lowerKey.contains("sn_") || lowerKey.contains("about_device")) {
                                
                                val holder = param.args[0]
                                val itemView = XposedHelpers.getObjectField(holder, "itemView") as? ViewGroup ?: return
                                maskTextViewsInHierarchy(itemView)
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to hook Preference UI", e)
            }

            isHooked = true
            Log.d(TAG, "Successfully initialized aggressive API & UI masking hooks.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply SensitiveInfoPatch hooks", e)
        }
    }

    /**
     * Recursively scans the layout of a Preference to find and mask real text values
     * without accidentally breaking static titles (like "IMEI (Slot 1)").
     */
    private fun maskTextViewsInHierarchy(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is TextView) {
                val text = child.text.toString().trim()
                
                // Pattern 1: Pure digits, 14 or 15 chars long (Standard IMEI/MEID format)
                val isImei = text.length >= 14 && text.all { it.isDigit() }
                
                // Pattern 2: Alphanumeric, strictly letters and numbers, 10+ chars (Samsung SN format)
                val isSerialNumber = text.length >= 10 && text.all { it.isLetterOrDigit() } && text.any { it.isDigit() }
                
                if (isImei || isSerialNumber) {
                    child.text = "•••••••••••••••"
                }
            } else if (child is ViewGroup) {
                maskTextViewsInHierarchy(child)
            }
        }
    }
}
