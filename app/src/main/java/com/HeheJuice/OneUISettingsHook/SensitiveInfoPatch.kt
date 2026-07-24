package com.HeheJuice.OneUISettingsHook

import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SensitiveInfoPatch {
    private const val TAG = "SensitiveInfoPatch"
    private var isHooked = false

    private const val MASK_TEXT = "********" 

    private const val TAG_REAL_TEXT = 0x7f099991
    private const val TAG_IS_UNMASKED = 0x7f099992
    private const val TAG_HOOK_LOCK = 0x7f099993

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
                            val rawInput = param.args[0]?.toString() ?: return

                            if (rawInput.isBlank() || rawInput == MASK_TEXT) return

                            if (tv.getTag(TAG_HOOK_LOCK) == true) return

                            val existingReal = tv.getTag(TAG_REAL_TEXT) as? String
                            val textToEvaluate = existingReal ?: rawInput

                            val cleanText = textToEvaluate
                                .replace(" ", "")
                                .replace("-", "")
                                .replace(".", "")
                                .replace("/", "")
                                .replace("(", "")
                                .replace(")", "")
                                .trim()

                            // 1. Matches formatted date strings like "06/01/2022", "06.01.2022", "2022-01-06"
                            val isFormattedDate = textToEvaluate.trim().matches(
                                Regex("""^\d{1,4}[./\-\s]\d{1,2}[./\-\s]\d{1,4}$""")
                            )

                            // 2. Matches dates with month names like "06 January 2022" or "Jan 2022"
                            val hasMonthName = textToEvaluate.contains(
                                Regex("""(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\b""")
                            )

                            // 3. Validates YYYYMMDD numeric strings against valid month (1..12) and day (1..31) ranges
                            val isRawYyyymmdd = cleanText.length == 8 && 
                                (cleanText.startsWith("19") || cleanText.startsWith("20")) &&
                                (cleanText.substring(4, 6).toIntOrNull() in 1..12) &&
                                (cleanText.substring(6, 8).toIntOrNull() in 1..31)

                            val isDate = isFormattedDate || hasMonthName || isRawYyyymmdd

                            val isImei = cleanText.length in 14..16 && cleanText.all { it.isDigit() }

                            val isSerialNumber = cleanText.length in 10..12 && 
                                                 cleanText.all { it.isLetterOrDigit() } && 
                                                 cleanText.any { it.isDigit() } && 
                                                 cleanText.any { it.isLetter() }

                            // Exclude dates from being categorized as phone numbers
                            val isPhoneNumber = !isDate && (
                                (cleanText.startsWith("+") && 
                                 cleanText.substring(1).all { it.isDigit() } && 
                                 cleanText.length in 9..16) ||
                                (cleanText.all { it.isDigit() } && 
                                 cleanText.length in 8..11)
                            )

                            if (isImei || isSerialNumber || isPhoneNumber) {
                                tv.setTag(TAG_REAL_TEXT, textToEvaluate)

                                val isUnmasked = tv.getTag(TAG_IS_UNMASKED) == true

                                if (!isUnmasked) {
                                    param.args[0] = MASK_TEXT
                                }

                                tv.isClickable = true
                                tv.setOnClickListener { view ->
                                    val targetTv = view as? TextView ?: return@setOnClickListener
                                    val realVal = targetTv.getTag(TAG_REAL_TEXT) as? String ?: return@setOnClickListener
                                    val currentlyUnmasked = targetTv.getTag(TAG_IS_UNMASKED) == true

                                    val nextState = !currentlyUnmasked
                                    targetTv.setTag(TAG_IS_UNMASKED, nextState)
                                    targetTv.setTag(TAG_HOOK_LOCK, true)
                                    targetTv.text = if (nextState) realVal else MASK_TEXT
                                    targetTv.setTag(TAG_HOOK_LOCK, false)
                                }

                                (tv.parent as? ViewGroup)?.let { parent ->
                                    parent.isClickable = true
                                    parent.setOnClickListener {
                                        tv.performClick()
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error in TextView.setText hook", e)
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
}
