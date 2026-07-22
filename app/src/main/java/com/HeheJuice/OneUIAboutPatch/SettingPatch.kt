package com.HeheJuice.OneUIAboutPatch

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
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

        XposedBridge.log("Hooking package: ${lpparam.packageName} for One UI 8")

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
                        XposedBridge.log("MyDeviceInfoFragment onViewCreated triggered on One UI 8")

                        // Since One UI 8 uses standard preference layouts, let's find the first card-like container 
                        // or recursively search for the header layout container view.
                        val headerView = findHeaderContainer(rootView)
                        
                        if (headerView != null) {
                            val cardDrawable = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = 48f
                                setColor(Color.parseColor("#1F1F1F")) // Dark card background
                                setStroke(2, Color.parseColor("#33FFFFFF")) // Subtle border stroke
                            }

                            headerView.background = cardDrawable
                            headerView.setPadding(32, 32, 32, 32)
                            XposedBridge.log("Successfully applied card background to One UI 8 About Phone header!")
                        } else {
                            XposedBridge.log("Header container view could not be resolved automatically.")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("Failed to hook MyDeviceInfoFragment on One UI 8: ${e.message}")
        }
    }

    // Helper function to safely traverse views and find the header container in One UI 8
    private fun findHeaderContainer(view: View): View? {
        if (view is ViewGroup) {
            // Check if this container matches typical header identification criteria (e.g., contains image + device name)
            // Or fallback to returning the first major layout container inside the preference list
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                // Look for resource entry names commonly found in One UI 8 headers
                val entryName = try {
                    view.resources.getResourceEntryName(child.id)
                } catch (e: Exception) {
                    ""
                }
                
                if (entryName.contains("header") || entryName.contains("status") || entryName.contains("info")) {
                    return child
                }
                
                // Recurse deeper if needed
                val nested = findHeaderContainer(child)
                if (nested != null) return nested
            }
        }
        return null
    }
}