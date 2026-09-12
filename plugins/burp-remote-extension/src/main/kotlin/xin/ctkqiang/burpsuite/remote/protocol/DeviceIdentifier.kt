/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明「一台已配对设备」的身份类型。远程控制端按定义就是危险的——它控制 Burp，
 * 因此每一个请求都必须能追溯到具体是哪一台设备发出的。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识一台与插件完成配对的设备。
 *
 * 该值是授权判断与审计追溯的主键：插件按它判断这台设备是否有权执行某条命令，
 * 审计日志也按它记录「谁在什么时刻做了什么」。若请求缺少该身份，必须直接拒绝，
 * 而不是退化为匿名访问——局域网并不等于可信网络。
 *
 * 取值在配对成功时由插件分配，并在该设备被解除配对前保持不变。解除配对后该值
 * 立即失效，任何携带旧值的请求都必须被拒绝。
 *
 * 序列化时按底层文本透明写出。
 *
 * @property value 已配对设备的唯一文本标识符，协议示例约定使用 `device_` 前缀。
 */
@JvmInline
@Serializable
value class DeviceIdentifier(val value: String)
