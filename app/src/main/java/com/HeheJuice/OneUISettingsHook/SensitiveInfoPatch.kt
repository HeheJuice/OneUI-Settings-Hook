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

                            // Check if the text is a formatted date (e.g., 2024-05-12, 2024.05.12, 20240512)
                            val isDate = textToEvaluate.matches(Regex("""^\d{4}[./-]\d{1,2}[./-]\d{1,2}$""")) ||
                                         (cleanText.length == 8 && (cleanText.startsWith("19") || cleanText.startsWith("20")))

                            val isImei = cleanText.length in 14..16 && cleanText.all { it.isDigit() }

                            val isSerialNumber = cleanText.length in 10..12 && 
                                                 cleanText.all { it.isLetterOrDigit() } && 
                                                 cleanText.any { it.isDigit() } && 
                                                 cleanText.any { it.isLetter() }

                            // Exclude dates from being flagged as phone numbers
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
