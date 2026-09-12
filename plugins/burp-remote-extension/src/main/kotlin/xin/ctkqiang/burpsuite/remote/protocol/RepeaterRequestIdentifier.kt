/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明「一次 Repeater 请求」的身份类型。移动端先创建请求、再显式执行，两步之间
 * 必须有一个稳定句柄把它们连起来。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识一次被送往 Repeater 的请求。
 *
 * 创建与执行是两个独立动作：创建接口负责把请求写入 Repeater 并返回该标识符，
 * 执行接口随后按该标识符触发发送。因此该值必须在创建成功后立即可用，并在执行
 * 完成前保持稳定，否则客户端会拿到一个无法回指的句柄。
 *
 * 取值由插件在请求创建时分配。同一次创建与执行必须使用同一个值；重复执行同一
 * 标识符属于重试，由命令的操作标识符负责幂等，而不是由本标识符承担。
 *
 * 序列化时按底层文本透明写出。
 *
 * @property value Repeater 请求的唯一文本标识符，协议示例约定使用 `repeater_` 前缀。
 */
@JvmInline
@Serializable
value class RepeaterRequestIdentifier(val value: String)
