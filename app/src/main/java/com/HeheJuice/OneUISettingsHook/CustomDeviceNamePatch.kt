package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.provider.Settings
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences

object CustomDeviceNamePatch {

    private const val TAG = "OneUISettingsHook"
    private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsHook"
    private const val TARGET_CLASS = "com.samsung.android.settings.deviceinfo.SecDeviceInfoUtils"

    // Load module SharedPreferences for instant cross-process reading
    private val pref: XSharedPreferences by lazy {
        XSharedPreferences(MODULE_PACKAGE_NAME, "${MODULE_PACKAGE_NAME}_preferences").apply {
            makeWorldReadable()
        }
    }

    fun applyPatch(classLoader: ClassLoader) {
        try {
            val secDeviceInfoUtilsClass = XposedHelpers.findClass(TARGET_CLASS, classLoader)

            val productNameHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // 1. Try reading directly from module SharedPreferences first
                    pref.reload()
                    var customName = pref.getString("custom_product_name", null)

                    // 2. Fall back to global settings table: default_device_name
                    if (customName.isNullOrBlank()) {
                        val context = param.args.firstOrNull { it is Context } as? Context
                        if (context != null) {
                            customName = Settings.Global.getString(context.contentResolver, "default_device_name")
                        }
                    }

                    // 3. Override return value if custom product name is set
                    if (!customName.isNullOrBlank()) {
                        param.result = customName
                    }
                }
            }

            // Target all product/device name getters in SecDeviceInfoUtils
            val targetMethods = listOf(
                "getDefaultDeviceName",
                "getOfficialDeviceName",
                "getMarketDeviceName",
                "getDeviceName"
            )

            for (methodName in targetMethods) {
                try {
                    // hookAllMethods catches ALL overloads regardless of parameter types
                    val unhooks = XposedBridge.hookAllMethods(secDeviceInfoUtilsClass, methodName, productNameHook)
                    if (unhooks.isNotEmpty()) {
                        Log.d(TAG, "Successfully hooked $methodName (${unhooks.size} overloads) in SecDeviceInfoUtils")
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to hook $methodName in SecDeviceInfoUtils", e)
                }
            }

        } catch (e: Throwable) {
            Log.e(TAG, "Failed to find class $TARGET_CLASS", e)
        }
    }
}
