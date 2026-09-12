/**
 * Burp Remote —— 协议层 / 配对
 *
 * 声明二维码所承载的配对票据结构。票据是插件与移动端之间关于「连接到哪里、用什么凭证」
 * 的唯一约定，因此它的字段名属于线上契约：改动字段名必须同时提升协议版本号。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 一次配对所必需的完整信息，也就是二维码实际编码的内容。
 *
 * 把「连接到哪里」与「如何证明自己」放进同一份数据，是为了让移动端扫一次即可完成配对：
 * 若地址与凭证分两处呈现，操作者就得在两台设备之间来回抄写，而抄错一位的表现只是
 * 「配对失败」，排查成本远高于把所有字段都编进二维码。
 *
 * 票据同时也是需要收敛暴露面的数据——它包含一次性配对码，并且会显示在屏幕上。缓解手段
 * 有三条：有效期只有几分钟、同一时刻只存在一张有效票据、配对成功后该票据立即作废。
 *
 * @property protocolVersion 生成该票据的插件所遵守的协议版本，移动端据此拒绝不兼容的票据。
 * @property host 移动端应当连接的本机地址，由插件探测得到。
 * @property port 移动端应当连接的端口，即远程端点预留的监听端口。
 * @property challengeIdentifier 本次配对会话的标识，移动端提交时原样回传以定位会话。
 * @property pairingCode 一次性配对码，用于证明发起者确实看到了屏幕。
 * @property expiresAt 票据失效的绝对时刻。这里用时间点而不是时长，因此不受移动端时钟
 *   漂移影响：即使手机时间快了十分钟，票据是否有效仍由插件一侧判定。
 */
@Serializable
data class PairingTicket(
    val protocolVersion: Int,
    val host: String,
    val port: Int,
    val challengeIdentifier: PairingChallengeIdentifier,
    val pairingCode: PairingCode,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val expiresAt: Instant,
)
