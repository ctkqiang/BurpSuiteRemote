package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 插件自报的能力清单。
 *
 * 用文本集合而不是枚举：新版插件多报一项能力时，旧客户端必须能原样带着它，而不是整条应答解析失败。
 */
data class ServerCapabilities(
    /** 能力名，例如 status、pairing、event-stream、snapshot。 */
    val names: Set<String>,
)
