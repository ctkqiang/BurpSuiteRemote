// 夹具用的本机地址解析：把票据里的 host 固定成调用方给的值。

package xin.ctkqiang.burpsuite.remote.harness

import xin.ctkqiang.burpsuite.remote.security.LocalNetworkAddressResolver

/** 夹具使用的地址解析；模拟器要靠 10.0.2.2 这类宿主机别名才连得上宿主机上的服务端。 */
class HarnessAdvertisedHostResolver(private val advertisedHostAddress: String) : LocalNetworkAddressResolver {
    override fun resolveAdvertisedHostAddress(): String = advertisedHostAddress
}
