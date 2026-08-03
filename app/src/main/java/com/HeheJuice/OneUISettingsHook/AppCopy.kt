package com.HeheJuice.OneUISettingsHook

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object AppCopy {
    private const val TAG = "DualAppPatch"
    private const val TARGET_PACKAGE = "com.samsung.android.da.daagent"

    fun applyPatch(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) return

        try {
            // Hook DAUtility to refresh the whitelist right before it gets updated in the System Server
            val utilityClass = "com.samsung.android.da.daagent.utils.DAUtility"
            
            XposedHelpers.findAndHookMethod(
                utilityClass,
                lpparam.classLoader,
                "updateWhitelistAppsInSystemServer",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as? Context ?: return
                        refreshWhiteList(context, lpparam.classLoader)
                    }
                }
            )
            Log.d(TAG, "Successfully hooked Dual Messenger (DAAgent) whitelist generation.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to hook Dual App Agent", e)
        }
    }

    private fun refreshWhiteList(context: Context, classLoader: ClassLoader) {
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            // Query all apps with launcher activities
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            val whiteList = mutableListOf<String>()

            for (resolveInfo in resolveInfos) {
                val appInfo = resolveInfo.activityInfo.applicationInfo
                
                // Emulate Smali bitmask check: flags & 0x81 (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)
                val isSystemApp = (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                
                // Emulate metadata check
                val isInstallOnlyOwner = appInfo.metaData?.getBoolean("com.samsung.android.multiuser.install_only_owner", false) ?: false

                // If it's a user app and not strictly limited to the owner profile, add it to the list
                if (!isSystemApp && !isInstallOnlyOwner) {
                    whiteList.add(appInfo.packageName)
                }
            }

            // Replace the static hardcoded list in WhiteListApps with our dynamic array
            val whiteListAppsClass = XposedHelpers.findClass(
                "com.samsung.android.da.daagent.provider.WhiteListApps", 
                classLoader
            )
            
            XposedHelpers.setStaticObjectField(
                whiteListAppsClass, 
                "DUAL_APP_WHITELIST_PACKAGES", 
                whiteList.toTypedArray()
            )

            Log.d(TAG, "Injected ${whiteList.size} apps into Dual Messenger whitelist.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error refreshing Dual Messenger whitelist", e)
        }
    }
}
