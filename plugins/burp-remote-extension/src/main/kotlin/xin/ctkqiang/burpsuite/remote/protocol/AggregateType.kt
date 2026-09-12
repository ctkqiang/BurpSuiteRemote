/**
 * Burp Remote —— 协议层 / 聚合类型
 *
 * 声明事件所归属的聚合类型。它与聚合标识符组成一个引用，用来回答「这条事件属于哪一个
 * 业务对象」，而无需解析事件载荷。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 事件所属的聚合类型。
 *
 * 聚合类型的取值范围刻意保持很小：事件溯源最容易被滥用的地方，就是把每一个名词都
 * 变成聚合，于是重放成本随对象数量线性增长，而业务规则却没有任何对应的不变量需要
 * 保护。这里的每一项都对应一组真正需要按顺序重放的状态迁移。
 *
 * 序列化值使用下划线分隔的机器码，与数据库列名风格一致，且不携带任何界面语言——
 * 插件只产出稳定的机器码，由客户端负责翻译成用户看得懂的文字。
 */
@Serializable
enum class AggregateType {
    /** 一台被配对并接入远程会话的移动设备。 */
    @SerialName("device")
    Device,

    /** 一条被观察、保存或标注的 Burp 历史记录。 */
    @SerialName("history")
    History,

    /** 一个正被挂起等待处置的拦截项。 */
    @SerialName("intercept")
    Intercept,

    /** 一个被送往 Repeater 的请求及其执行结果。 */
    @SerialName("repeater_request")
    RepeaterRequest,

    /** 一张被导入或美化的截图。 */
    @SerialName("screenshot")
    Screenshot,

    /** 一次远程控制会话的建立与结束。 */
    @SerialName("remote_session")
    RemoteSession,
}
