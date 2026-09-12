// 截图的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识一张截图；原始图和美化图共用同一个身份，免得客户端自己维护产物之间的对应关系。
 *
 * @property value 文本取值。
 */
@JvmInline
value class ScreenshotIdentifier(val value: String)
