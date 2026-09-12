/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明「一条 HTTP 历史记录」的身份类型。历史记录由 Burp 拥有，插件只做转述，
 * 因此该身份必须能够跨进程、跨重启稳定地指回 Burp 中的同一条记录。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识 Burp 观察到的一条 HTTP 历史记录。
 *
 * 该值用于把列表项与详情接口关联起来：客户端先取回历史摘要列表，再按此标识符
 * 拉取单条详情，因此它必须在一次会话内稳定，且不得随列表分页顺序变化。
 *
 * 取值来源于 Burp 自身的记录身份，插件不得重新推导或自行分配。历史记录归属于
 * Burp（见规则第 5.8 条），插件不重建它，只引用它。
 *
 * 序列化时按底层文本透明写出。
 *
 * @property value 历史记录的唯一文本标识符，协议示例约定使用 `history_` 前缀。
 */
@JvmInline
@Serializable
value class HistoryIdentifier(val value: String)
