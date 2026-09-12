// 请求美化截图。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.ScreenshotIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.ScreenshotIdentifierSerializer

/**
 * 请求美化一张截图；plan 未定义其余载荷，因此只带标识符。
 *
 * @property screenshotIdentifier 要美化的截图。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class BeautifyScreenshot(
    @Serializable(with = ScreenshotIdentifierSerializer::class)
    val screenshotIdentifier: ScreenshotIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
