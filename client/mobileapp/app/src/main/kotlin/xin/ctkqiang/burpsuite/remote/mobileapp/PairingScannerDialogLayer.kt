package xin.ctkqiang.burpsuite.remote.mobileapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection.BurpRemoteScannerDialog
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingConclusion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 扫码弹窗的容器层，由外壳持有。
 *
 * 两件事只有外壳做得成：一是用不透明底色盖住顶栏与底栏——弹窗开着的时候，底下那条底栏会让用户
 * 以为自己还能切分区；二是把「配对走到哪一步了」说出来。弹窗自己只判票据能不能用（解不出、过期、
 * 协议不符），插件侧的结论只有外壳拿得到（[RemotePairingConclusion]），不在这里显示就等于丢了。
 *
 * 取景、手输、本地校验与关闭按钮都在 [BurpRemoteScannerDialog] 里，这里不重复任何一份。
 *
 * @param conclusion 插件侧对上一次提交的结论；成功时弹窗已被外壳收掉，因此这里只处理失败。
 * @param isPairingInFlight 配对请求是否还在飞；在飞期间只加一条状态条，不遮挡关闭入口，
 *   因此配对卡住时用户仍然出得去。
 */
@Composable
internal fun PairingScannerDialogLayer(
    conclusion: RemotePairingConclusion?,
    isPairingInFlight: Boolean,
    onDismiss: () -> Unit,
    onScannedTicketText: (String) -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    Box(modifier = Modifier.fillMaxSize().background(tokens.colourScheme.background)) {
        BurpRemoteScannerDialog(
            onDismiss = onDismiss,
            onScannedTicketText = onScannedTicketText,
        )

        // 居中而不是压在手电筒与关闭上：那两处都在弹窗自己的角落，挡住它们等于把出口收掉。
        when {
            isPairingInFlight ->
                PairingStatus(
                    text = stringResource(R.string.pairing_scanner_status_in_flight),
                    tone = BurpRemoteStatusTone.Warning,
                )

            conclusion != null && conclusion != RemotePairingConclusion.Succeeded ->
                PairingStatus(
                    text = stringResource(conclusion.messageResource),
                    tone = BurpRemoteStatusTone.Danger,
                )

            else -> Unit
        }
    }
}

@Composable
private fun PairingStatus(
    text: String,
    tone: BurpRemoteStatusTone,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BurpRemoteStatusPill(text = text, tone = tone)
    }
}
