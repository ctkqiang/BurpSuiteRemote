// 本机地址解析的端口：配对票据该填哪个本机地址。

package xin.ctkqiang.burpsuite.remote.security

/**
 * 解析移动端应当连接的本机地址。
 *
 * 抽成接口是为了让配对逻辑能脱离网络测试；同一个地址既要写进票据，也决定远程端点监听在哪，两处必须同源。
 */
interface LocalNetworkAddressResolver {
    /**
     * 返回应当写进配对票据的本机地址；没有可用的局域网接口时返回 null。
     *
     * 回环地址和链路本地地址（169.254/16）移动端都到不了，一律不返回。
     */
    fun resolveAdvertisedHostAddress(): String?
}
