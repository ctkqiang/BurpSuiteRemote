package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord

/**
 * 把 HTTP 历史事件折成读模型。
 *
 * 纯函数：无 I/O、无时钟、无随机（rules.md §5.6）。事件没报的字段不覆盖已有的值——
 * 「这次没说」不等于「现在是空的」。
 */
object HistoryProjectionReducer : ProjectionReducer<HistoryProjectionState> {
    /**
     * 这条事件落在哪条历史记录上；不属于历史投影的事件返回 null。
     *
     * 适配层靠它决定要读出哪一行再折，免得把「事件属于哪个投影」的规则抄成两份。
     */
    fun affectedHistoryIdentifier(event: RecordedEvent): HistoryIdentifier? =
        when (event.eventType) {
            EventType.HISTORY_ITEM_OBSERVED,
            EventType.HISTORY_ITEM_SAVED,
            EventType.HISTORY_ANNOTATION_ADDED,
            -> HistoryIdentifier(event.aggregateIdentifier.value)
            else -> null
        }

    override fun reduce(
        state: HistoryProjectionState,
        event: RecordedEvent,
    ): HistoryProjectionState {
        if (event.eventIdentifier in state.appliedEventIdentifiers) return state

        val stateWithAppliedEvent =
            state.copy(appliedEventIdentifiers = state.appliedEventIdentifiers + event.eventIdentifier)
        val historyIdentifier = affectedHistoryIdentifier(event) ?: return stateWithAppliedEvent
        val existingRecord = stateWithAppliedEvent.recordsByIdentifier[historyIdentifier]
        val updatedRecord = reduceRecord(existingRecord, historyIdentifier, event) ?: return stateWithAppliedEvent

        return stateWithAppliedEvent.copy(
            recordsByIdentifier = stateWithAppliedEvent.recordsByIdentifier + (historyIdentifier to updatedRecord),
        )
    }

    private fun reduceRecord(
        existingRecord: HistoryRecord?,
        historyIdentifier: HistoryIdentifier,
        event: RecordedEvent,
    ): HistoryRecord? =
        when (event.eventType) {
            EventType.HISTORY_ITEM_OBSERVED -> observe(existingRecord, historyIdentifier, event)
            EventType.HISTORY_ITEM_SAVED -> archive(existingRecord, historyIdentifier, event)
            EventType.HISTORY_ANNOTATION_ADDED -> annotate(existingRecord, historyIdentifier, event)
            else -> existingRecord
        }

    private fun observe(
        existingRecord: HistoryRecord?,
        historyIdentifier: HistoryIdentifier,
        event: RecordedEvent,
    ): HistoryRecord {
        val attributes = event.payloadAttributes
        val record = existingRecord ?: emptyHistoryRecord(historyIdentifier, event)

        return record.copy(
            sequenceNumber = event.sequenceNumber,
            host = attributes.text(HOST_FIELD) ?: record.host,
            method = attributes.text(METHOD_FIELD) ?: record.method,
            scheme = attributes.text(SCHEME_FIELD) ?: record.scheme,
            path = attributes.text(PATH_FIELD) ?: record.path,
            statusCode = attributes.integer(STATUS_CODE_FIELD) ?: record.statusCode,
            mimeType = attributes.text(MIME_TYPE_FIELD) ?: record.mimeType,
            responseLength = attributes.long(RESPONSE_LENGTH_FIELD) ?: record.responseLength,
            usesTls = attributes.boolean(USES_TLS_FIELD) ?: record.usesTls,
            destinationInternetProtocolAddress =
                attributes.text(DESTINATION_INTERNET_PROTOCOL_ADDRESS_FIELD)
                    ?: record.destinationInternetProtocolAddress,
            listenerPort = attributes.integer(LISTENER_PORT_FIELD) ?: record.listenerPort,
            durationMilliseconds = attributes.long(DURATION_MILLISECONDS_FIELD) ?: record.durationMilliseconds,
            isEdited = attributes.boolean(IS_EDITED_FIELD) ?: record.isEdited,
            title = attributes.text(TITLE_FIELD) ?: record.title,
        )
    }

    // 归档事件也可能先于观察事件到货（掉线期间 Burp 侧继续在跑），因此这里允许先建一条只带归档状态的记录，
    // 丢掉这个事实比留下一条不完整的记录更糟。
    private fun archive(
        existingRecord: HistoryRecord?,
        historyIdentifier: HistoryIdentifier,
        event: RecordedEvent,
    ): HistoryRecord {
        val record = existingRecord ?: emptyHistoryRecord(historyIdentifier, event)

        return record.copy(
            sequenceNumber = event.sequenceNumber,
            archiveState = HistoryArchiveState.Archived,
            savedAt = event.occurredAt,
        )
    }

    private fun annotate(
        existingRecord: HistoryRecord?,
        historyIdentifier: HistoryIdentifier,
        event: RecordedEvent,
    ): HistoryRecord {
        val record = existingRecord ?: emptyHistoryRecord(historyIdentifier, event)

        return record.copy(
            sequenceNumber = event.sequenceNumber,
            annotationCount = record.annotationCount + 1,
            lastAnnotatedAt = event.occurredAt,
        )
    }

    private fun emptyHistoryRecord(
        historyIdentifier: HistoryIdentifier,
        event: RecordedEvent,
    ): HistoryRecord =
        HistoryRecord(
            historyIdentifier = historyIdentifier,
            sequenceNumber = event.sequenceNumber,
            occurredAt = event.occurredAt,
            host = null,
            method = null,
            scheme = null,
            path = null,
            statusCode = null,
            mimeType = null,
            responseLength = null,
            usesTls = null,
            destinationInternetProtocolAddress = null,
            listenerPort = null,
            durationMilliseconds = null,
            isEdited = false,
            title = null,
            archiveState = HistoryArchiveState.Live,
            annotationCount = 0,
            lastAnnotatedAt = null,
            savedAt = null,
        )

    // 载荷字段名原文未定义，这里按领域字段的 camelCase 名定，并集中在 reducer 一处，免得散落两套写法。
    private const val HOST_FIELD = "host"
    private const val METHOD_FIELD = "method"
    private const val SCHEME_FIELD = "scheme"
    private const val PATH_FIELD = "path"
    private const val STATUS_CODE_FIELD = "statusCode"
    private const val MIME_TYPE_FIELD = "mimeType"
    private const val RESPONSE_LENGTH_FIELD = "responseLength"
    private const val USES_TLS_FIELD = "usesTls"
    private const val DESTINATION_INTERNET_PROTOCOL_ADDRESS_FIELD = "destinationInternetProtocolAddress"
    private const val LISTENER_PORT_FIELD = "listenerPort"
    private const val DURATION_MILLISECONDS_FIELD = "durationMilliseconds"
    private const val IS_EDITED_FIELD = "isEdited"
    private const val TITLE_FIELD = "title"
}
