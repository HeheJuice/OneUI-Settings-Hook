package com.HeheJuice.OneUIAboutPatch

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedMain : IXposedHookLoadPackage {

    companion object {
        private const val TARGET_PACKAGE = "com.android.settings"
        private const val TARGET_FRAGMENT = "com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) return

        XposedBridge.log("Hooking package: ${lpparam.packageName}")

        try {
            XposedHelpers.findAndHookMethod(
                TARGET_FRAGMENT,
                lpparam.classLoader,
                "onViewCreated",
                View::class.java,
                android.os.Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val rootView = param.args[0] as? View ?: return
                        val context = rootView.context

                        // Locate the header container view ID safely at runtime
                        val headerId = context.resources.getIdentifier(
                            "sec_device_info_settings_header",
                            "id",
                            context.packageName
                        )

                        if (headerId != 0) {
                            val headerView = rootView.findViewById<View>(headerId)
                            if (headerView != null) {
                                // Build the One UI style card background with rounded corners and border stroke
                                val cardDrawable = GradientDrawable().apply {
                                    shape = GradientDrawable.RECTANGLE
                                    cornerRadius = 48f
                                    setColor(Color.parseColor("#1F1F1F")) // Dark background card color
                                    setStroke(2, Color.parseColor("#33FFFFFF")) // Subtle border stroke
                                }

                                headerView.background = cardDrawable
                                headerView.setPadding(32, 32, 32, 32)
                                XposedBridge.log("Successfully applied card background to About Phone header.")
                            }
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("Failed to hook MyDeviceInfoFragment: ${e.message}")
        }
    }
}
