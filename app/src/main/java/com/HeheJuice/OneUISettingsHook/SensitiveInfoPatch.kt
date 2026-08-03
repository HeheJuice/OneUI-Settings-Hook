package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.util.WeakHashMap

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

    // WeakHashMap cache so activity checks happen once per Activity lifecycle, eliminating UI jank
    private val skipActivityCache = WeakHashMap<Activity, Boolean>()

    private val SKIP_KEYWORDS = arrayOf(
        "battery", "wifi", "wi-fi", "wlan", "connections",
        "network", "bluetooth", "location", "storage",
        "cache", "name", "devicename", "san", "app"
    )

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

                            // Fast Path Exit: Most sensitive data (IMEI, Serial, Phone) falls within 8 to 40 chars.
                            // Skips heavy evaluation on short labels ("OK", "Back") or long paragraphs.
                            val inputLength = rawInput.length
                            if (inputLength < 8 || inputLength > 40) return

                            // Check cached page status
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

                            if (isDate) return

                            // Refined sensitivity matching
                            val isImei = cleanText.length in 14..16 && cleanText.all { it.isDigit() } && passesLuhnCheck(cleanText)
                            val isSerial = isSerialNumber(cleanText)
                            val isPhoneNumber = (cleanText.startsWith("+") && cleanText.substring(1).all { it.isDigit() } && cleanText.length in 9..16) ||
                                                (cleanText.all { it.isDigit() } && cleanText.length in 10..11 && (cleanText.startsWith("1") || cleanText.startsWith("0")))

                            if (isImei || isSerial || isPhoneNumber) {
                                XposedHelpers.setAdditionalInstanceField(tv, FIELD_REAL_TEXT, textToEvaluate)
                                val isUnmasked = XposedHelpers.getAdditionalInstanceField(tv, FIELD_IS_UNMASKED) == true

                                if (!isUnmasked) {
                                    param.args[0] = MASK_TEXT
                                }

                                if (existingReal == null) {
                                    tv.isClickable = true
                                    tv.setOnClickListener { view ->
                                        val targetTv = view as? TextView ?: return@setOnClickListener
                                        val realVal = XposedHelpers.getAdditionalInstanceField(targetTv, FIELD_REAL_TEXT) as? String ?: return@setOnClickListener
                                        val currentlyUnmasked = XposedHelpers.getAdditionalInstanceField(targetTv, FIELD_IS_UNMASKED) == true

                                        val nextState = !currentlyUnmasked
                                        XposedHelpers.setAdditionalInstanceField(targetTv, FIELD_IS_UNMASKED, nextState)

                                        try {
                                            XposedHelpers.setAdditionalInstanceField(targetTv, FIELD_HOOK_LOCK, true)
                                            targetTv.text = if (nextState) realVal else MASK_TEXT
                                        } finally {
                                            XposedHelpers.setAdditionalInstanceField(targetTv, FIELD_HOOK_LOCK, false)
                                        }
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            // Suppress exceptions in high-frequency hooks to avoid log spam
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

    /**
     * Single-pass, zero-allocation check for hardware serial numbers.
     * Ensures only ASCII alphanumeric strings (10-12 chars) trigger the mask.
     */
    private fun isSerialNumber(text: String): Boolean {
        if (text.length !in 10..12) return false

        var hasDigit = false
        var hasLetter = false

        for (i in 0 until text.length) {
            val c = text[i]
            when (c) {
                in '0'..'9' -> hasDigit = true
                in 'a'..'z', in 'A'..'Z' -> hasLetter = true
                else -> return false // Immediately drops out if non-ASCII character (like Hangul/Emojis)
            }
        }

        return hasDigit && hasLetter
    }

    private fun isRawDate(cleanText: String): Boolean {
        if (cleanText.length != 8) return false
        if (!cleanText.startsWith("19") && !cleanText.startsWith("20")) return false
        val month = cleanText.substring(4, 6).toIntOrNull() ?: 0
        val day = cleanText.substring(6, 8).toIntOrNull() ?: 0
        return month in 1..12 && day in 1..31
    }

    /**
     * Luhn algorithm validation for 15-digit IMEIs to avoid false-positive masking on random numbers.
     */
    private fun passesLuhnCheck(number: String): Boolean {
        if (number.length != 15) return true // Fallback for 14-digit or 16-digit IMEISV
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun shouldSkipPage(tv: TextView): Boolean {
        val context = tv.context ?: return false
        val activity = getActivity(context) ?: return false

        return skipActivityCache.getOrPut(activity) {
            val activityName = activity.javaClass.name.lowercase()
            val showFragment = activity.intent?.getStringExtra(":settings:show_fragment")?.lowercase() ?: ""
            val title = activity.title?.toString()?.lowercase() ?: ""

            SKIP_KEYWORDS.any { keyword ->
                activityName.contains(keyword) || showFragment.contains(keyword) || title.contains(keyword)
            }
        }
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
