package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState

/**
 * Burp 连接屏状态。
 *
 * 每个「还不能做」的动作都配一个 [pairingCommandUnavailableReasonResource]，界面据此画成禁用并写出原因；
 * 能扫到的票据是真的，扫到就照实显示，不替它补默认值。
 */
data class BurpConnectionUserInterfaceState(
    /** 当前连接状态。 */
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    /** 插件所在主机地址；还没扫到票据时为 null，界面按「尚未配置」显示。 */
    val serverAddress: String? = null,
    /** 插件监听端口；没有票据时用协议层的默认值。 */
    val serverPort: Int = DEFAULT_REMOTE_PORT,
    /** 用户正在输入的配对码。 */
    val pairingCodeInput: String = "",
    /** 扫码面板是否打开。 */
    val isScannerOpen: Boolean = false,
    /** 相机权限此刻的处境。 */
    val cameraPermission: PairingCameraPermission = PairingCameraPermission.Unknown,
    /** 扫到的配对票据；没扫到或解不出来时为 null。 */
    val scannedTicket: PairingTicket? = null,
    /** 扫到了内容但解不成票据——二维码不是本协议的东西，界面照实说，不猜。 */
    val hasUndecodableTicket: Boolean = false,
    /** 扫到的票据是否已经过了失效时刻。 */
    val isScannedTicketExpired: Boolean = false,
    /** 配对或重试这类控制命令此刻有没有可发的通路；没有时为空，界面把原因写在按钮旁。 */
    val pairingCommandUnavailableReasonResource: Int? = null,
    /** [pairingCommandUnavailableReasonResource] 为空时，配对命令是否已经具备全部输入。 */
    val canSubmitPairing: Boolean = false,
    /** 重试连接是否可用。 */
    val canRetryConnection: Boolean = false,
    /** 上一次配对尝试的结局；还没试过时为 null。 */
    val pairingOutcome: PairingOutcome? = null,
)
