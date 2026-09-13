// HTTP 请求在界面上的语气判定。

package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical

import androidx.compose.ui.graphics.Color
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme

/**
 * 方法名到语气。
 *
 * 读类方法不改服务端状态，保持在线色；写类方法会改状态，标成警告；删除不可逆，标成危险。
 * 一屏扫下来，会改状态的那几条先跳出来。
 *
 * @param method HTTP 方法名；大小写不敏感，`null` 与未知方法同档。
 * @return 该方法在界面上的语气。
 */
fun burpRemoteMethodTone(method: String?): BurpRemoteStatusTone =
    when (method?.uppercase()) {
        HTTP_METHOD_GET, HTTP_METHOD_HEAD, HTTP_METHOD_OPTIONS -> BurpRemoteStatusTone.Live
        HTTP_METHOD_POST, HTTP_METHOD_PUT, HTTP_METHOD_PATCH -> BurpRemoteStatusTone.Warning
        HTTP_METHOD_DELETE -> BurpRemoteStatusTone.Danger
        else -> BurpRemoteStatusTone.Neutral
    }

/**
 * 状态码到语气。
 *
 * 分档只看百位：1xx 与 2xx 都算正常完成，3xx 是一次转向（本身不含结论），4xx 是客户端错，
 * 5xx 是服务端错。这样插件将来多返回几个码，界面不必跟着改。
 *
 * @param statusCode HTTP 状态码；请求尚未回来时给 `null`，与未知码同档。
 * @return 该状态码在界面上的语气。
 */
fun burpRemoteStatusTone(statusCode: Int?): BurpRemoteStatusTone =
    when (statusCode?.div(HTTP_STATUS_CLASS_DIVISOR)) {
        HTTP_STATUS_CLASS_INFORMATIONAL, HTTP_STATUS_CLASS_SUCCESS -> BurpRemoteStatusTone.Live
        HTTP_STATUS_CLASS_REDIRECTION -> BurpRemoteStatusTone.Neutral
        HTTP_STATUS_CLASS_CLIENT_ERROR -> BurpRemoteStatusTone.Warning
        HTTP_STATUS_CLASS_SERVER_ERROR -> BurpRemoteStatusTone.Danger
        else -> BurpRemoteStatusTone.Neutral
    }

/**
 * 语气到语义色。
 *
 * 这是整个界面里唯一一处把语气翻成颜色的地方：各屏只说自己是哪种语气，色号一律由主题给。
 *
 * @param colourScheme 当前主题的配色。
 * @return 该语气对应的颜色。
 */
fun BurpRemoteStatusTone.colourIn(colourScheme: BurpRemoteColourScheme): Color =
    when (this) {
        BurpRemoteStatusTone.Neutral -> colourScheme.contentSecondary
        BurpRemoteStatusTone.Live -> colourScheme.success
        BurpRemoteStatusTone.Warning -> colourScheme.warning
        BurpRemoteStatusTone.Danger -> colourScheme.danger
    }

/**
 * 请求目标的展示文本。
 *
 * 主机与路径各自可能缺失——插件只采到一半时也会推事件——因此能拼多少拼多少，
 * 一个都拼不出来时才退到占位符。
 *
 * @param host 目标主机；未知给 `null`。
 * @param path 目标路径；未知给 `null`。
 * @param absentValue 两者都未知时的占位文本。
 * @return 拼好的展示文本。
 */
fun burpRemoteTargetText(
    host: String?,
    path: String?,
    absentValue: String,
): String =
    when {
        host != null && path != null -> host + path
        host != null -> host
        path != null -> path
        else -> absentValue
    }

private const val HTTP_METHOD_GET = "GET"
private const val HTTP_METHOD_HEAD = "HEAD"
private const val HTTP_METHOD_OPTIONS = "OPTIONS"
private const val HTTP_METHOD_POST = "POST"
private const val HTTP_METHOD_PUT = "PUT"
private const val HTTP_METHOD_PATCH = "PATCH"
private const val HTTP_METHOD_DELETE = "DELETE"

private const val HTTP_STATUS_CLASS_DIVISOR = 100
private const val HTTP_STATUS_CLASS_INFORMATIONAL = 1
private const val HTTP_STATUS_CLASS_SUCCESS = 2
private const val HTTP_STATUS_CLASS_REDIRECTION = 3
private const val HTTP_STATUS_CLASS_CLIENT_ERROR = 4
private const val HTTP_STATUS_CLASS_SERVER_ERROR = 5
