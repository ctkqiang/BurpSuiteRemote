// 传输层：枚举网卡，解析本机地址。

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.security.LocalNetworkAddressResolver
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 通过枚举操作系统网卡来解析本机地址。
 *
 * 不向外部服务查公网地址：配对本来就在同一局域网内，把内网结构暴露给第三方没有必要，也过不了安全审计。
 */
class SystemLocalNetworkAddressResolver : LocalNetworkAddressResolver {
    /**
     * 返回一个可用的站点本地 IPv4 地址；没有可用接口时返回 null。
     *
     * 结果排序是因为多网卡下「第一个」不稳定；网卡枚举失败时异常直接往外抛，不吞掉环境级故障。
     */
    override fun resolveAdvertisedHostAddress(): String? = collectAdvertisableAddresses().firstOrNull()

    private fun collectAdvertisableAddresses(): List<String> =
        NetworkInterface.getNetworkInterfaces()
            .iterator()
            .asSequence()
            .filter { it.isUp }
            .filterNot { it.isLoopback }
            .filterNot { it.isVirtual }
            .flatMap { networkInterface -> networkInterface.inetAddresses.iterator().asSequence() }
            .filterIsInstance<Inet4Address>()
            .filter { it.isSiteLocalAddress }
            .map { it.hostAddress }
            .sorted()
            .toList()
}
