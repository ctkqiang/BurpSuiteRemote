package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 网络超时。
 *
 * 原文未定义数值，这里集中一处并允许注入：超时一旦写死在实现里，测试就只能真等，
 * 而一个不肯等的测试迟早会被删掉。
 */
data class RemoteTimeouts(
    /** 建立连接与握手的等待上限。 */
    val handshakeMilliseconds: Long = 5_000L,
    /** 单次 REST 请求的等待上限。 */
    val requestMilliseconds: Long = 10_000L,
)
