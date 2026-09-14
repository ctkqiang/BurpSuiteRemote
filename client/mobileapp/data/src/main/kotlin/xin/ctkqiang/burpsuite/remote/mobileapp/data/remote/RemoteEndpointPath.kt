package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

/** 客户端用到的插件端点路径；路径字面量只此一份，改协议时不必全仓库搜索替换。 */
object RemoteEndpointPath {
    /** 运行态查询。 */
    const val STATUS = "/v1/status"

    /** 设备配对。 */
    const val PAIR = "/v1/pair"

    /** 能力清单查询。 */
    const val CAPABILITIES = "/v1/capabilities"

    /** 状态快照查询。 */
    const val SNAPSHOT = "/v1/snapshot"

    /** 历史列表查询；取单条时在其后追加「/」与历史标识。 */
    const val HISTORY = "/v1/history"

    /** 拦截队列；单条操作在其后追加「/」、拦截标识与下列动作后缀。 */
    const val INTERCEPTS = "/v1/intercepts"

    /** 修改一条被拦截的报文。 */
    const val INTERCEPT_MODIFY_SUFFIX = "/modify"

    /** 放行一条被拦截的报文。 */
    const val INTERCEPT_FORWARD_SUFFIX = "/forward"

    /** 丢弃一条被拦截的报文。 */
    const val INTERCEPT_DROP_SUFFIX = "/drop"

    /** Repeater 请求；执行单条时在其后追加「/」、请求标识与执行后缀。 */
    const val REPEATER = "/v1/repeater"

    /** 执行一条 Repeater 请求。 */
    const val REPEATER_EXECUTE_SUFFIX = "/execute"

    /** 作用域；把某条历史记录的主机加入作用域时，在其后追加「/」与历史标识。 */
    const val SCOPE = "/v1/scope"

    /** 事件通道。 */
    const val EVENTS = "/v1/events"
}
