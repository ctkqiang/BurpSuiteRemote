package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItemGroup
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 安全设置（plan §47、plan §54、plan §57）。
 *
 * 这一屏陈述的是「客户端拒绝做什么」，不是一批可拨动的开关：能配的项都还没有对应的读写端口，
 * 因此这里把约定写成一段段可以逐条读的说明，而不是摆一批拨动后没有任何效果的控件。
 *
 * 每条陈述都是一行列表项——短句在上、解释在下。原先每条都居中摆一大块空状态，一屏读下来是四堵
 * 居中的文字墙，眼睛没有可以落的对齐线；现在整屏共用一套左对齐的行网格。
 */
@Composable
fun SecurityScreen(modifier: Modifier = Modifier) {
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
        Section(
            headingResource = R.string.settings_security_redaction_heading,
            titleResource = R.string.settings_security_redaction_headline,
            detailResource = R.string.settings_security_redaction_body,
        )
        Section(
            headingResource = R.string.settings_security_logging_heading,
            titleResource = R.string.settings_security_logging_headline,
            detailResource = R.string.settings_security_logging_body,
        )
        Section(
            headingResource = R.string.settings_security_pairing_heading,
            titleResource = R.string.settings_security_pairing_headline,
            detailResource = R.string.settings_security_pairing_body,
        )
        // 末段讲的是缺口而不是约定，因此不套分区标题：多一个标题会让它看起来像一个可配的分区。
        Section(
            titleResource = R.string.settings_security_reason_heading,
            detailResource = R.string.settings_security_reason,
        )
    }
}

/**
 * 一节说明：可选的分区标题，加一行列表项。
 *
 * 标题与行分开是有意的：标题回答「这是哪一类」，行回答「这一类里具体是什么」。两者合成一句
 * 会变成又长又平的段落，扫读时找不回层级。
 *
 * @param titleResource 这一条陈述本身。
 * @param detailResource 这条陈述的解释。
 * @param headingResource 可选的分区标题；为空时这一节直接以列表项出现。
 */
@Composable
internal fun Section(
    @StringRes titleResource: Int,
    @StringRes detailResource: Int,
    @StringRes headingResource: Int? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        if (headingResource != null) {
            BurpRemoteSectionHeading(text = stringResource(headingResource))
        }
        BurpRemoteListItemGroup {
            BurpRemoteListItem(
                title = stringResource(titleResource),
                subtitle = stringResource(detailResource),
            )
        }
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
