package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

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
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 安全设置（plan §47、plan §54、plan §57）。
 *
 * 这一屏目前是说明性的：能配的项都还没有对应的读写端口，因此这里写清现状与约定，
 * 而不是摆一批拨动后没有任何效果的开关。
 */
@Composable
fun SecurityScreen(modifier: Modifier = Modifier) {
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
                Section(
                    headingResource = R.string.settings_security_redaction_heading,
                    content = {
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.settings_security_redaction_headline),
                            detail = stringResource(R.string.settings_security_redaction_body),
                        )
                    },
                )
                Section(
                    headingResource = R.string.settings_security_logging_heading,
                    content = {
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.settings_security_logging_headline),
                            detail = stringResource(R.string.settings_security_logging_body),
                        )
                    },
                )
                Section(
                    headingResource = R.string.settings_security_pairing_heading,
                    content = {
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.settings_security_pairing_headline),
                            detail = stringResource(R.string.settings_security_pairing_body),
                        )
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.settings_security_reason_heading),
                            detail = stringResource(R.string.settings_security_reason),
                        )
                    },
                )
            }
        }
    }
}

@Composable
internal fun Section(
    @StringRes headingResource: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(headingResource))
        content()
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun SecurityScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) { SecurityScreen() }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun SecurityScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) { SecurityScreen() }
}
