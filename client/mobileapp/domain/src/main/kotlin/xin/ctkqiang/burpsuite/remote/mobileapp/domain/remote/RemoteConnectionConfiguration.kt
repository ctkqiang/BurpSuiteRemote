package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 连接一个插件端点所需的配置；plan §47 把这几项放在设置里，由用户维护。
 *
 * TLS 现状：插件侧目前只提供明文传输，[isTlsEnabled] 打开只会改用 https 与 wss 方案，
 * 不代表链路已经加密——真要有加密，得插件侧先实现。
 */
data class RemoteConnectionConfiguration(
    /** 插件所在主机地址。 */
    val host: String,
    /** 插件监听端口。 */
    val port: Int,
    /** 本机设备名，供配对与审计追溯显示。 */
    val deviceName: String,
    /** 是否使用 TLS 方案；插件侧未实现 TLS 前保持关闭。 */
    val isTlsEnabled: Boolean = false,
)
