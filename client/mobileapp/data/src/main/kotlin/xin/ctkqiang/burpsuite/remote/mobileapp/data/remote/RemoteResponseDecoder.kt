package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemotePayload
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ServerCapabilities

/**
 * 应答载荷 → 领域值。
 *
 * 解析失败一律映射成 [RemoteFailure.MalformedServerResponse]：契约不一致是预期内的结局，
 * 不该让它以异常的形式炸穿到界面层。
 */
object RemoteResponseDecoder {
    /** 解运行态载荷；/v1/status 与 /v1/snapshot 共用它。 */
    fun decodeRuntimeState(payload: JsonElement?): RemoteResult<RemoteRuntimeState> =
        decodePayload(payload, RemoteRuntimeStatePayload.serializer()) { runtimeStatePayload ->
            RemoteRuntimeState(
                protocolVersion = runtimeStatePayload.protocolVersion,
                remotePort = runtimeStatePayload.remotePort,
                connectedDeviceCount = runtimeStatePayload.connectedDeviceCount,
                pairedDeviceCount = runtimeStatePayload.pairedDeviceCount,
                latestEventSequenceNumber = runtimeStatePayload.latestEventSequenceNumber,
            )
        }

    /** 解能力清单载荷。 */
    fun decodeCapabilities(payload: JsonElement?): RemoteResult<ServerCapabilities> =
        decodePayload(payload, ServerCapabilitiesPayload.serializer()) { capabilitiesPayload ->
            ServerCapabilities(names = capabilitiesPayload.capabilities.toSet())
        }

    /** 解配对成功载荷。 */
    fun decodePairingIdentity(payload: JsonElement?): RemoteResult<DeviceIdentifier> =
        decodePayload(payload, RemotePairingResultPayload.serializer()) { pairingResultPayload ->
            DeviceIdentifier(pairingResultPayload.deviceIdentifier)
        }

    /** 原样承载历史类载荷的 JSON 文本；字段集未定下来之前，不替插件编一个形状。 */
    fun decodeHistoryPayload(payload: JsonElement?): RemoteResult<RemotePayload> {
        val presentPayload = payload ?: return RemoteResult.Failed(RemoteFailure.MalformedServerResponse)
        return RemoteResult.Succeeded(
            RemotePayload(RemoteWireJson.instance.encodeToString(JsonElement.serializer(), presentPayload)),
        )
    }

    /**
     * 判定应答里是否带着一个失败的结局。
     *
     * 查询端点的成功应答用 payload 承载数据，因此出现 result 就说明这次调用没有拿到东西。
     */
    fun decodeFailure(envelope: RemoteResponseEnvelope): RemoteFailure? =
        when (val result = envelope.result) {
            null -> null
            is CommandResult.Rejected -> RemoteFailureMapper.fromRejected(result.reason)
            is CommandResult.Failed -> RemoteFailureMapper.fromFailed(result.error)
            is CommandResult.Succeeded -> RemoteFailure.MalformedServerResponse
        }

    /**
     * 判定命令类应答的结局。
     *
     * 与查询类应答相反：命令类应答成功时不带 payload，只回一条 `Succeeded`，因此这里不带载荷。
     */
    fun decodeCommandOutcome(envelope: RemoteResponseEnvelope): RemoteResult<Unit> =
        when (val result = envelope.result) {
            is CommandResult.Succeeded -> RemoteResult.Succeeded(Unit)
            is CommandResult.Rejected -> RemoteResult.Failed(RemoteFailureMapper.fromRejected(result.reason))
            is CommandResult.Failed -> RemoteResult.Failed(RemoteFailureMapper.fromFailed(result.error))
            null -> RemoteResult.Failed(RemoteFailure.MalformedServerResponse)
        }

    private fun <WirePayload, Value> decodePayload(
        payload: JsonElement?,
        serializer: KSerializer<WirePayload>,
        translate: (WirePayload) -> Value,
    ): RemoteResult<Value> {
        val presentPayload = payload ?: return RemoteResult.Failed(RemoteFailure.MalformedServerResponse)
        val decodedPayload =
            try {
                RemoteWireJson.instance.decodeFromJsonElement(serializer, presentPayload)
            } catch (malformedPayload: IllegalArgumentException) {
                return RemoteResult.Failed(RemoteFailure.MalformedServerResponse)
            }
        return RemoteResult.Succeeded(translate(decodedPayload))
    }
}
