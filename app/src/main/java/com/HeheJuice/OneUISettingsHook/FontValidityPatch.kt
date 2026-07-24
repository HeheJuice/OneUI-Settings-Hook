package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.util.Log
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

object FontValidityPatch {
    private const val TAG = "FontValidityPatch"
    private var isHooked = false

    fun applyPatch(classLoader: ClassLoader) {
        if (isHooked) return
        try {
            val targetClass = "com.samsung.android.settings.display.SecDisplayUtils"

            XposedHelpers.findAndHookMethod(
                targetClass,
                classLoader,
                "isInvalidFont",
                Context::class.java,
                String::class.java,
                XC_MethodReplacement.returnConstant(false)
            )

            isHooked = true
            Log.d(TAG, "Successfully hooked SecDisplayUtils.isInvalidFont to return false.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply FontValidityPatch", e)
        }
    }
}
