// 线上协议版本的唯一事实来源。

package xin.ctkqiang.burpsuite.remote.protocol

/** 协议版本的唯一事实来源，消息构造函数都读这里，别在各处硬编码字面量。 */
object RemoteProtocolVersion {
    /** 当前插件与移动端共同遵守的协议版本；只增不减。 */
    const val CURRENT_PROTOCOL_VERSION: Int = 1
}
