// 扩展入口：唯一的 BurpExtension 实现，只负责接线。

package xin.ctkqiang.burpsuite.remote

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Registration
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.security.DevicePairingService
import xin.ctkqiang.burpsuite.remote.security.PairedDeviceRegistry
import xin.ctkqiang.burpsuite.remote.transport.SystemLocalNetworkAddressResolver
import xin.ctkqiang.burpsuite.remote.userinterface.RemoteStatusPanel
import java.time.Clock
import java.util.Locale

/**
 * Burp Remote 的扩展入口。
 *
 * Montoya 会自行实例化本类并调用 [initialize]，所以要保留公开无参构造器；构造器里不做副作用，初始化失败时 Burp 才能把异常归到本扩展。
 */
class BurpRemoteExtension : BurpExtension {
    /**
     * 扩展初始化钩子，由 Burp 在加载完成后调用一次。
     *
     * 声明名称、装配配对服务、注册标签页并登记卸载回调；卸载回调只能在仍是已加载状态时注册，所以注册句柄要一直存活。
     */
    override fun initialize(montoyaApi: MontoyaApi) {
        montoyaApi.extension().setName(EXTENSION_NAME)
        // 日志只记状态，不记配对码：Burp 输出面板常被整段复制进缺陷报告，凭证出现在那里就等于公开。
        montoyaApi.logging().logToOutput("Burp Remote 已加载：$EXTENSION_NAME")

        // 时钟只建一次并注入各方：有效期由安全层判定、倒计时由界面展示，两者必须读同一个时间源。
        val clock = Clock.systemUTC()

        // 登记处以同一个实例同时交给服务与界面：各持一份，界面就会显示与真实授权状态无关的列表。
        val pairedDeviceRegistry = PairedDeviceRegistry()

        val devicePairingService =
            DevicePairingService(
                localNetworkAddressResolver = SystemLocalNetworkAddressResolver(),
                pairedDeviceRegistry = pairedDeviceRegistry,
                clock = clock,
                remotePort = DEFAULT_REMOTE_PORT,
            )

        val statusPanel =
            RemoteStatusPanel(
                extensionName = EXTENSION_NAME,
                clock = clock,
                // 语言在这里解析一次再注入，控件内部不读环境，于是它成了可替换的输入而不是隐式依赖。
                locale = Locale.getDefault(),
                pairingTicketSupplier = devicePairingService::openPairingSession,
                pairedDeviceSupplier = pairedDeviceRegistry::snapshot,
                // 用 lambda 而不是方法引用：移除方法返回是否命中，而面板只关心请求已发出、随后重读列表。
                pairedDeviceRevoker = { deviceIdentifier ->
                    pairedDeviceRegistry.removePairedDevice(deviceIdentifier)
                },
            )

        // 标签页继承 Burp 当前主题，否则在深色模式下会是一块刺眼的浅色区域。
        montoyaApi.userInterface().applyThemeToComponent(statusPanel)

        // 只加载 JAR 不会产生任何界面，标签页必须在此显式注册。
        val suiteTabRegistration: Registration =
            montoyaApi.userInterface().registerSuiteTab(EXTENSION_NAME, statusPanel)

        montoyaApi.extension().registerUnloadingHandler {
            // 卸载时主动摘掉标签页：Burp 不替扩展回收组件，留着会残留界面，重载时还会叠出两个同名标签页。
            suiteTabRegistration.deregister()

            montoyaApi.logging().logToOutput("Burp Remote 正在卸载：$EXTENSION_NAME")
        }
    }

    private companion object {
        // 产品名不参与本地化，Burp 的扩展列表只有英文，改名会和支持工单里记的扩展名对不上。
        private const val EXTENSION_NAME = "Burp Remote"
    }
}
