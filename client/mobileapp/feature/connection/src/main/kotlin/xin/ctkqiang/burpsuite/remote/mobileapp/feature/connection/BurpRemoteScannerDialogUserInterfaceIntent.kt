package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/** 用户在扫码弹窗上的意图。识别与校验都发生在状态持有处，界面只报告「用户点了什么 / 相机送来了一帧」。 */
internal sealed interface BurpRemoteScannerDialogUserInterfaceIntent {
    /** 切换到另一种输入方式。 */
    data class SelectMode(val mode: PairingScannerMode) : BurpRemoteScannerDialogUserInterfaceIntent

    /** 切换补光。 */
    data object ToggleTorch : BurpRemoteScannerDialogUserInterfaceIntent

    /** 手输框的内容变了。 */
    data class UpdateManualTicketText(
        val manualTicketText: String,
    ) : BurpRemoteScannerDialogUserInterfaceIntent

    /** 提交手输的配对文本。 */
    data object SubmitManualTicketText : BurpRemoteScannerDialogUserInterfaceIntent

    /** 界面报告了权限申请的结果。 */
    data class ReportCameraPermission(val isGranted: Boolean) : BurpRemoteScannerDialogUserInterfaceIntent
}
