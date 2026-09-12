package xin.ctkqiang.burpsuite.remote.mobileapp.feature.screenshot

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ScreenshotProcessingState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.NotImplementedReasonText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 截图工作区（plan §28–§30）。
 *
 * 这一屏没有 ViewModel：客户端还没有截图端口，没有任何可观察的状态，硬造一个状态持有者只是摆设。
 * 它显示的是真实存在的领域知识——处理阶段清单——以及诚实的空状态。
 */
@Composable
fun ScreenshotScreen(modifier: Modifier = Modifier) {
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
            ScreenHeading(titleResource = R.string.screenshot_title)
            PipelineSection()
            StoredScreenshotsSection()
        }
    }
}

@Composable
private fun PipelineSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.screenshot_pipeline_heading)
        Text(
            text = stringResource(R.string.screenshot_pipeline_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // 阶段名直接来自领域模型，界面不自己维护一份平行的字符串表。
        ScreenshotProcessingState.entries.forEach { processingState ->
            Text(
                text = stringResource(processingState.labelResource),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StoredScreenshotsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.screenshot_stored_heading)
        EmptyStateText(messageResource = R.string.screenshot_stored_empty)
        NotImplementedReasonText(reasonResource = R.string.screenshot_reason_import)
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.screenshot_action_import))
        }
    }
}

@get:StringRes
private val ScreenshotProcessingState.labelResource: Int
    get() =
        when (this) {
            ScreenshotProcessingState.Imported -> R.string.screenshot_stage_imported
            ScreenshotProcessingState.Analyzing -> R.string.screenshot_stage_analyzing
            ScreenshotProcessingState.Detected -> R.string.screenshot_stage_detected
            ScreenshotProcessingState.Beautifying -> R.string.screenshot_stage_beautifying
            ScreenshotProcessingState.Ready -> R.string.screenshot_stage_ready
            ScreenshotProcessingState.Failed -> R.string.screenshot_stage_failed
        }

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun ScreenshotScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) { ScreenshotScreen() }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun ScreenshotScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) { ScreenshotScreen() }
}

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
