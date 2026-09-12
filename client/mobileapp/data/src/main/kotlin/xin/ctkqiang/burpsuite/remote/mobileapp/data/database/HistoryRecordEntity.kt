package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import java.time.Instant

/**
 * `history` 的一行：只放元数据，报文本体不在这里（plan §19）。
 *
 * 列名照领域读模型的字段名转成下划线形式；plan §19 里那几列没有人消费，就不建，
 * 免得留下一批永远为空的列（rules.md §3.2 要求列名写全词、规则见 §7.1 一表一读模型）。
 */
@Entity(tableName = "history")
data class HistoryRecordEntity(
    /** 历史记录身份，主键。 */
    @PrimaryKey
    @ColumnInfo(name = "history_identifier")
    val historyIdentifier: String,
    /** 产生这条记录的事件序号；未知时为 null。 */
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Long?,
    /** 事件发生时刻，毫秒整数。 */
    @ColumnInfo(name = "timestamp")
    val occurredAtMilliseconds: Long,
    /** 目标主机。 */
    @ColumnInfo(name = "host")
    val host: String?,
    /** HTTP 方法。 */
    @ColumnInfo(name = "method")
    val method: String?,
    /** 协议方案。 */
    @ColumnInfo(name = "scheme")
    val scheme: String?,
    /** 请求路径。 */
    @ColumnInfo(name = "path")
    val path: String?,
    /** 响应状态码。 */
    @ColumnInfo(name = "status_code")
    val statusCode: Int?,
    /** 响应 MIME 类型。 */
    @ColumnInfo(name = "mime_type")
    val mimeType: String?,
    /** 响应字节数。 */
    @ColumnInfo(name = "response_length")
    val responseLength: Long?,
    /** 是否经 TLS 传输。 */
    @ColumnInfo(name = "uses_tls")
    val usesTls: Boolean?,
    /** 目标地址。 */
    @ColumnInfo(name = "destination_internet_protocol_address")
    val destinationInternetProtocolAddress: String?,
    /** 监听端口。 */
    @ColumnInfo(name = "listener_port")
    val listenerPort: Int?,
    /** 请求耗时，毫秒整数。 */
    @ColumnInfo(name = "duration_milliseconds")
    val durationMilliseconds: Long?,
    /** 用户是否改写过这条记录。 */
    @ColumnInfo(name = "is_edited")
    val isEdited: Boolean,
    /** 用户给这条记录起的标题。 */
    @ColumnInfo(name = "title")
    val title: String?,
    /** 归档状态，按枚举名存放。 */
    @ColumnInfo(name = "archive_state")
    val archiveState: String,
    /** 已收到的注解条数。 */
    @ColumnInfo(name = "annotation_count")
    val annotationCount: Int,
    /** 最后一条注解的时刻，毫秒整数。 */
    @ColumnInfo(name = "last_annotated_at")
    val lastAnnotatedAtMilliseconds: Long?,
    /** 归档时刻，毫秒整数。 */
    @ColumnInfo(name = "saved_at")
    val savedAtMilliseconds: Long?,
)

/** 实体还原成领域读模型。 */
internal fun HistoryRecordEntity.toDomain(): HistoryRecord =
    HistoryRecord(
        historyIdentifier = HistoryIdentifier(historyIdentifier),
        sequenceNumber = sequenceNumber,
        occurredAt = Instant.ofEpochMilli(occurredAtMilliseconds),
        host = host,
        method = method,
        scheme = scheme,
        path = path,
        statusCode = statusCode,
        mimeType = mimeType,
        responseLength = responseLength,
        usesTls = usesTls,
        destinationInternetProtocolAddress = destinationInternetProtocolAddress,
        listenerPort = listenerPort,
        durationMilliseconds = durationMilliseconds,
        isEdited = isEdited,
        title = title,
        archiveState = historyArchiveStateFromStorageValue(archiveState),
        annotationCount = annotationCount,
        lastAnnotatedAt = lastAnnotatedAtMilliseconds?.let { milliseconds -> Instant.ofEpochMilli(milliseconds) },
        savedAt = savedAtMilliseconds?.let { milliseconds -> Instant.ofEpochMilli(milliseconds) },
    )

/** 领域读模型落成实体。 */
internal fun HistoryRecord.toEntity(): HistoryRecordEntity =
    HistoryRecordEntity(
        historyIdentifier = historyIdentifier.value,
        sequenceNumber = sequenceNumber,
        occurredAtMilliseconds = occurredAt.toEpochMilli(),
        host = host,
        method = method,
        scheme = scheme,
        path = path,
        statusCode = statusCode,
        mimeType = mimeType,
        responseLength = responseLength,
        usesTls = usesTls,
        destinationInternetProtocolAddress = destinationInternetProtocolAddress,
        listenerPort = listenerPort,
        durationMilliseconds = durationMilliseconds,
        isEdited = isEdited,
        title = title,
        archiveState = archiveState.name,
        annotationCount = annotationCount,
        lastAnnotatedAtMilliseconds = lastAnnotatedAt?.toEpochMilli(),
        savedAtMilliseconds = savedAt?.toEpochMilli(),
    )

// 认不出的取值按 Live 处理：这一列由本应用写入，读到陌生值说明数据被改过，
// 此时宁可把它当实时记录，也不要让整个列表读不出来。
private fun historyArchiveStateFromStorageValue(storageValue: String): HistoryArchiveState =
    HistoryArchiveState.entries.firstOrNull { archiveState -> archiveState.name == storageValue }
        ?: HistoryArchiveState.Live
