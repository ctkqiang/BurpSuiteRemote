/**
 * Burp Remote —— 协议层 / 消息类别
 *
 * 声明线上消息的类别枚举。每条消息都必须携带该字段，接收方据此判断这条报文该走命令
 * 通道还是事件通道，而不必先解析载荷再做决定。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 线上消息的类别。
 *
 * 该枚举是封闭集合，接收方遇到未知类别必须明确拒绝，而不是降级成「忽略这条消息」。
 * 把不认识的报文静默丢弃，会让协议版本不匹配伪装成业务数据缺失，这是最难排查的一类
 * 故障：两端都认为自己工作正常，只有数据在悄悄变少。
 *
 * 序列化值取单个小写单词，与协议文档中的示例保持一致；它们是协议契约的一部分，
 * 改动等于破坏兼容性，必须同时提升协议版本。
 */
@Serializable
enum class RemoteMessageType {
    /** 客户端发起的意图，走 REST 命令通道，必须携带操作标识符以保证重试幂等。 */
    @SerialName("command")
    Command,

    /** 一次只读查询，走 REST 查询通道，不产生任何副作用。 */
    @SerialName("query")
    Query,

    /** 全量或区间快照，用于客户端投影损坏、或所需序列号区间已无法继续服务时的重建。 */
    @SerialName("snapshot")
    Snapshot,

    /** 已发生的客观事实，走 WebSocket 实时通道，只推送元数据而不携带大体积报文正文。 */
    @SerialName("event")
    Event,

    /** 序列号出现空洞时的补发请求，绝不允许静默跳过缺失区间。 */
    @SerialName("resume")
    Resume,
}
