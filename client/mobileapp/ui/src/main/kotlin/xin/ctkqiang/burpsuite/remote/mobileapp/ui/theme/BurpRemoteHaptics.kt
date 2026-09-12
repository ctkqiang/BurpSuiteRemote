package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * 触感反馈。
 *
 * 五种动作分别落到不同强度的系统触感：如果只有一种「震一下」，用户就分不清刚才是选中了还是失败了。
 * 系统没有对应触感的版本会静默不震，不做降级补偿——假装震一下比不震更误导人。
 */
class BurpRemoteHaptics(
    private val hapticFeedback: HapticFeedback,
) {
    /** 点击：高频、最轻的一档。 */
    fun tap() = hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)

    /** 切换：选中/取消这类状态变化。 */
    fun select() = hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)

    /** 成功：例如配对通过、导出完成。 */
    fun success() = hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** 警告：还能继续，但需要留意。 */
    fun warning() = hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

    /** 失败：命令被拒或链路断开。 */
    fun failure() = hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
}
