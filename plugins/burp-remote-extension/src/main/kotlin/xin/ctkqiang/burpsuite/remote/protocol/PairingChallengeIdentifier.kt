/**
 * Burp Remote —— 协议层 / 配对
 *
 * 声明「一次配对尝试」的身份类型。配对只回答一个问题——这台设备是谁——因此每一次尝试
 * 都必须能被独立识别与作废，否则插件无法回答「屏幕上这张二维码是否还有效」。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识一次配对尝试。
 *
 * 该值随票据写入二维码，并在移动端提交配对码时回传。插件据此把一次提交对应到某一次
 * 具体的配对会话：只有当前仍有效的那个标识才可能配对成功，旧标识一律拒绝。
 *
 * 之所以让它独立于配对码存在，是因为两者的安全属性不同：标识用来定位会话，可以安全地
 * 写进日志与审计记录；配对码用来证明持有者确实看过屏幕，属于凭证，绝不能落进日志。
 * 把两者合并成一个值，就等于放弃「能审计但不泄密」这条中间道路。
 *
 * @property value 配对会话的唯一文本标识符，协议示例约定使用 `challenge_` 前缀。
 */
@JvmInline
@Serializable
value class PairingChallengeIdentifier(val value: String)
