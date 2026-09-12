package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState

/**
 * 把拦截事件折成读模型。
 *
 * 纯函数：无 I/O、无时钟、无随机（rules.md §5.6）。放行与丢弃是终态，再收到创建事件也只是把同一条记录重置回队列。
 */
object InterceptProjectionReducer : ProjectionReducer<InterceptProjectionState> {
    /** 这条事件落在哪个拦截项上；不属于拦截投影的事件返回 null。 */
    fun affectedInterceptIdentifier(event: RecordedEvent): InterceptIdentifier? =
        when (event.eventType) {
            EventType.INTERCEPT_CREATED,
            EventType.INTERCEPT_MODIFIED,
            EventType.INTERCEPT_FORWARDED,
            EventType.INTERCEPT_DROPPED,
            -> InterceptIdentifier(event.aggregateIdentifier.value)
            else -> null
        }

    override fun reduce(
        state: InterceptProjectionState,
        event: RecordedEvent,
    ): InterceptProjectionState {
        if (event.eventIdentifier in state.appliedEventIdentifiers) return state

        val stateWithAppliedEvent =
            state.copy(appliedEventIdentifiers = state.appliedEventIdentifiers + event.eventIdentifier)
        val interceptIdentifier = affectedInterceptIdentifier(event) ?: return stateWithAppliedEvent
        val existingRecord = stateWithAppliedEvent.recordsByIdentifier[interceptIdentifier]
        val updatedRecord = reduceRecord(existingRecord, interceptIdentifier, event) ?: return stateWithAppliedEvent

        return stateWithAppliedEvent.copy(
            recordsByIdentifier = stateWithAppliedEvent.recordsByIdentifier + (interceptIdentifier to updatedRecord),
        )
    }

    private fun reduceRecord(
        existingRecord: InterceptRecord?,
        interceptIdentifier: InterceptIdentifier,
        event: RecordedEvent,
    ): InterceptRecord? {
        val record = existingRecord ?: emptyInterceptRecord(interceptIdentifier, event)
        val attributes = event.payloadAttributes

        return when (event.eventType) {
            EventType.INTERCEPT_CREATED ->
                record.copy(
                    sequenceNumber = event.sequenceNumber,
                    updatedAt = event.occurredAt,
                    state = InterceptState.Pending,
                    host = attributes.text(HOST_FIELD) ?: record.host,
                    method = attributes.text(METHOD_FIELD) ?: record.method,
                    path = attributes.text(PATH_FIELD) ?: record.path,
                )
            EventType.INTERCEPT_MODIFIED ->
                record.copy(
                    sequenceNumber = event.sequenceNumber,
                    updatedAt = event.occurredAt,
                    state = InterceptState.Modified,
                )
            EventType.INTERCEPT_FORWARDED ->
                record.copy(
                    sequenceNumber = event.sequenceNumber,
                    updatedAt = event.occurredAt,
                    state = InterceptState.Forwarded,
                )
            EventType.INTERCEPT_DROPPED ->
                record.copy(
                    sequenceNumber = event.sequenceNumber,
                    updatedAt = event.occurredAt,
                    state = InterceptState.Dropped,
                )
            else -> existingRecord
        }
    }

    private fun emptyInterceptRecord(
        interceptIdentifier: InterceptIdentifier,
        event: RecordedEvent,
    ): InterceptRecord =
        InterceptRecord(
            interceptIdentifier = interceptIdentifier,
            sequenceNumber = event.sequenceNumber,
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
            state = InterceptState.Pending,
            host = null,
            method = null,
            path = null,
        )

    // 载荷字段名原文未定义，这里按领域字段的 camelCase 名定，并集中在 reducer 一处。
    private const val HOST_FIELD = "host"
    private const val METHOD_FIELD = "method"
    private const val PATH_FIELD = "path"
}
