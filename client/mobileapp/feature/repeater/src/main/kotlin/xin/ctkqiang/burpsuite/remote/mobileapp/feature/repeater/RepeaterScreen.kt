package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ConnectionStateIndicator
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LiveOfflineBadge
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.NotImplementedReasonText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 重放（plan §43 的 Live/Repeater）。
 *
 * 这一屏没有意图：客户端还没有重放请求的端口，也没有执行命令的端口，因此新建与执行两个按钮是禁用的，
 * 界面上也找不到一个点了没反应的地方。
 */
@Composable
fun RepeaterScreen(
    uiState: RepeaterUserInterfaceState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            ScreenHeading(titleResource = R.string.repeater_title)
            ConnectionCard(connectionState = uiState.connectionState)
            RequestListSection()
            ExecutionSection()
        }
    }
}

@Composable
private fun ConnectionCard(connectionState: ConnectionState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveOfflineBadge(connectionState = connectionState)
            Spacer(modifier = Modifier.width(ROW_SPACING))
            ConnectionStateIndicator(connectionState = connectionState)
        }
    }
}

@Composable
private fun RequestListSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.repeater_request_list_heading)
        EmptyStateText(messageResource = R.string.repeater_request_list_empty)
        NotImplementedReasonText(reasonResource = R.string.repeater_reason_request_list)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.repeater_action_create))
        }
    }
}

@Composable
private fun ExecutionSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.repeater_execution_heading)
        EmptyStateText(messageResource = R.string.repeater_execution_empty)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.repeater_action_execute))
        }
        NotImplementedReasonText(reasonResource = R.string.repeater_reason_execute)
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "在线浅色", showBackground = true)
@Composable
private fun RepeaterScreenLiveLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        RepeaterScreen(uiState = RepeaterUserInterfaceState(connectionState = ConnectionState.Connected))
    }
}

@Preview(name = "在线深色", showBackground = true)
@Composable
private fun RepeaterScreenLiveDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        RepeaterScreen(uiState = RepeaterUserInterfaceState(connectionState = ConnectionState.Connected))
    }
}

@Preview(name = "离线浅色", showBackground = true)
@Composable
private fun RepeaterScreenOfflineLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        RepeaterScreen(uiState = RepeaterUserInterfaceState())
    }
}

@Preview(name = "离线深色", showBackground = true)
@Composable
private fun RepeaterScreenOfflineDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        RepeaterScreen(uiState = RepeaterUserInterfaceState())
    }
}

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
private val CARD_PADDING = 12.dp
