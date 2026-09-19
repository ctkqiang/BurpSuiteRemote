// 把 Repeater 事件折成读模型。

package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord
import java.time.Instant

/**
 * 把 Repeater 事件折成读模型。
 *
 * 纯函数：无 I/O、无时钟、无随机（rules.md §5.6）。三次事件驱动一条记录的生命周期：
 * created 生成条目 → started 置 isExecuting=true → completed 填执行结果并置 isExecuting=false。
 */
object RepeaterProjectionReducer : ProjectionReducer<RepeaterProjectionState> {
    /** 这条事件落在哪个 Repeater 请求上；不属于 Repeater 投影的事件返回 null。 */
    fun affectedRepeaterIdentifier(event: RecordedEvent): RepeaterRequestIdentifier? =
        when (event.eventType) {
            EventType.REPEATER_CREATED,
            EventType.REPEATER_EXECUTION_STARTED,
            EventType.REPEATER_EXECUTION_COMPLETED,
            -> RepeaterRequestIdentifier(event.aggregateIdentifier.value)
            else -> null
        }

    override fun reduce(
        state: RepeaterProjectionState,
        event: RecordedEvent,
    ): RepeaterProjectionState {
        if (event.eventIdentifier in state.appliedEventIdentifiers) return state

        val stateWithAppliedEvent =
            state.copy(appliedEventIdentifiers = state.appliedEventIdentifiers + event.eventIdentifier)
        val identifier = affectedRepeaterIdentifier(event) ?: return stateWithAppliedEvent
        val existingRecord = stateWithAppliedEvent.recordsByIdentifier[identifier]
        val updatedRecord = reduceRecord(existingRecord, identifier, event) ?: return stateWithAppliedEvent

        return stateWithAppliedEvent.copy(
            recordsByIdentifier = stateWithAppliedEvent.recordsByIdentifier + (identifier to updatedRecord),
        )
    }

    private fun reduceRecord(
        existingRecord: RepeaterRecord?,
        identifier: RepeaterRequestIdentifier,
        event: RecordedEvent,
    ): RepeaterRecord? {
        val record = existingRecord ?: emptyRepeaterRecord(identifier, event)
        val attributes = event.payloadAttributes

        return when (event.eventType) {
            EventType.REPEATER_CREATED ->
                record.copy(
                    sequenceNumber = event.sequenceNumber,
                    updatedAt = event.occurredAt,
                    requestText = attributes.text(REQUEST_TEXT_FIELD) ?: record.requestText,
                    tabName = attributes.text(TAB_NAME_FIELD) ?: record.tabName,
                    createdAt =
                        attributes.long(CREATED_AT_FIELD)?.let { Instant.ofEpochMilli(it) }
                            ?: record.createdAt,
                )
            EventType.REPEATER_EXECUTION_STARTED ->
                record.copy(
                    sequenceNumber = event.sequenceNumber,
                    updatedAt = event.occurredAt,
                    isExecuting = true,
                )
            EventType.REPEATER_EXECUTION_COMPLETED ->
                record.copy(
                    sequenceNumber = event.sequenceNumber,
                    updatedAt = event.occurredAt,
                    isExecuting = false,
                    lastExecutedAt =
                        attributes.long(LAST_EXECUTED_AT_FIELD)?.let { Instant.ofEpochMilli(it) }
                            ?: event.occurredAt,
                    lastStatusCode = attributes.integer(LAST_STATUS_CODE_FIELD) ?: record.lastStatusCode,
                    lastDurationMilliseconds = attributes.long(LAST_DURATION_FIELD) ?: record.lastDurationMilliseconds,
                    lastExecutionFailed = attributes.boolean(EXECUTION_FAILED_FIELD) ?: record.lastExecutionFailed,
                )
            else -> existingRecord
        }
    }

    private fun emptyRepeaterRecord(
        identifier: RepeaterRequestIdentifier,
        event: RecordedEvent,
    ): RepeaterRecord =
        RepeaterRecord(
            repeaterRequestIdentifier = identifier,
            sequenceNumber = event.sequenceNumber,
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
            requestText = null,
            tabName = null,
            lastExecutedAt = null,
            lastStatusCode = null,
            lastDurationMilliseconds = null,
            lastExecutionFailed = null,
            isExecuting = false,
        )

    // 载荷字段名按插件端 REPEATER_*_FIELD 常量定；集中在 reducer 一处，免得散在各处。
    private const val REQUEST_TEXT_FIELD = "requestText"
    private const val TAB_NAME_FIELD = "tabName"
    private const val CREATED_AT_FIELD = "createdAtEpochMilliseconds"
    private const val LAST_EXECUTED_AT_FIELD = "lastExecutedAtEpochMilliseconds"
    private const val LAST_STATUS_CODE_FIELD = "lastStatusCode"
    private const val LAST_DURATION_FIELD = "lastDurationMilliseconds"
    private const val EXECUTION_FAILED_FIELD = "failed"
}
