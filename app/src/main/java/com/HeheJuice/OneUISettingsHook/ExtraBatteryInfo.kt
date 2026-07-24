package com.HeheJuice.OneUISettingsHook

import android.util.Log
import de.robv.android.xposed.XC_MethodReplacement.returnConstant
import de.robv.android.xposed.XposedHelpers

object ExtraBatteryInfo {
    private const val TAG = "ExtraBatteryInfo"
    private var isHooked = false

    fun applyPatch(classLoader: ClassLoader) {
        if (isHooked) return
        try {
            XposedHelpers.findAndHookMethod(
                "com.samsung.android.settings.deviceinfo.batteryinfo.BatteryRegulatoryPreferenceController",
                classLoader,
                "getAvailabilityStatus",
                returnConstant(0)
            )

            isHooked = true
            Log.d(TAG, "Successfully unlocked extra battery info.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply ExtraBatteryInfo patch", e)
        }
    }
}
