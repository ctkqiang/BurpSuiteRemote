// 已配对设备的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识一台已完成配对的设备，是授权判断与审计追溯的主键。
 *
 * @property value 文本取值，示例约定 device_ 前缀。
 */
@JvmInline
value class DeviceIdentifier(val value: String)
