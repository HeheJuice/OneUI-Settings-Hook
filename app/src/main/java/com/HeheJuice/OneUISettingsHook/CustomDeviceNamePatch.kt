package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.provider.Settings
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object CustomDeviceNamePatch {

    private const val TAG = "OneUISettingsHook"
    private const val TARGET_CLASS = "com.samsung.android.settings.deviceinfo.SecDeviceInfoUtils"

    fun applyPatch(classLoader: ClassLoader) {
        try {
            val secDeviceInfoUtilsClass = XposedHelpers.findClass(TARGET_CLASS, classLoader)

            val deviceNameHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = param.args.firstOrNull { it is Context } as? Context ?: return
                    
                    // Retrieve custom name set via root/settings from global table or prop fallbacks
                    val customName = Settings.Global.getString(context.contentResolver, "default_device_name")
                        ?: Settings.Global.getString(context.contentResolver, "device_name")

                    if (!customName.isNullOrBlank()) {
                        param.result = customName
                    }
                }
            }

            // Methods present in SecDeviceInfoUtils.smali that return device name variants
            val targetMethods = listOf(
                "getDefaultDeviceName",
                "getDeviceName",
                "getOfficialDeviceName",
                "getMarketDeviceName"
            )

            for (methodName in targetMethods) {
                try {
                    XposedHelpers.findAndHookMethod(
                        secDeviceInfoUtilsClass,
                        methodName,
                        Context::class.java,
                        deviceNameHook
                    )
                    Log.d(TAG, "Successfully hooked SecDeviceInfoUtils.$methodName")
                } catch (e: Throwable) {
                    // Method variation might be absent depending on specific OneUI firmware base
                }
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to hook SecDeviceInfoUtils for custom device name", e)
        }
    }
}
