package com.HeheJuice.OneUISettingsUIPatch

import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SensitiveInfoPatch {
    private const val TAG = "SensitiveInfoPatch"
    private var isHooked = false

    // Stores preference keys currently toggled to unmasked/revealed state
    private val unmaskedKeys = mutableSetOf<String>()

    // Custom View Tag IDs to store original unmasked text
    private const val TAG_REAL_TEXT = 0x7f099991

    fun applyPatch(classLoader: ClassLoader) {
        if (isHooked) return
        try {
            val preferenceClass = XposedHelpers.findClass("androidx.preference.Preference", classLoader)
            
            // Hook preference binding to mask text and attach tap-to-toggle listener
            XposedHelpers.findAndHookMethod(
                preferenceClass,
                "onBindViewHolder",
                XposedHelpers.findClass("androidx.preference.PreferenceViewHolder", classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val preference = param.thisObject
                            val key = XposedHelpers.callMethod(preference, "getKey") as? String 
                                ?: preference.hashCode().toString()
                            
                            val holder = param.args[0]
                            val itemView = XposedHelpers.getObjectField(holder, "itemView") as? ViewGroup ?: return

                            processPreferenceView(itemView, key)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error binding sensitive preference view", e)
                        }
                    }
                }
            )

            isHooked = true
            Log.d(TAG, "Successfully initialized tap-to-toggle sensitive info patch.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply SensitiveInfoPatch hooks", e)
        }
    }

    private fun processPreferenceView(itemView: ViewGroup, prefKey: String) {
        val sensitiveTextViews = mutableListOf<TextView>()
        findSensitiveTextViews(itemView, sensitiveTextViews)

        if (sensitiveTextViews.isEmpty()) return

        // 1. Store real text and set initial state
        for (tv in sensitiveTextViews) {
            var realText = tv.getTag(TAG_REAL_TEXT) as? String
            if (realText == null) {
                realText = tv.text.toString().trim()
                tv.setTag(TAG_REAL_TEXT, realText)
            }

            val isUnmasked = unmaskedKeys.contains(prefKey)
            tv.text = if (isUnmasked) realText else "***"
        }

        // 2. Add tap-to-toggle click listener on the item row
        itemView.setOnClickListener {
            val currentState = unmaskedKeys.contains(prefKey)
            if (currentState) {
                unmaskedKeys.remove(prefKey)
            } else {
                unmaskedKeys.add(prefKey)
            }

            val newIsUnmasked = !currentState
            for (tv in sensitiveTextViews) {
                val realText = tv.getTag(TAG_REAL_TEXT) as? String ?: continue
                tv.text = if (newIsUnmasked) realText else "***"
            }
        }
    }

    /**
     * Recursively identifies TextViews containing IMEI numbers or Serial Numbers.
     */
    private fun findSensitiveTextViews(viewGroup: ViewGroup, resultList: MutableList<TextView>) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is TextView) {
                val text = (child.getTag(TAG_REAL_TEXT) as? String) ?: child.text.toString().trim()
                
                // Pattern 1: 14-15 digit pure numeric string (IMEI / MEID)
                val isImei = text.length >= 14 && text.all { it.isDigit() }
                
                // Pattern 2: 10+ alphanumeric characters with digits (Samsung Serial Number)
                val isSerialNumber = text.length >= 10 && text.all { it.isLetterOrDigit() } && text.any { it.isDigit() }

                if (isImei || isSerialNumber) {
                    resultList.add(child)
                }
            } else if (child is ViewGroup) {
                findSensitiveTextViews(child, resultList)
            }
        }
    }
}
