package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 实时/离线徽标（plan §59）。
 *
 * 只有「会话此刻可用」才算 LIVE，其余状态一律 OFFLINE——过渡态和故障态都不该被念成在线。
 * 底色与字色都取自主题，这里不写色值（rules.md §10）。
 */
@Composable
fun LiveOfflineBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val isLive = connectionState.isConnected
    Text(
        text =
            stringResource(
                if (isLive) R.string.components_mode_live else R.string.components_mode_offline,
            ),
        style = MaterialTheme.typography.labelLarge,
        color = if (isLive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .clip(RoundedCornerShape(BADGE_CORNER_RADIUS))
                .background(badgeBackground(isLive))
                .padding(horizontal = BADGE_HORIZONTAL_PADDING, vertical = BADGE_VERTICAL_PADDING),
    )
}

@Composable
private fun badgeBackground(isLive: Boolean): Color =
    if (isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant

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

private val BADGE_CORNER_RADIUS = 4.dp
private val BADGE_HORIZONTAL_PADDING = 8.dp
private val BADGE_VERTICAL_PADDING = 2.dp
