package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import io.ktor.http.encodeURLPathPart
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.AddHistoryToScope
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.AnnotateHistory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.BeautifyScreenshot
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.BookmarkHistory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ConnectToBurp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.DropIntercept
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ExecuteRepeater
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ForwardIntercept
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ImportScreenshot
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ModifyIntercept
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.PairDevice
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.RemoteCommand
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.SaveHistory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.SendToRepeater
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ShareHistory

/**
 * 命令到插件端点路径的对照表。
 *
 * 命令类型不出现在报文里（rules.md §11）：路径本身就是分派依据，所以这张表与插件的路由表逐条对齐。
 * 插件注册了路由、只是还没接上执行体时，这里照常发出去，让插件自己回「尚未实现」——
 * 它补上执行体那天，这一层一个字都不用改。
 */
object RemoteCommandEndpoint {
    /** 命令要打的端点路径；插件连路由都还没注册时返回 null，调用方据此直接判为「服务端还没做」。 */
    fun pathOf(command: RemoteCommand): String? =
        when (command) {
            is AddHistoryToScope ->
                RemoteEndpointPath.SCOPE + "/" + command.historyIdentifier.value.encodeURLPathPart()

            is ModifyIntercept ->
                interceptPath(command.interceptIdentifier, RemoteEndpointPath.INTERCEPT_MODIFY_SUFFIX)

            is ForwardIntercept ->
                interceptPath(command.interceptIdentifier, RemoteEndpointPath.INTERCEPT_FORWARD_SUFFIX)

            is DropIntercept ->
                interceptPath(command.interceptIdentifier, RemoteEndpointPath.INTERCEPT_DROP_SUFFIX)

            is SendToRepeater -> RemoteEndpointPath.REPEATER

            is ExecuteRepeater ->
                RemoteEndpointPath.REPEATER + "/" + command.repeaterRequestIdentifier.value.encodeURLPathPart() +
                    RemoteEndpointPath.REPEATER_EXECUTE_SUFFIX

            // 插件没有这些路由：要么本该在客户端闭环，要么对应的插件能力还没排期。
            is AnnotateHistory,
            is BeautifyScreenshot,
            is BookmarkHistory,
            is ConnectToBurp,
            is ImportScreenshot,
            is PairDevice,
            is SaveHistory,
            is ShareHistory,
            -> null
        }

    private fun interceptPath(
        interceptIdentifier: InterceptIdentifier,
        actionSuffix: String,
    ): String = RemoteEndpointPath.INTERCEPTS + "/" + interceptIdentifier.value.encodeURLPathPart() + actionSuffix
}
