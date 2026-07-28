package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.provider.Settings
import android.util.Log
import de.robv.android.xposed.AndroidAppHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences

object CustomDeviceNamePatch {

    private const val TAG = "OneUISettingsHook"
    private const val MODULE_PACKAGE_NAME = "com.HeheJuice.OneUISettingsHook"

    // Reads from "mod_settings" file saved by SettingsActivity
    private val pref: XSharedPreferences by lazy {
        XSharedPreferences(MODULE_PACKAGE_NAME, "mod_settings").apply {
            makeWorldReadable()
        }
    }

    fun applyPatch(classLoader: ClassLoader) {
        val deviceNameHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                var customName: String? = null

                // 1. Try reading from module's SharedPreferences
                try {
                    pref.reload()
                    customName = pref.getString("custom_product_name", null)
                } catch (e: Throwable) {
                    // Ignore SharedPreferences read errors
                }

                // 2. Try fetching from Context argument or AndroidAppHelper
                if (customName.isNullOrBlank()) {
                    val context = (param.args.firstOrNull { it is Context } as? Context)
                        ?: AndroidAppHelper.currentApplication()

                    if (context != null) {
                        customName = Settings.Global.getString(context.contentResolver, "default_device_name")
                    }
                }

                // 3. Fallback: Read UN1CA system property persist.sys.device_name
                if (customName.isNullOrBlank()) {
                    try {
                        val sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", classLoader)
                        val propValue = XposedHelpers.callStaticMethod(
                            sysPropClass,
                            "get",
                            "persist.sys.device_name",
                            ""
                        ) as? String

                        if (!propValue.isNullOrBlank()) {
                            customName = propValue
                        }
                    } catch (e: Throwable) {
                        // Ignore reflection errors
                    }
                }

                // Apply the override return value
                if (!customName.isNullOrBlank()) {
                    param.result = customName
                }
            }
        }

        // --- Target 1: SecDeviceInfoUtils ---
        val secDeviceInfoUtilsClass = try {
            XposedHelpers.findClass("com.samsung.android.settings.deviceinfo.SecDeviceInfoUtils", classLoader)
        } catch (e: Throwable) {
            null
        }

        if (secDeviceInfoUtilsClass != null) {
            val utilsMethods = listOf(
                "getDefaultDeviceName",
                "getOfficialDeviceName",
                "getMarketDeviceName",
                "getDeviceName"
            )

            for (methodName in utilsMethods) {
                try {
                    XposedBridge.hookAllMethods(secDeviceInfoUtilsClass, methodName, deviceNameHook)
                    Log.d(TAG, "Successfully hooked SecDeviceInfoUtils.$methodName")
                } catch (e: Throwable) {
                    Log.e(TAG, "Could not hook SecDeviceInfoUtils.$methodName", e)
                }
            }
        }

        // --- Target 2: Modern OneUI 6/7 / UN1CA Preference Controllers ---
        val headerControllers = listOf(
            "com.samsung.android.settings.deviceinfo.aboutphone.DeviceNamePreferenceController",
            "com.samsung.android.settings.deviceinfo.aboutphone.SecAboutDeviceHeaderPreferenceController",
            "com.samsung.android.settings.deviceinfo.aboutphone.SecAboutDeviceUtils"
        )

        for (controllerClassName in headerControllers) {
            try {
                val clazz = XposedHelpers.findClass(controllerClassName, classLoader)
                XposedBridge.hookAllMethods(clazz, "getDeviceName", deviceNameHook)
                XposedBridge.hookAllMethods(clazz, "getDefaultDeviceName", deviceNameHook)
                Log.d(TAG, "Successfully hooked $controllerClassName")
            } catch (e: Throwable) {
                // Ignore if class is absent in specific base
            }
        }
    }
}
