/**
 * Burp Remote —— 传输层 / 本机地址
 *
 * 实现本机地址的探测。地址由操作系统的网卡状态决定，属于环境事实，因此探测动作被隔离在
 * 传输层：配对服务只依赖 `LocalNetworkAddressResolver` 接口，无需知道网卡的存在。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.security.LocalNetworkAddressResolver
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

/**
 * 通过枚举操作系统网卡来解析本机地址。
 *
 * 选择「枚举网卡并筛选站点本地地址」，而不是向外部服务查询本机的公网地址：配对发生在同一
 * 局域网内，向第三方暴露内网结构既无必要，也无法通过安全审计（plan §54：局域网不等于
 * 可信网络）。
 */
class SystemLocalNetworkAddressResolver : LocalNetworkAddressResolver {
    /**
     * 返回一个可用的站点本地 IPv4 地址。
     *
     * 多网卡机器上「第一个」并非稳定概念，因此这里对结果排序，使同一台机器在多次调用中
     * 得到同一个答案——一个会随机变化的配对地址，会让「上次能连上、这次连不上」变成
     * 无法复现的故障。等设置界面就绪后，应当由操作者显式选择对外公布的网卡，而不是由
     * 代码替他决定。
     *
     * @return 点分十进制 IPv4 地址；没有可用接口时返回 null。
     * @throws SocketException 枚举网卡失败时抛出。这是环境级故障，不予吞掉，否则插件会在
     *   一个连自己网络状态都读不到的进程里继续运行。
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
