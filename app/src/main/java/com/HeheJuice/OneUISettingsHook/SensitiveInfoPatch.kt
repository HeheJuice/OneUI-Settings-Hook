package com.HeheJuice.OneUISettingsHook

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
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

                            // 新增：统一跳过判断（包含电池页、WiFi、版本、信号等）
                            if (shouldSkipMasking(tv, rawInput)) return

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

                            // Matches formatted dates or strings with month names
                            val isFormattedDate = textToEvaluate.trim().matches(
                                Regex("""^\d{1,4}[./\-\s]\d{1,2}[./\-\s]\d{1,4}$""")
                            )

                            val hasMonthName = textToEvaluate.contains(
                                Regex("""(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\b""")
                            )

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

    /**
     * 综合判断是否应该跳过掩码逻辑
     * 包括：电池信息页、WiFi相关页、关于手机/软件信息页、以及包含特定关键词的文本
     */
    private fun shouldSkipMasking(tv: TextView, rawText: String): Boolean {
        // 1. 原有电池页面跳过
        if (isBatteryPage(tv)) return true

        val activity = getActivity(tv.context)
        if (activity != null) {
            val activityName = activity.javaClass.name.lowercase()
            val fragment = activity.intent?.getStringExtra(":settings:show_fragment")?.lowercase() ?: ""

            // 2. WiFi / 网络相关页面（WiFi名称、信号强度）
            if (activityName.contains("wifi") || activityName.contains("network") ||
                fragment.contains("wifi") || fragment.contains("network")) {
                return true
            }

            // 3. 关于手机 / 软件信息页面（应用版本）
            if (activityName.contains("about") || activityName.contains("software") ||
                activityName.contains("version") || fragment.contains("about") ||
                fragment.contains("software") || fragment.contains("version")) {
                return true
            }

            // 4. 状态信息页面（信号强度等常见于“状态”或“SIM卡状态”）
            if (activityName.contains("status") || activityName.contains("sim") ||
                fragment.contains("status") || fragment.contains("sim")) {
                return true
            }
        }

        // 5. 文本内容关键词（额外保险，但避免误判）
        val lowerText = rawText.lowercase()
        if (lowerText.contains("wifi") || lowerText.contains("ssid") ||
            lowerText.contains("版本") || lowerText.contains("version") ||
            lowerText.contains("信号") || lowerText.contains("signal") ||
            lowerText.contains("dbm") || lowerText.contains("强度")) {
            // 若文本看起来像版本号或信号值，可以跳过（例如“版本号: 12.0.1”）
            // 但这里简单返回 true，实际可根据需要细化
            return true
        }

        // 6. 特定视图 ID（如果有明确资源 ID 可在此添加，例如 R.id.wifi_ssid）
        // if (tv.id == R.id.wifi_ssid_text) return true

        return false
    }

    /**
     * 检查是否位于电池信息页面（原有方法）
     */
    private fun isBatteryPage(tv: TextView): Boolean {
        val activity = getActivity(tv.context) ?: return false

        // 1. 检查 Activity 类名
        val activityName = activity.javaClass.name.lowercase()
        if (activityName.contains("battery")) return true

        // 2. 检查 Fragment 目标
        val showFragment = activity.intent?.getStringExtra(":settings:show_fragment")?.lowercase() ?: ""
        if (showFragment.contains("battery")) return true

        // 3. 检查 Activity 标题
        val title = activity.title?.toString()?.lowercase() ?: ""
        if (title.contains("battery")) return true

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