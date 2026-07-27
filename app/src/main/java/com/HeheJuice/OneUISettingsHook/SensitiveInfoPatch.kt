package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SensitiveInfoPatch {
    private const val TAG = "SensitiveInfoPatch"
    private var isHooked = false

    private const val MASK_TEXT = "********"

    private const val FIELD_REAL_TEXT = "HeheJuice_RealText"
    private const val FIELD_IS_UNMASKED = "HeheJuice_IsUnmasked"
    private const val FIELD_HOOK_LOCK = "HeheJuice_HookLock"

    private val DATE_REGEX = Regex("""^\d{1,4}[./\-\s]\d{1,2}[./\-\s]\d{1,4}$""")
    private val MONTH_REGEX = Regex("""(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\b""")
    private val NETWORK_UNITS_REGEX = Regex("""(?i)(dBm|asu|mbps|gbps|ghz|mhz|wpa|wep)""")
    private val VERSION_OR_IP_REGEX = Regex("""^[vV]?\d{1,4}(\.\d{1,6}){1,4}(\s?[-_a-zA-Z0-9().]+)?$""")

    fun applyPatch(classLoader: ClassLoader) {
        if (isHooked) return
        try {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "setText",
                CharSequence::class.java,
                TextView.BufferType::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val tv = param.thisObject as? TextView ?: return
                            val rawInput = param.args[0]?.toString()

                            if (rawInput.isNullOrBlank() || rawInput == MASK_TEXT) return
                            if (XposedHelpers.getAdditionalInstanceField(tv, FIELD_HOOK_LOCK) == true) return

                            // NEW: Upgraded from isBatteryPage to cover Wi-Fi, Bluetooth, and Connections
                            if (shouldSkipPage(tv)) return

                            val existingReal = XposedHelpers.getAdditionalInstanceField(tv, FIELD_REAL_TEXT) as? String
                            val textToEvaluate = existingReal ?: rawInput
                            val trimmedText = textToEvaluate.trim()

                            if (trimmedText.contains(NETWORK_UNITS_REGEX)) return
                            if (trimmedText.count { it == ':' } >= 2) return
                            if (trimmedText.matches(VERSION_OR_IP_REGEX)) return

                            val cleanText = textToEvaluate
                                .replace(" ", "")
                                .replace("-", "")
                                .replace(".", "")
                                .replace("/", "")
                                .replace("(", "")
                                .replace(")", "")
                                .trim()

                            val isDate = trimmedText.matches(DATE_REGEX) ||
                                         textToEvaluate.contains(MONTH_REGEX) ||
                                         isRawDate(cleanText)

                            val isImei = cleanText.length in 14..16 && cleanText.all { it.isDigit() }

                            val isSerialNumber = cleanText.length in 10..12 &&
                                    cleanText.all { it.isLetterOrDigit() } &&
                                    cleanText.any { it.isDigit() } &&
                                    cleanText.any { it.isLetter() }

                            val isPhoneNumber = !isDate && (
                                    (cleanText.startsWith("+") &&
                                            cleanText.substring(1).all { it.isDigit() } &&
                                            cleanText.length in 9..16) ||
                                            (cleanText.all { it.isDigit() } &&
                                                    cleanText.length in 8..11)
                                    )

                            if (isImei || isSerialNumber || isPhoneNumber) {
                                XposedHelpers.setAdditionalInstanceField(tv, FIELD_REAL_TEXT, textToEvaluate)
                                val isUnmasked = XposedHelpers.getAdditionalInstanceField(tv, FIELD_IS_UNMASKED) == true

                                if (!isUnmasked) {
                                    param.args[0] = MASK_TEXT
                                }

                                if (!tv.isClickable && existingReal == null) {
                                    tv.isClickable = true
                                    tv.setOnClickListener { view ->
                                        val targetTv = view as? TextView ?: return@setOnClickListener
                                        val realVal = XposedHelpers.getAdditionalInstanceField(targetTv, FIELD_REAL_TEXT) as? String ?: return@setOnClickListener
                                        val currentlyUnmasked = XposedHelpers.getAdditionalInstanceField(targetTv, FIELD_IS_UNMASKED) == true

                                        val nextState = !currentlyUnmasked
                                        XposedHelpers.setAdditionalInstanceField(targetTv, FIELD_IS_UNMASKED, nextState)
                                        XposedHelpers.setAdditionalInstanceField(targetTv, FIELD_HOOK_LOCK, true)
                                        targetTv.text = if (nextState) realVal else MASK_TEXT
                                        XposedHelpers.setAdditionalInstanceField(targetTv, FIELD_HOOK_LOCK, false)
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            // Suppress logs
                        }
                    }
                }
            )

            isHooked = true
            Log.d(TAG, "Successfully initialized global TextView setText sensitive info hook.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply SensitiveInfoPatch hooks", e)
        }
    }

    private fun isRawDate(cleanText: String): Boolean {
        if (cleanText.length != 8) return false
        if (!cleanText.startsWith("19") && !cleanText.startsWith("20")) return false
        val month = cleanText.substring(4, 6).toIntOrNull() ?: 0
        val day = cleanText.substring(6, 8).toIntOrNull() ?: 0
        return month in 1..12 && day in 1..31
    }

    /**
     * Checks if the TextView resides inside a page where we want to skip masking entirely.
     * This avoids accidentally masking Wi-Fi SSIDs, Bluetooth device names, or battery stats.
     */
    private fun shouldSkipPage(tv: TextView): Boolean {
        val activity = getActivity(tv.context) ?: return false

        val activityName = activity.javaClass.name.lowercase()
        val showFragment = activity.intent?.getStringExtra(":settings:show_fragment")?.lowercase() ?: ""
        val title = activity.title?.toString()?.lowercase() ?: ""

        // Array of keywords to ignore masking on
        val skipKeywords = arrayOf(
            "battery", 
            "wifi", 
            "wi-fi", 
            "wlan", 
            "connections", 
            "network", 
            "bluetooth"
        )

        for (keyword in skipKeywords) {
            if (activityName.contains(keyword) || showFragment.contains(keyword) || title.contains(keyword)) {
                return true
            }
        }
        return false
    }

    private fun getActivity(context: Context?): Activity? {
        var currentContext = context
        while (currentContext != null) {
            if (currentContext is Activity) return currentContext
            if (currentContext is ContextWrapper) {
                val base = currentContext.baseContext
                if (base === currentContext) break
                currentContext = base
            } else {
                break
            }
        }
        return null
    }
}
