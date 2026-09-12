package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.ScreenshotIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ScreenshotProcessingState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ScreenshotRecord

/**
 * 把截图事件折成读模型。
 *
 * 纯函数：无 I/O、无时钟、无随机（rules.md §5.6）。美化只补上处理结果，原始图那一列始终不被动过（plan §29）。
 */
object ScreenshotProjectionReducer : ProjectionReducer<ScreenshotProjectionState> {
    /** 这条事件落在哪张截图上；不属于截图投影的事件返回 null。 */
    fun affectedScreenshotIdentifier(event: RecordedEvent): ScreenshotIdentifier? =
        when (event.eventType) {
            EventType.SCREENSHOT_IMPORTED,
            EventType.SCREENSHOT_BEAUTIFIED,
            -> ScreenshotIdentifier(event.aggregateIdentifier.value)
            else -> null
        }

    override fun reduce(
        state: ScreenshotProjectionState,
        event: RecordedEvent,
    ): ScreenshotProjectionState {
        if (event.eventIdentifier in state.appliedEventIdentifiers) return state

        val stateWithAppliedEvent =
            state.copy(appliedEventIdentifiers = state.appliedEventIdentifiers + event.eventIdentifier)
        val screenshotIdentifier = affectedScreenshotIdentifier(event) ?: return stateWithAppliedEvent
        val existingRecord = stateWithAppliedEvent.recordsByIdentifier[screenshotIdentifier]
        val updatedRecord = reduceRecord(existingRecord, screenshotIdentifier, event) ?: return stateWithAppliedEvent

        return stateWithAppliedEvent.copy(
            recordsByIdentifier =
                stateWithAppliedEvent.recordsByIdentifier + (screenshotIdentifier to updatedRecord),
        )
    }

    private fun reduceRecord(
        existingRecord: ScreenshotRecord?,
        screenshotIdentifier: ScreenshotIdentifier,
        event: RecordedEvent,
    ): ScreenshotRecord? {
        val record = existingRecord ?: emptyScreenshotRecord(screenshotIdentifier, event)
        val attributes = event.payloadAttributes

        return when (event.eventType) {
            // 不在这里写回 Imported：美化事件可能先折进来，把状态倒退回未处理只会让界面闪一下。
            EventType.SCREENSHOT_IMPORTED ->
                record.copy(
                    historyIdentifier =
                        attributes.text(HISTORY_IDENTIFIER_FIELD)?.let(::HistoryIdentifier)
                            ?: record.historyIdentifier,
                    originalUri = attributes.text(ORIGINAL_URI_FIELD) ?: record.originalUri,
                    width = attributes.integer(WIDTH_FIELD) ?: record.width,
                    height = attributes.integer(HEIGHT_FIELD) ?: record.height,
                    format = attributes.text(FORMAT_FIELD) ?: record.format,
                )
            EventType.SCREENSHOT_BEAUTIFIED ->
                record.copy(
                    processedUri = attributes.text(PROCESSED_URI_FIELD) ?: record.processedUri,
                    processedAt = event.occurredAt,
                    processingVersion = attributes.integer(PROCESSING_VERSION_FIELD) ?: record.processingVersion,
                    processingState = ScreenshotProcessingState.Ready,
                )
            else -> existingRecord
        }
    }

    private fun emptyScreenshotRecord(
        screenshotIdentifier: ScreenshotIdentifier,
        event: RecordedEvent,
    ): ScreenshotRecord =
        ScreenshotRecord(
            screenshotIdentifier = screenshotIdentifier,
            createdAt = event.occurredAt,
            historyIdentifier = null,
            originalUri = null,
            processedUri = null,
            width = null,
            height = null,
            format = null,
            processedAt = null,
            processingVersion = null,
            processingState = ScreenshotProcessingState.Imported,
        )

    // 载荷字段名原文未定义，这里按领域字段的 camelCase 名定，并集中在 reducer 一处。
    private const val HISTORY_IDENTIFIER_FIELD = "historyIdentifier"
    private const val ORIGINAL_URI_FIELD = "originalUri"
    private const val PROCESSED_URI_FIELD = "processedUri"
    private const val WIDTH_FIELD = "width"
    private const val HEIGHT_FIELD = "height"
    private const val FORMAT_FIELD = "format"
    private const val PROCESSING_VERSION_FIELD = "processingVersion"
}
