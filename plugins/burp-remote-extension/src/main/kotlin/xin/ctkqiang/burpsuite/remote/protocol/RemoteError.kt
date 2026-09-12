// 命令执行失败的机器可读描述。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 一次执行失败的原因。
 *
 * 只带错误码与「能否重试」，不带自由文本：失败信息一旦可以自由书写，请求头与响应体就会顺着它流进日志（rules.md §12）。
 *
 * @property code 错误码。
 * @property isRetryable 同一个操作标识再发一次是否有意义。
 */
@Serializable
data class RemoteError(
    val code: RemoteErrorCode,
    val isRetryable: Boolean,
)
