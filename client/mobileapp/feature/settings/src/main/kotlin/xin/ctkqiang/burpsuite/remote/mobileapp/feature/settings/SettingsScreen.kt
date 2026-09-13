package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItemGroup
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteDarkColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteLightColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 外观与主题（plan §45、plan §47 的 theme 一项）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。
 *
 * 每个主题是一行列表项，右侧挂一小块**该主题真实的那套配色**：选之前看到什么，选之后就得到什么。
 * 原先一个主题一张卡、卡里再摆一块大预览加一段居中空状态，一屏读下来是三张厚卡与三堵居中的字；
 * 现在三种主题在同一张卡的三行里并排可比，色块放在行尾，眼睛一次就能扫完。
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUserInterfaceState,
    onIntent: (SettingsUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()

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
        // 说明也走列表行：居中摆一大段话会让人找不到对齐线，而且它和下面的选项不是同一层级。
        BurpRemoteListItemGroup {
            BurpRemoteListItem(
                title = stringResource(R.string.settings_theme_headline),
                subtitle = stringResource(R.string.settings_theme_detail),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
            BurpRemoteSectionHeading(text = stringResource(R.string.settings_theme_heading))
            BurpRemoteListItemGroup {
                ThemeMode.entries.forEachIndexed { index, themeMode ->
                    BurpRemoteListItem(
                        title = stringResource(themeMode.labelResource),
                        subtitle = stringResource(themeMode.detailResource),
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ThemeSwatch(themeMode = themeMode)
                                if (themeMode == uiState.themeMode) {
                                    BurpRemoteStatusPill(
                                        text = stringResource(R.string.settings_theme_selected),
                                        tone = BurpRemoteStatusTone.Live,
                                    )
                                }
                            }
                        },
                        showsDivider = index != ThemeMode.entries.lastIndex,
                        onClick = {
                            haptics.select()
                            onIntent(SettingsUserInterfaceIntent.SelectThemeMode(themeMode))
                        },
                    )
                }
            }
        }
    }
}

/**
 * 主题色块：把一套配色缩成一小块。
 *
 * 只画三样——底色、强调色、次要字色。这三样正好是「这套主题长什么样」的全部结论，
 * 多画一项就变成装饰，而装饰在小尺寸上只会糊成一团。
 *
 * 「跟随系统」没法用一块色块表示两种可能，因此画的是**此刻实际会生效**的那一套：
 * 系统现在是深色就画深色。看到的仍然是真实结果，而不是两个半块的折中。
 */
@Composable
private fun ThemeSwatch(themeMode: ThemeMode) {
    val colourScheme =
        when (themeMode) {
            ThemeMode.Automatic ->
                if (isSystemInDarkTheme()) BurpRemoteDarkColourScheme else BurpRemoteLightColourScheme

            ThemeMode.Light -> BurpRemoteLightColourScheme
            ThemeMode.Dark -> BurpRemoteDarkColourScheme
        }
    val shape = RoundedCornerShape(BurpRemoteRadius.Control)

    Row(
        modifier =
            Modifier
                .size(width = SWATCH_WIDTH, height = SWATCH_HEIGHT)
                .clip(shape)
                .background(color = colourScheme.background, shape = shape)
                .border(width = 1.dp, color = colourScheme.outline, shape = shape)
                .padding(BurpRemoteSpacing.ExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(SWATCH_ACCENT_WIDTH)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                    .background(colourScheme.accent),
        )
        Box(
            modifier =
                Modifier
                    .width(SWATCH_TEXT_WIDTH)
                    .height(SWATCH_TEXT_HEIGHT)
                    .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                    .background(colourScheme.contentSecondary),
        )
    }
}

@get:StringRes
private val ThemeMode.labelResource: Int
    get() =
        when (this) {
            ThemeMode.Automatic -> R.string.settings_theme_automatic
            ThemeMode.Light -> R.string.settings_theme_light
            ThemeMode.Dark -> R.string.settings_theme_dark
        }

@get:StringRes
private val ThemeMode.detailResource: Int
    get() =
        when (this) {
            ThemeMode.Automatic -> R.string.settings_theme_automatic_detail
            ThemeMode.Light -> R.string.settings_theme_light_detail
            ThemeMode.Dark -> R.string.settings_theme_dark_detail
        }

private val SWATCH_WIDTH = 44.dp
private val SWATCH_HEIGHT = 26.dp
private val SWATCH_ACCENT_WIDTH = 8.dp
private val SWATCH_TEXT_WIDTH = 16.dp
private val SWATCH_TEXT_HEIGHT = 6.dp

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        SettingsScreen(uiState = SettingsUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        SettingsScreen(uiState = SettingsUserInterfaceState(ThemeMode.Dark), onIntent = {})
    }
}
