package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.RemoteServerEndpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingConclusion

/**
 * Burp 连接屏状态（plan §47 的连接配置、plan §55 的配对流程）。
 *
 * 「还不能做」的每个动作都配一个 [unavailableReasonResource]，界面据此画成禁用并写出原因；
 * 扫到的票据是真的，扫到就照实显示，不替它补默认值，也不把「解不出来」说成「没扫到」。
 */
data class BurpConnectionUserInterfaceState(
    /** 当前连接状态。 */
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    /** 已保存的插件端点；配对成功或用户保存后由它给出，来自票据的那份值就在这里。 */
    val savedEndpoint: RemoteServerEndpoint? = null,
    /** 本机是否已持有插件签发的设备身份。 */
    val isPaired: Boolean = false,
    /** 地址输入框里的内容；没配过端点时为空。 */
    val hostInput: String = "",
    /** 端口输入框里的内容；空表示还没配过。端口是文本，输入过程中允许半截数字。 */
    val portInput: String = "",
    /** 手输或粘贴的配对文本；与扫码得到的文本走同一条配对链路。 */
    val pairingTextInput: String = "",
    /** 扫码面板是否打开。 */
    val isScannerOpen: Boolean = false,
    /** 相机权限此刻的处境。 */
    val cameraPermission: PairingCameraPermission = PairingCameraPermission.Unknown,
    /** 扫码补光是否打开。 */
    val isTorchEnabled: Boolean = false,
    /** 扫到的配对票据；没扫到或解不出来时为 null。 */
    val scannedTicket: PairingTicket? = null,
    /** 票据为什么用不了；可用或还没扫到时为 null。 */
    val ticketRejection: PairingTicketRejection? = null,
    /** 本客户端支持的协议版本；协议不符时界面要把两个版本都写出来。 */
    val supportedProtocolVersion: Int = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
    /** 配对或连接这类控制命令此刻有没有可发的通路；没有时为空，界面把原因写在按钮旁。 */
    val unavailableReasonResource: Int? = null,
    /** 配对请求是否还在飞。 */
    val isPairingInFlight: Boolean = false,
    /** 上一次配对尝试的结局；还没试过时为 null。 */
    val pairingOutcome: PairingOutcome? = null,
    /** 配对被拒时的归类；成功或还没试过时为 null。 */
    val pairingRejectionReason: PairingRejectionReason? = null,
    /** 上一次配对尝试的具体结论；失败原因由它的文案给出，不合并成一句「配对失败」。 */
    val pairingConclusion: RemotePairingConclusion? = null,
) {
    /** 会话此刻是否真的可用；只有 Connected 算在线（plan §59）。 */
    val isLive: Boolean get() = connectionState.isConnected

    /** 票据能不能用来配对：拿到了、解得出、没过期、协议版本也对得上。 */
    val isTicketUsable: Boolean get() = scannedTicket != null && ticketRejection == null

    /** 输入框里的端口；写不成合法端口时为 null。 */
    val parsedPortInput: Int? get() = portInput.toIntOrNull()?.takeIf { port -> port in VALID_PORT_RANGE }

    /** 地址与端口都对了才允许保存。 */
    val isEndpointSavable: Boolean get() = hostInput.isNotBlank() && parsedPortInput != null

    /** 端口填了但不是合法端口；界面据此写出「端口该长什么样」。 */
    val hasInvalidPort: Boolean get() = portInput.isNotBlank() && parsedPortInput == null

    /** 配对需要有文本可走。 */
    val canStartPairing: Boolean get() = pairingTextInput.isNotBlank() && !isPairingInFlight

    /** 重试连接要有已保存的端点。 */
    val canRetryConnection: Boolean get() = savedEndpoint != null
}

/** 端口是 16 位无符号数：0 与 65536 都不该被送进传输层。 */
private val VALID_PORT_RANGE = 1..65_535
