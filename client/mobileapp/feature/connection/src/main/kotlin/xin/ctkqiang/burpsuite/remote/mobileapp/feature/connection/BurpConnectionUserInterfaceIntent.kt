package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import androidx.camera.core.ImageProxy

/**
 * 用户在连接屏上的意图。
 *
 * 相机权限申请、条码识别、配对命令与偏好写入都不在界面里做：界面只报告「用户点了什么」「相机
 * 送来了一帧」「输入框里现在是什么」，具体怎么问系统、怎么解二维码、怎么发命令由 ViewModel 决定
 * （rules.md §8.3）。
 */
sealed interface BurpConnectionUserInterfaceIntent {
    /** 用户改了地址输入。 */
    data class UpdateHostInput(val hostInput: String) : BurpConnectionUserInterfaceIntent

    /** 用户改了端口输入。 */
    data class UpdatePortInput(val portInput: String) : BurpConnectionUserInterfaceIntent

    /** 用户改了手输的配对文本。 */
    data class UpdatePairingTextInput(val pairingTextInput: String) : BurpConnectionUserInterfaceIntent

    /** 用户要求保存地址与端口。 */
    data object SaveEndpoint : BurpConnectionUserInterfaceIntent

    /** 用户提交配对；扫码得到的文本与手输的文本走同一条链路。 */
    data object StartPairing : BurpConnectionUserInterfaceIntent

    /** 外部递进来一张票据文本（仅调试入口）；等同「刚扫到」，之后立刻配对。 */
    data class PairTicketText(val ticketText: String) : BurpConnectionUserInterfaceIntent

    /** 用户打开了扫码面板。 */
    data object OpenPairingScanner : BurpConnectionUserInterfaceIntent

    /** 用户关掉了扫码面板。 */
    data object ClosePairingScanner : BurpConnectionUserInterfaceIntent

    /** 用户切换了扫码补光。 */
    data object ToggleScannerTorch : BurpConnectionUserInterfaceIntent

    /** 用户要求申请相机权限。 */
    data object RequestCameraPermission : BurpConnectionUserInterfaceIntent

    /** 界面报告了权限申请的结果。 */
    data class ReportCameraPermission(val isGranted: Boolean) : BurpConnectionUserInterfaceIntent

    /** 相机送来一帧待识别的画面。 */
    data class AnalyzeCameraFrame(val imageProxy: ImageProxy) : BurpConnectionUserInterfaceIntent

    /** 用户要求重试连接。 */
    data object RetryConnection : BurpConnectionUserInterfaceIntent

    /** 用户要求清除配对。 */
    data object ClearPairing : BurpConnectionUserInterfaceIntent
}
