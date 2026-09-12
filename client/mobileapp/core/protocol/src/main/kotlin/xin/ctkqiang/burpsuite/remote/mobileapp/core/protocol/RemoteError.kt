// 命令执行失败的机器可读描述。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.Serializable

/**
 * 命令执行失败；只带稳定错误码与「能否重试」，不带自由文本，免得把流量内容带进日志。
 *
 * @property code 错误码。
 * @property isRetryable 换个时机重试是否可能有不同结果。
 */
@Serializable
data class RemoteError(val code: RemoteErrorCode, val isRetryable: Boolean)
