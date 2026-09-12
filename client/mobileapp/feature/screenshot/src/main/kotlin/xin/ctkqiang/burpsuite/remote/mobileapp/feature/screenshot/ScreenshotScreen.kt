package xin.ctkqiang.burpsuite.remote.mobileapp.feature.screenshot

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ScreenshotProcessingState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 截图工作区（plan §28–§30）。
 *
 * 这一屏没有 ViewModel：客户端还没有截图端口，没有任何可观察的状态，硬造一个状态持有者只是摆设。
 * 它显示的是真实存在的领域知识——处理阶段清单——以及诚实的空状态：本机没有的东西不摆出来。
 */
@Composable
fun ScreenshotScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        run {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = BurpRemoteSpacing.Large,
                            vertical = BurpRemoteSpacing.Large,
                        ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
            ) {
                PipelineSection()
                StoredScreenshotsSection()
            }
        }
    }
}

@Composable
private fun PipelineSection() {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.screenshot_pipeline_heading))
        BurpRemoteEmptyState(
            headline = stringResource(R.string.screenshot_pipeline_note_headline),
            detail = stringResource(R.string.screenshot_pipeline_note),
        )
        BurpRemoteCard {
            // 阶段名直接来自领域模型，界面不自己维护一份平行的字符串表。
            ScreenshotProcessingState.entries.forEachIndexed { index, processingState ->
                BurpRemoteTechnicalValue(
                    text = (index + 1).toString(),
                    label = stringResource(processingState.labelResource),
                )
            }
        }
    }
}

@Composable
private fun StoredScreenshotsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.screenshot_stored_heading))
        BurpRemoteEmptyState(
            headline = stringResource(R.string.screenshot_stored_empty_headline),
            detail = stringResource(R.string.screenshot_stored_empty),
        )
        BurpRemoteEmptyState(
            headline = stringResource(R.string.screenshot_reason_headline),
            detail = stringResource(R.string.screenshot_reason_import),
        )
        BurpRemoteButton(
            text = stringResource(R.string.screenshot_action_import),
            onClick = {},
            style = BurpRemoteButtonStyle.Secondary,
            isEnabled = false,
        )
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
