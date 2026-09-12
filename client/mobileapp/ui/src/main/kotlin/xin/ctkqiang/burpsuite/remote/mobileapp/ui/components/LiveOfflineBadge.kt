package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 实时/离线徽标（plan §59）。
 *
 * 只有「会话此刻可用」才算 LIVE，其余状态一律 OFFLINE——过渡态和故障态都不该被念成在线。
 */
@Composable
fun LiveOfflineBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val isLive = connectionState.isConnected
    Box(modifier = modifier) {
        BurpRemoteStatusPill(
            text =
                stringResource(
                    if (isLive) R.string.components_mode_live else R.string.components_mode_offline,
                ),
            tone = if (isLive) BurpRemoteStatusTone.Live else BurpRemoteStatusTone.Neutral,
        )
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun LiveOfflineBadgeLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        LiveOfflineBadge(connectionState = ConnectionState.Connected)
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun LiveOfflineBadgeDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        LiveOfflineBadge(connectionState = ConnectionState.Disconnected)
    }
}
