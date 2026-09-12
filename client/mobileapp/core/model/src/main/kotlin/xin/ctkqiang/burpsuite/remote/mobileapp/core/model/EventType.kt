// 事件的线上类型串。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 事件的线上类型串；用值类型而不是枚举，是为了让新版插件引入的未知类型能被原样保留。
 *
 * @property value 点分类型串，例如 history.item.observed。
 */
@JvmInline
value class EventType(val value: String) {
    companion object {
        /** 设备已连接。 */
        val DEVICE_CONNECTED: EventType = EventType("device.connected")

        /** 设备已断开。 */
        val DEVICE_DISCONNECTED: EventType = EventType("device.disconnected")

        /** 设备已完成配对。 */
        val DEVICE_PAIRED: EventType = EventType("device.paired")

        /** 设备已解除配对。 */
        val DEVICE_UNPAIRED: EventType = EventType("device.unpaired")

        /** HTTP 历史记录已被观察到。 */
        val HISTORY_ITEM_OBSERVED: EventType = EventType("history.item.observed")

        /** HTTP 历史记录已被保存。 */
        val HISTORY_ITEM_SAVED: EventType = EventType("history.item.saved")

        /** 历史记录上新增了一条注解。 */
        val HISTORY_ANNOTATION_ADDED: EventType = EventType("history.annotation.added")

        /** 拦截项已产生。 */
        val INTERCEPT_CREATED: EventType = EventType("intercept.created")

        /** 拦截项已被修改。 */
        val INTERCEPT_MODIFIED: EventType = EventType("intercept.modified")

        /** 拦截项已被放行。 */
        val INTERCEPT_FORWARDED: EventType = EventType("intercept.forwarded")

        /** 拦截项已被丢弃。 */
        val INTERCEPT_DROPPED: EventType = EventType("intercept.dropped")

        /** Repeater 请求已创建。 */
        val REPEATER_CREATED: EventType = EventType("repeater.created")

        /** Repeater 执行已开始。 */
        val REPEATER_EXECUTION_STARTED: EventType = EventType("repeater.execution.started")

        /** Repeater 执行已完成。 */
        val REPEATER_EXECUTION_COMPLETED: EventType = EventType("repeater.execution.completed")

        /** 截图已导入。 */
        val SCREENSHOT_IMPORTED: EventType = EventType("screenshot.imported")

        /** 截图已美化。 */
        val SCREENSHOT_BEAUTIFIED: EventType = EventType("screenshot.beautified")
    }
}
