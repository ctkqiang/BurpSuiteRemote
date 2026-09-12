package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/**
 * 扫码弹窗发出的一次性效果。
 *
 * 票据文本只能交出去一次：交给装配层之后这一屏的使命就结束了，重复交付会变成重复配对。
 */
internal sealed interface BurpRemoteScannerDialogUserInterfaceEffect {
    /** 识别并通过本地校验的票据文本；装配层拿它去走配对链路。 */
    data class TicketAccepted(val encodedTicketText: String) : BurpRemoteScannerDialogUserInterfaceEffect
}
