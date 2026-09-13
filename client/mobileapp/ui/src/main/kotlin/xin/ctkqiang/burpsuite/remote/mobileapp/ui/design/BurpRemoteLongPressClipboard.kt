package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.AnnotatedString

/**
 * 长按把整段文本写进剪贴板，短按原样放行给外层。
 *
 * 这里不能用 `detectTapGestures`：它在按下的一刻就消费掉事件，主通道又是子先父后，
 * 外层卡片的点击整块收不到手指——「点一下进详情」会静默失效。
 * 所以改成只在超过系统长按时限后才接管：短按不消费，长按才复制并吃掉余下事件，
 * 免得同一次按住又去触发外层点击。
 */
@Composable
fun Modifier.burpRemoteLongPressClipboard(text: String): Modifier {
    val clipboardManager = LocalClipboardManager.current
    val haptics = rememberBurpRemoteHaptics()
    val longPressTimeoutMilliseconds = LocalViewConfiguration.current.longPressTimeoutMillis

    return pointerInput(text, longPressTimeoutMilliseconds) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val wasReleasedInTime =
                withTimeout(longPressTimeoutMilliseconds) {
                    awaitPointerSequenceEnd()
                    Unit
                } != null

            if (!wasReleasedInTime) {
                clipboardManager.setText(AnnotatedString(text))
                haptics.success()
                consumeUntilPointerSequenceEnd()
            }
        }
    }
}

/**
 * 等到这一串触摸结束或被人抢走。
 *
 * 抢走指别的处理器在同一个事件里消费了指针——典型是竖向滚动接管了拖动，此时长按应当作废。
 * 不消费任何东西：短按要留给外层，这里只是旁观。
 */
private suspend fun AwaitPointerEventScope.awaitPointerSequenceEnd() {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.all { change -> !change.pressed }) return
        if (event.changes.any { change -> change.isConsumed }) return
    }
}

/**
 * 长按成立后吃掉余下的事件。
 *
 * 外层卡片的点击靠「抬起未被消费」才成立，这里必须把抬起消费掉，否则一次长按会同时复制又进详情。
 * 若期间已被别人消费则直接收工，避免和无障碍、滚动之类的外层处理器互相咬住。
 */
private suspend fun AwaitPointerEventScope.consumeUntilPointerSequenceEnd() {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.all { change -> !change.pressed }) {
            event.changes.forEach { change -> change.consume() }
            return
        }
        if (event.changes.any { change -> change.isConsumed }) return
    }
}
