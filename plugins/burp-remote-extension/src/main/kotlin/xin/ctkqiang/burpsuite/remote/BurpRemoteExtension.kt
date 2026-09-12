/**
 * Burp Remote —— 扩展入口
 *
 * 本文件是整个 Burp 扩展唯一的入口点。Burp 在加载 JAR 时会遍历其中的类，
 * 查找实现了 `burp.api.montoya.BurpExtension` 的类型；只要一个都没有找到，
 * Burp 就会抛出 `Extension class is not a recognized type` 并拒绝加载。
 *
 * 因此入口类只承担「接线」职责：声明扩展名称、装配配对服务与界面组件、登记卸载回调。
 * 它不承载协议逻辑，也不承载事件溯源逻辑，这样插件在卸载时才可能干净退出，
 * 而不会留下悬挂的线程、端口占用或残留的界面。
 *
 * @author 钟智强
 */

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
 * Montoya 运行时会自行实例化本类并调用 [initialize]，因此本类必须保留一个公开的
 * 无参构造器。构造器中不得执行任何副作用：真正的工作一律在 [initialize] 内完成，
 * 这样一旦初始化失败，Burp 才能把异常准确地归因到本扩展，而不是在类加载阶段
 * 抛出难以排查的错误。
 */
class BurpRemoteExtension : BurpExtension {
    /**
     * 扩展初始化钩子，由 Burp 在加载完成后调用一次。
     *
     * 这里做四件事：声明扩展名称、装配安全层的配对服务、注册界面标签页、登记卸载回调。
     * 卸载回调必须在此处注册，因为 Montoya 要求卸载处理器只能在扩展仍处于已加载状态时
     * 登记，一旦卸载流程开始就无法补注册；而标签页的注销动作只能写在该回调里，所以注册
     * 句柄必须一直存活到那一刻。
     *
     * @param montoyaApi Burp 注入的运行时句柄，提供日志、代理、HTTP 等能力。
     */
    override fun initialize(montoyaApi: MontoyaApi) {
        montoyaApi.extension().setName(EXTENSION_NAME)
        // 日志只记录加载状态，绝不记录配对码：配对码属于一次性凭证，而 Burp 的输出面板常被
        // 用户整段复制进缺陷报告，凭证出现在那里就等同于被公开（rules.md §12）。
        montoyaApi.logging().logToOutput("Burp Remote 已加载：$EXTENSION_NAME")

        // 时钟在入口处创建一次并注入所有依赖方：票据的有效期由安全层判定、剩余时长由界面
        // 展示，两者必须读同一个时间源，否则界面上的倒计时与真正的失效时刻会出现偏差。
        val clock = Clock.systemUTC()

        // 已配对设备的登记处在这里创建，并同时交给配对服务与界面：服务负责写入，界面负责读取。
        // 两者共用同一个实例是必须的——各持一份，界面就会显示出一份与真实授权状态无关的列表，
        // 而这份列表正是操作者用来判断「谁还能控制我这台 Burp」的依据。
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
                // 界面语言取自操作系统的语言设置，在这里解析一次并注入。控件内部不读环境，
                // 语言因此成为一项可替换的输入，而不是散落在各处的隐式依赖（rules.md §9）。
                locale = Locale.getDefault(),
                pairingTicketSupplier = devicePairingService::openPairingSession,
                pairedDeviceSupplier = pairedDeviceRegistry::snapshot,
                // 用 lambda 而不是方法引用：登记处的移除方法返回「是否确实存在」，而面板只关心
                // 「请求已发出、随后按真实状态重读列表」，不需要也不应该依赖那个返回值。
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
            // 卸载时必须主动摘掉标签页。Burp 不会替扩展回收已经交给它的组件：保留注册
            // 会让界面残留，并在重新加载扩展时叠出两个同名标签页。
            suiteTabRegistration.deregister()

            montoyaApi.logging().logToOutput("Burp Remote 正在卸载：$EXTENSION_NAME")
        }
    }

    private companion object {
        /**
         * 展示在 Burp「已安装扩展」列表中的名称。
         *
         * 使用常量而非字面量，是为了避免同一名称在注册与日志两处出现不一致的拼写。
         *
         * 它是产品名，不参与本地化：Burp 自身的扩展列表只有英文，一个随语言改名的扩展会让
         * 「用户口中的那个标签页」与支持工单里记录的扩展名对不上。
         */
        private const val EXTENSION_NAME = "Burp Remote"
    }
}
