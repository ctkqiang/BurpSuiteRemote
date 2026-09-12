package xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings

/**
 * 插件端点的地址与端口（plan §47）。
 *
 * 成对存放而不是两条各自独立的偏好：只改其中一个会留下一个必然连不上的组合，
 * 而那个中间态既没有用，也没法向用户解释。
 */
data class RemoteServerEndpoint(
    /** 插件所在主机地址。 */
    val host: String,
    /** 插件监听端口。 */
    val port: Int,
)
