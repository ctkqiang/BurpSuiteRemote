// 技术日志的分类，决定一条日志该被当成哪一类事实来读。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

/** 技术日志的分类。分类是日志的第一层结构，按它过滤就能只看自己关心的那一段。 */
enum class TechnicalLogCategory {
    /** 配对：扫码、票据校验、配对请求、设备身份落地。 */
    Pairing,

    /** 传输：REST 请求与应答码。 */
    Transport,

    /** 事件通道：WebSocket 握手、续传序号、会话结束原因。 */
    EventStream,

    /** 事件摄入：序号、去重命中、断洞。 */
    EventIngestion,

    /** 导航：路由切换。 */
    Navigation,

    /** 界面：用户动作、投影读取与刷新结果。 */
    UserInterface,

    /** 故障：需要人看的异常摘要。 */
    Failure,
}
