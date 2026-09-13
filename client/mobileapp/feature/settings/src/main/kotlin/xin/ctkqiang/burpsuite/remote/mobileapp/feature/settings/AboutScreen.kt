package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteBrandMark
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItemGroup
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 关于与开发者信息。
 *
 * 作者、邮箱与仓库地址是**事实**而不是可配置项，因此它们走 `translatable="false"` 的资源，
 * 不进翻译条目：把一个人的名字和邮箱交给翻译既没有意义，还会在某个语言里被拼错。
 *
 * 邮箱与仓库是可点的：这一屏存在的意义就是让人能找过来，把地址画成不可点的纯文本等于把路堵回去。
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val uriHandler = LocalUriHandler.current
    // 取值要先落到局部变量：onClick 是普通回调而不是可组合上下文，在里头读资源编译器会直接拦下。
    val emailValue = stringResource(R.string.about_email_value)
    val sourceValue = stringResource(R.string.about_source_value)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = BurpRemoteSpacing.ScreenEdge,
                    vertical = BurpRemoteSpacing.ExtraLarge,
                ),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraLarge),
    ) {
        BurpRemoteCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
            ) {
                BurpRemoteBrandMark(
                    modifier = Modifier.size(BurpRemoteSizing.StateIcon),
                    colour = tokens.colourScheme.accent,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraSmall),
                ) {
                    BurpRemoteText(
                        text = stringResource(R.string.about_application_name),
                        style = tokens.typography.subtitle,
                        colour = tokens.colourScheme.contentPrimary,
                    )
                    BurpRemoteText(
                        text = stringResource(R.string.about_tagline),
                        style = tokens.typography.caption,
                        colour = tokens.colourScheme.contentSecondary,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
            BurpRemoteSectionHeading(text = stringResource(R.string.about_developer_heading))
            BurpRemoteListItemGroup {
                BurpRemoteListItem(
                    title = stringResource(R.string.about_author_label),
                    subtitle = stringResource(R.string.about_author_value),
                    showsDivider = true,
                )
                BurpRemoteListItem(
                    title = stringResource(R.string.about_email_label),
                    subtitle = emailValue,
                    onClick = { uriHandler.openUri(MAIL_TO_PREFIX + emailValue) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
            BurpRemoteSectionHeading(text = stringResource(R.string.about_source_heading))
            BurpRemoteListItemGroup {
                BurpRemoteListItem(
                    title = stringResource(R.string.about_source_label),
                    subtitle = sourceValue,
                    onClick = { uriHandler.openUri(sourceValue) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
            BurpRemoteSectionHeading(text = stringResource(R.string.about_open_source_heading))
            BurpRemoteListItemGroup {
                BurpRemoteListItem(
                    title = stringResource(R.string.about_open_source_headline),
                    subtitle = stringResource(R.string.about_open_source_body),
                )
            }
        }
    }
}

// mailto: 前缀不进资源：它不是给人读的文字，翻译它只会把链接弄坏。
private const val MAIL_TO_PREFIX = "mailto:"

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun AboutScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) { AboutScreen() }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun AboutScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) { AboutScreen() }
}
