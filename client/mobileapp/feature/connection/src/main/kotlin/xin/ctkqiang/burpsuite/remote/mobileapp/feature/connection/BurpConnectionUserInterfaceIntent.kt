package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import androidx.camera.core.ImageProxy

/**
 * 用户在连接屏上的意图。
 *
 * 相机权限申请与条码识别都不在界面里做：界面只报告「发生了一帧」「用户点了申请」，
 * 具体怎么问系统、怎么解二维码由 ViewModel 决定（rules.md §8.3）。
 */
sealed interface BurpConnectionUserInterfaceIntent {
    /** 用户改了配对码输入。 */
    data class UpdatePairingCodeInput(val pairingCodeInput: String) : BurpConnectionUserInterfaceIntent

    /** 用户打开了扫码面板。 */
    data object OpenPairingScanner : BurpConnectionUserInterfaceIntent

    /** 用户关掉了扫码面板。 */
    data object ClosePairingScanner : BurpConnectionUserInterfaceIntent

    /** 用户要求申请相机权限。 */
    data object RequestCameraPermission : BurpConnectionUserInterfaceIntent

    /** 界面报告了权限申请的结果。 */
    data class ReportCameraPermission(val isGranted: Boolean) : BurpConnectionUserInterfaceIntent

    /** 相机送来一帧待识别的画面。 */
    data class AnalyzeCameraFrame(val imageProxy: ImageProxy) : BurpConnectionUserInterfaceIntent

    /** 用户提交配对。 */
    data object SubmitPairing : BurpConnectionUserInterfaceIntent

    /** 用户要求重试连接。 */
    data object RetryConnection : BurpConnectionUserInterfaceIntent
}
