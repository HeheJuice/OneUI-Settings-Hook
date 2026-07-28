package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.provider.Settings
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object CustomDeviceNamePatch {

    private const val TAG = "OneUISettingsHook"
    private const val TARGET_CLASS = "com.samsung.android.settings.deviceinfo.SecDeviceInfoUtils"

    fun applyPatch(classLoader: ClassLoader) {
        try {
            val secDeviceInfoUtilsClass = XposedHelpers.findClass(TARGET_CLASS, classLoader)

            val productNameHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = param.args.firstOrNull { it is Context } as? Context ?: return
                    
                    // STRICTLY read default_device_name. Do NOT fall back to "device_name"!
                    val customProductName = Settings.Global.getString(context.contentResolver, "default_device_name")

                    if (!customProductName.isNullOrBlank()) {
                        param.result = customProductName
                    }
                }
            }

            // Methods that resolve the factory product/model name
            val productMethods = listOf(
                "getDefaultDeviceName",
                "getOfficialDeviceName",
                "getMarketDeviceName"
            )

            for (methodName in productMethods) {
                try {
                    XposedHelpers.findAndHookMethod(
                        secDeviceInfoUtilsClass,
                        methodName,
                        Context::class.java,
                        productNameHook
                    )
                    Log.d(TAG, "Successfully hooked SecDeviceInfoUtils.$methodName for Product Name")
                } catch (e: Throwable) {
                    // Method may not exist on some ROM bases
                }
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to hook SecDeviceInfoUtils for custom device name", e)
        }
    }
}
