package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.RemoteCommand
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.PairingAttempt
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteHistoryMessage
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemotePayload
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteTimeouts
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ServerCapabilities
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * REST 客户端：把插件端点翻译成领域调用。
 *
 * 只承载插件真实提供的东西：查询端点原样取回载荷，命令端点带上操作标识并把插件结局翻译成领域结果；
 * 插件回「尚未实现」时如实映射成 [RemoteFailure.ActionNotSupported]，界面才知道该说「服务端还没做」。
 */
class RemoteRestClient(
    private val httpClient: HttpClient,
    private val deviceIdentifierProvider: RemoteDeviceIdentifierProvider,
    private val timeouts: RemoteTimeouts,
    private val operationIdentifierGenerator: OperationIdentifierGenerator = OperationIdentifierGenerator.random(),
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : RemoteSnapshotSource {
    /** 配对：把票据里的一次性配对码换成设备身份，对应 POST /v1/pair。 */
    suspend fun pair(pairingAttempt: PairingAttempt): RemoteResult<DeviceIdentifier> {
        val requestBody =
            RemoteWireJson.instance.encodeToString(
                RemoteClientMessage.serializer(),
                RemoteClientMessage(
                    protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                    messageType = RemoteClientMessageType.Pair,
                    challengeIdentifier = pairingAttempt.challengeIdentifier,
                    pairingCode = pairingAttempt.pairingCode,
                ),
            )
        return when (
            val payloadResult =
                requestPayload(
                    host = pairingAttempt.host,
                    port = pairingAttempt.port,
                    isTlsEnabled = pairingAttempt.isTlsEnabled,
                    httpMethod = HttpMethod.Post,
                    path = RemoteEndpointPath.PAIR,
                    requestBody = requestBody,
                )
        ) {
            is RemoteResult.Failed -> payloadResult
            is RemoteResult.Succeeded -> RemoteResponseDecoder.decodePairingIdentity(payloadResult.value)
        }
    }

    /** 读取插件运行态，对应 GET /v1/status。 */
    suspend fun readRuntimeState(configuration: RemoteConnectionConfiguration): RemoteResult<RemoteRuntimeState> =
        readQuery(configuration, RemoteEndpointPath.STATUS, RemoteResponseDecoder::decodeRuntimeState)

    /** 读取插件自报能力，对应 GET /v1/capabilities。 */
    suspend fun readCapabilities(configuration: RemoteConnectionConfiguration): RemoteResult<ServerCapabilities> =
        readQuery(configuration, RemoteEndpointPath.CAPABILITIES, RemoteResponseDecoder::decodeCapabilities)

    /** 取当前状态快照，对应 GET /v1/snapshot。 */
    override suspend fun requestSnapshot(
        configuration: RemoteConnectionConfiguration,
    ): RemoteResult<RemoteRuntimeState> =
        readQuery(configuration, RemoteEndpointPath.SNAPSHOT, RemoteResponseDecoder::decodeRuntimeState)

    /** 读取远端历史列表原文，对应 GET /v1/history。 */
    suspend fun readRemoteHistory(configuration: RemoteConnectionConfiguration): RemoteResult<RemotePayload> =
        readQuery(configuration, RemoteEndpointPath.HISTORY, RemoteResponseDecoder::decodeHistoryPayload)

    /** 按标识取回单条历史报文的完整内容，对应 GET /v1/history/{historyIdentifier}。 */
    suspend fun readRemoteHistoryMessage(
        configuration: RemoteConnectionConfiguration,
        historyIdentifier: HistoryIdentifier,
    ): RemoteResult<RemoteHistoryMessage> =
        readQuery(
            configuration,
            RemoteEndpointPath.HISTORY + "/" + historyIdentifier.value.encodeURLPathPart(),
            RemoteResponseDecoder::decodeHistoryMessage,
        )

    /**
     * 取一个新的执行身份。
     *
     * 发令方必须在构造命令对象之前拿到它，所以这里不是挂起函数；取到之后原样放进命令，
     * 重试复用同一个值，插件据此去重（rules.md §5.5）。
     */
    fun nextOperationIdentifier(): OperationIdentifier = operationIdentifierGenerator.generate()

    /**
     * 执行一条控制命令。
     *
     * 命令自带的执行身份原样上行，因此重试不会产生第二次副作用；插件没有这条路由时不去打扰它，
     * 也不编一个成功，直接如实回 [RemoteFailure.ActionNotSupported]。
     */
    suspend fun dispatch(
        configuration: RemoteConnectionConfiguration,
        command: RemoteCommand,
    ): RemoteResult<Unit> {
        val path =
            RemoteCommandEndpoint.pathOf(command)
                ?: return RemoteResult.Failed(RemoteFailure.ActionNotSupported)
        return executeControlCommand(configuration, path, command.operationIdentifier)
    }

    private suspend fun <Value> readQuery(
        configuration: RemoteConnectionConfiguration,
        path: String,
        decode: (JsonElement) -> RemoteResult<Value>,
    ): RemoteResult<Value> =
        when (
            val payloadResult =
                requestPayload(
                    host = configuration.host,
                    port = configuration.port,
                    isTlsEnabled = configuration.isTlsEnabled,
                    httpMethod = HttpMethod.Get,
                    path = path,
                )
        ) {
            is RemoteResult.Failed -> payloadResult
            is RemoteResult.Succeeded -> decode(payloadResult.value)
        }

    // 一次调用的完整链路：HTTP → 状态码 → 应答信封 → 版本校验 → 载荷。
    private suspend fun requestPayload(
        host: String,
        port: Int,
        isTlsEnabled: Boolean,
        httpMethod: HttpMethod,
        path: String,
        requestBody: String? = null,
    ): RemoteResult<JsonElement> {
        val envelope =
            when (
                val envelopeResult =
                    requestEnvelope(
                        host = host,
                        port = port,
                        isTlsEnabled = isTlsEnabled,
                        httpMethod = httpMethod,
                        path = path,
                        operationIdentifier = null,
                        requestBody = requestBody,
                    )
            ) {
                is RemoteResult.Failed -> return envelopeResult
                is RemoteResult.Succeeded -> envelopeResult.value
            }
        RemoteResponseDecoder.decodeFailure(envelope)?.let { failure -> return RemoteResult.Failed(failure) }
        val payload = envelope.payload
        return if (payload == null) {
            RemoteResult.Failed(RemoteFailure.MalformedServerResponse)
        } else {
            RemoteResult.Succeeded(payload)
        }
    }

    // 命令类端点只多两样东西：操作标识请求头，以及「成功不带载荷」的结局判定。
    private suspend fun executeControlCommand(
        configuration: RemoteConnectionConfiguration,
        path: String,
        operationIdentifier: OperationIdentifier,
    ): RemoteResult<Unit> =
        when (
            val envelopeResult =
                requestEnvelope(
                    host = configuration.host,
                    port = configuration.port,
                    isTlsEnabled = configuration.isTlsEnabled,
                    httpMethod = HttpMethod.Post,
                    path = path,
                    operationIdentifier = operationIdentifier,
                )
        ) {
            is RemoteResult.Failed -> envelopeResult
            is RemoteResult.Succeeded -> RemoteResponseDecoder.decodeCommandOutcome(envelopeResult.value)
        }

    private suspend fun requestEnvelope(
        host: String,
        port: Int,
        isTlsEnabled: Boolean,
        httpMethod: HttpMethod,
        path: String,
        operationIdentifier: OperationIdentifier?,
        requestBody: String? = null,
    ): RemoteResult<RemoteResponseEnvelope> {
        // 只记方法与路径：请求头里有身份、请求体里有用户数据，两者都不进日志（rules.md §12）。
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.Transport,
                message = "发出请求",
                attributes = mapOf("method" to httpMethod.value, "path" to path),
            ),
        )
        val bodyText =
            when (
                val bodyResult =
                    sendRequest(restUrl(host, port, isTlsEnabled, path), httpMethod, operationIdentifier, requestBody)
            ) {
                is RemoteResult.Failed -> return bodyResult
                is RemoteResult.Succeeded -> bodyResult.value
            }
        val envelope =
            try {
                RemoteWireJson.instance.decodeFromString(RemoteResponseEnvelope.serializer(), bodyText)
            } catch (malformedResponse: IllegalArgumentException) {
                return RemoteResult.Failed(RemoteFailure.MalformedServerResponse)
            }
        if (envelope.protocolVersion != RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION) {
            return RemoteResult.Failed(RemoteFailure.ProtocolVersionUnsupported)
        }
        return RemoteResult.Succeeded(envelope)
    }

    private suspend fun sendRequest(
        url: String,
        httpMethod: HttpMethod,
        operationIdentifier: OperationIdentifier?,
        requestBody: String?,
    ): RemoteResult<String> =
        try {
            withTimeout(timeouts.requestMilliseconds) {
                val deviceIdentifier = deviceIdentifierProvider.currentDeviceIdentifier()
                val response =
                    httpClient.request(url) {
                        method = httpMethod
                        deviceIdentifier?.let { identifier -> header(DEVICE_IDENTIFIER_HEADER, identifier.value) }
                        operationIdentifier?.let { identifier ->
                            header(OPERATION_IDENTIFIER_HEADER, identifier.value)
                        }
                        if (requestBody != null) {
                            contentType(ContentType.Application.Json)
                            setBody(requestBody)
                        }
                    }
                val bodyText = response.bodyAsText()
                technicalLog.record(
                    TechnicalLogEvent(
                        category = TechnicalLogCategory.Transport,
                        message = "收到应答",
                        attributes = mapOf("status" to response.status.value.toString()),
                    ),
                )
                if (response.status.isSuccess()) {
                    RemoteResult.Succeeded(bodyText)
                } else {
                    RemoteResult.Failed(RemoteFailureMapper.fromHttpStatusCode(response.status.value))
                }
            }
        } catch (requestTimeout: TimeoutCancellationException) {
            transportFailureOf(cause = requestTimeout, failure = RemoteFailure.TimedOut)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (unreachableServer: ConnectException) {
            transportFailureOf(cause = unreachableServer, failure = RemoteFailure.ServerUnavailable)
        } catch (unresolvableHost: UnknownHostException) {
            transportFailureOf(cause = unresolvableHost, failure = RemoteFailure.ServerUnavailable)
        } catch (transportFailure: IOException) {
            transportFailureOf(cause = transportFailure, failure = RemoteFailure.TransportFailure)
        } catch (unexpectedFailure: Exception) {
            transportFailureOf(cause = unexpectedFailure, failure = RemoteFailure.TransportFailure)
        }

    // 传输级失败必须留下异常类型与摘要：这是排查「连不上」唯一的一手材料，报文与身份仍然不进日志。
    private fun transportFailureOf(
        cause: Exception,
        failure: RemoteFailure,
    ): RemoteResult.Failed {
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.Failure,
                message = "传输级失败",
                attributes = mapOf("failure" to failure.toString()),
                failure = cause,
            ),
        )
        return RemoteResult.Failed(failure)
    }

    private fun restUrl(
        host: String,
        port: Int,
        isTlsEnabled: Boolean,
        path: String,
    ): String {
        val scheme = if (isTlsEnabled) HTTPS_SCHEME else HTTP_SCHEME
        return "$scheme://$host:$port$path"
    }

    private companion object {
        // 身份走请求头；操作标识只有控制命令用得到，查询端点不带它。
        const val DEVICE_IDENTIFIER_HEADER = "X-Burp-Remote-Device-Identifier"

        const val OPERATION_IDENTIFIER_HEADER = "X-Burp-Remote-Operation-Identifier"

        // 插件侧目前只提供明文传输；打开 TLS 只是改用加密方案，不代表链路已经加密。
        const val HTTP_SCHEME = "http"
        const val HTTPS_SCHEME = "https"
    }
}
