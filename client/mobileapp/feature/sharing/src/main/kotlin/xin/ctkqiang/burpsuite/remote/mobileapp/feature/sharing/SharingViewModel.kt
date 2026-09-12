package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

/**
 * 分享与导出的状态持有者（plan §75）。
 *
 * 导出包只由本机已有的东西组装：归档本体既不参与扫描也不参与打包，因此这一步动不了原始归档
 * （rules.md §12）。写盘失败如实上报，不谎报成功。
 */
class SharingViewModel(
    private val historyIdentifier: String,
    historyRepository: HistoryRepository,
    private val exportDestinationWriter: ExportDestinationWriter,
    /** 写进导出包的说明文字；由装配层从资源里取，ViewModel 不碰资源。 */
    private val exportNoticeText: String,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
    private val timeProvider: TimeProvider = TimeProvider { Instant.now() },
) : ViewModel() {
    private val effectChannel = Channel<SharingUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；文件选择器这类动作走这里，不塞进状态。 */
    val effects: Flow<SharingUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val exportOutcome = MutableStateFlow(ExportOutcome.NotAttempted)

    private val observedRecord: StateFlow<HistoryRecord?> =
        historyRepository
            .observeHistoryRecord(HistoryIdentifier(value = historyIdentifier))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = null,
            )

    val uiState: StateFlow<SharingUserInterfaceState> =
        combine(observedRecord, exportOutcome) { record, outcome -> stateOf(record = record, outcome = outcome) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = SharingUserInterfaceState(),
            )

    fun handleIntent(intent: SharingUserInterfaceIntent) {
        when (intent) {
            is SharingUserInterfaceIntent.ConfirmExport -> {
                record(category = TechnicalLogCategory.UserInterface, message = "用户确认导出，打开系统文件选择器")
                effectChannel.trySend(SharingUserInterfaceEffect.RequestExportDestination(suggestedFileName()))
            }

            is SharingUserInterfaceIntent.WriteExport -> writeExport(intent.destinationUri)
        }
    }

    private fun writeExport(destinationUri: String) {
        val record = observedRecord.value ?: return
        viewModelScope.launch {
            val exportedAt = timeProvider.now()
            val bundleBytes =
                ExportBundleWriter.build(
                    manifest = manifestOf(record = record, exportedAt = exportedAt),
                    redactionReport = redactionReportOf(record = record, exportedAt = exportedAt),
                    noticeText = exportNoticeText,
                )
            val outcome =
                runCatching {
                    exportDestinationWriter.write(
                        destinationUri = Uri.parse(destinationUri),
                        bundleBytes = bundleBytes,
                    )
                }
            val exportOutcomeValue = if (outcome.isSuccess) ExportOutcome.Succeeded else ExportOutcome.Failed
            val logCategory =
                if (outcome.isSuccess) TechnicalLogCategory.UserInterface else TechnicalLogCategory.Failure
            record(
                category = logCategory,
                message = if (outcome.isSuccess) "导出包已写出" else "导出包写出失败",
                attributes =
                    mapOf(
                        "bundleByteCount" to bundleBytes.size.toString(),
                        // 失败原因只记异常类型：导出内容里可能有凭据（rules.md §12）。
                        "failure" to outcome.exceptionOrNull()?.javaClass?.simpleName.orEmpty(),
                    ),
            )
            exportOutcome.update { exportOutcomeValue }
        }
    }

    private fun stateOf(
        record: HistoryRecord?,
        outcome: ExportOutcome,
    ): SharingUserInterfaceState =
        SharingUserInterfaceState(
            record = record,
            hasLoaded = true,
            redaction = record?.let { existingRecord -> summarizeRedaction(existingRecord) },
            hasExported = outcome == ExportOutcome.Succeeded,
            hasExportFailed = outcome == ExportOutcome.Failed,
        )

    private fun summarizeRedaction(record: HistoryRecord): RedactionSummary {
        val exportableLines = exportableLinesOf(record)
        return RedactionSummary(
            scannedLines = exportableLines,
            findings = SensitiveDataRedactor.scan(exportableLines),
            redactedLines = SensitiveDataRedactor.redact(exportableLines),
        )
    }

    // 只导出本机真有的字段；报文本体还没有被捕获，因此不在这一列里（rules.md §5.1）。
    private fun exportableLinesOf(record: HistoryRecord): List<String> =
        listOfNotNull(
            record.method?.let { method -> "method=" + method },
            record.host?.let { host -> "host=" + host },
            record.path?.let { path -> "path=" + path },
            record.statusCode?.let { statusCode -> "statusCode=" + statusCode },
            record.title?.let { title -> "title=" + title },
        )

    private fun manifestOf(
        record: HistoryRecord,
        exportedAt: Instant,
    ): ExportManifest =
        ExportManifest(
            format = ExportManifest.FORMAT_NAME,
            exportFormatVersion = ExportManifest.CURRENT_EXPORT_FORMAT_VERSION,
            createdAt = exportedAt.toString(),
            items =
                listOf(
                    ExportManifestEntry(
                        historyIdentifier = record.historyIdentifier.value,
                        method = record.method,
                        host = record.host,
                        path = record.path,
                    ),
                ),
        )

    private fun redactionReportOf(
        record: HistoryRecord,
        exportedAt: Instant,
    ): ExportRedactionReport {
        val summary = summarizeRedaction(record)
        return ExportRedactionReport(
            exportedAt = exportedAt.toString(),
            scannedLineCount = summary.scannedLines.size,
            findings =
                summary.findings.map { finding ->
                    ExportRedactionReport.ExportRedactionFinding(
                        kind = finding.kind.name,
                        lineNumber = finding.lineNumber,
                    )
                },
        )
    }

    private fun suggestedFileName(): String = EXPORT_FILE_NAME_PREFIX + historyIdentifier + EXPORT_FILE_NAME_SUFFIX

    private fun record(
        category: TechnicalLogCategory,
        message: String,
        attributes: Map<String, String> = emptyMap(),
    ) {
        technicalLog.record(
            TechnicalLogEvent(category = category, message = message, attributes = attributes),
        )
    }

    private enum class ExportOutcome {
        NotAttempted,
        Succeeded,
        Failed,
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        const val EXPORT_FILE_NAME_PREFIX = "burpremote-"
        const val EXPORT_FILE_NAME_SUFFIX = ".burpremote"
    }
}
