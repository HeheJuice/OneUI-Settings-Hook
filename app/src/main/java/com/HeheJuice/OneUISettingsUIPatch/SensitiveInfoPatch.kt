package com.HeheJuice.OneUISettingsUIPatch

import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SensitiveInfoPatch {
    private const val TAG = "SensitiveInfoPatch"
    private var isHooked = false

    // Custom View Tag keys to store real text and toggle state per TextView
    private const val TAG_REAL_TEXT = 0x7f099991
    private const val TAG_IS_UNMASKED = 0x7f099992
    private const val TAG_HOOK_LOCK = 0x7f099993

    fun applyPatch(classLoader: ClassLoader) {
        if (isHooked) return
        try {
            // Hook TextView.setText(CharSequence, BufferType) directly.
            // This catches all dynamic updates, including Samsung's custom top card views.
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

                            if (rawInput.isBlank() || rawInput == "***") return

                            // Skip if we are manually toggling text via click listener
                            if (tv.getTag(TAG_HOOK_LOCK) == true) return

                            val existingReal = tv.getTag(TAG_REAL_TEXT) as? String
                            val textToEvaluate = existingReal ?: rawInput

                            // Clean spaces/dashes for pattern checking
                            val cleanText = textToEvaluate.replace(" ", "").replace("-", "").trim()

                            // 1. IMEI: 14 to 16 digits
                            val isImei = cleanText.length in 14..16 && cleanText.all { it.isDigit() }

                            // 2. Samsung Serial Number: 10 to 12 alphanumeric characters with letters & numbers
                            val isSerialNumber = cleanText.length in 10..12 && 
                                                 cleanText.all { it.isLetterOrDigit() } && 
                                                 cleanText.any { it.isDigit() } && 
                                                 cleanText.any { it.isLetter() }

                            if (isImei || isSerialNumber) {
                                // Save original sensitive value
                                tv.setTag(TAG_REAL_TEXT, textToEvaluate)

                                val isUnmasked = tv.getTag(TAG_IS_UNMASKED) == true

                                if (!isUnmasked) {
                                    param.args[0] = "***"
                                }

                                // Attach tap listener to the TextView
                                tv.isClickable = true
                                tv.setOnClickListener { view ->
                                    val targetTv = view as? TextView ?: return@setOnClickListener
                                    val realVal = targetTv.getTag(TAG_REAL_TEXT) as? String ?: return@setOnClickListener
                                    val currentlyUnmasked = targetTv.getTag(TAG_IS_UNMASKED) == true

                                    val nextState = !currentlyUnmasked
                                    targetTv.setTag(TAG_IS_UNMASKED, nextState)
                                    targetTv.setTag(TAG_HOOK_LOCK, true)
                                    targetTv.text = if (nextState) realVal else "***"
                                    targetTv.setTag(TAG_HOOK_LOCK, false)
                                }

                                // Expand clickable touch area to the parent container row
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
