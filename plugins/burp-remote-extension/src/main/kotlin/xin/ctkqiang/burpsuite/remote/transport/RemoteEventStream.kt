// 事件流的读取端口。

package xin.ctkqiang.burpsuite.remote.transport

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope

/**
 * 事件通道读取事件日志的端口。
 *
 * 传输层只按序号读取；事件从哪来、写在哪，是事件层与适配层的事（adapter 尚未实现时，日志就是空的）。
 */
interface RemoteEventStream {
    /** 日志中仍能提供的最小序号；日志为空时返回 1，于是「从零续传」永远成立。 */
    fun earliestAvailableSequenceNumber(): Long

    /** 已产生的最新序号；尚未产生任何事件时返回 0。 */
    fun latestSequenceNumber(): Long

    /**
     * 订阅实时事件。
     *
     * 这是热流：订阅者先收到仍在保留窗口内的事件，再收到后续新事件。重复投递是允许的
     * （at-least-once），去重由客户端按 eventIdentifier 与 sequenceNumber 完成（rules.md §5.5）。
     */
    fun observeEvents(): Flow<RemoteEventEnvelope>
}
