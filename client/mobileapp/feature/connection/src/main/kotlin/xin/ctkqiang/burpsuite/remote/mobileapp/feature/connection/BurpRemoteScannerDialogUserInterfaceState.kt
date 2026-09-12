package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion
import java.time.Instant

/**
 * 扫码弹窗的状态。
 *
 * [rejectionTicketProtocolVersion] 与 [rejectionTicketExpiresAt] 只在一张票据被本地拦下时才有值：
 * 协议不符要把两个版本号都写出来，过期要把失效时刻写出来，光说一句「票据不可用」等于让用户自己猜。
 *
 * @property mode 当前输入方式。
 * @property cameraPermission 相机权限此刻的处境。
 * @property isTorchEnabled 补光是否打开。
 * @property manualTicketText 手输框里的内容。
 * @property rejection 票据为什么用不了；可用或还没扫到时为空。
 * @property rejectionTicketProtocolVersion 被拦下的票据声明的协议版本。
 * @property rejectionTicketExpiresAt 被拦下的票据的失效时刻。
 * @property supportedProtocolVersion 本客户端支持的协议版本；协议不符时与上面那个一起显示。
 */
internal data class BurpRemoteScannerDialogUserInterfaceState(
    val mode: PairingScannerMode = PairingScannerMode.Camera,
    val cameraPermission: PairingCameraPermission = PairingCameraPermission.Unknown,
    val isTorchEnabled: Boolean = false,
    val manualTicketText: String = "",
    val rejection: PairingTicketRejection? = null,
    val rejectionTicketProtocolVersion: Int? = null,
    val rejectionTicketExpiresAt: Instant? = null,
    val supportedProtocolVersion: Int = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
)
