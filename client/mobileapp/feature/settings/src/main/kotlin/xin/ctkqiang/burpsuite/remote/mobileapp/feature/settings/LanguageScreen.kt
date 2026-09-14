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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 语言设置（plan §46）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。换语言要重建界面才生效，那件事由装配层接效果去做
 * （rules.md §8.1：一次性动作走效果，不进状态）。
 *
 * 语言名一律用各自的语言书写：用户不确定「Nederlands 是哪个」时，看到的就是它自己的写法。
 * 行里不再补说明文字 —— 语言名本身已经说完了这一行要说的全部内容，凑一句话只会拉长列表。
 */
@Composable
fun LanguageScreen(
    uiState: LanguageUserInterfaceState,
    onIntent: (LanguageUserInterfaceIntent) -> Unit,
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
        // 换语言会重建界面这件事必须先说：它是这一屏唯一有副作用的动作。
        BurpRemoteListItemGroup {
            BurpRemoteListItem(
                title = stringResource(R.string.settings_language_headline),
                subtitle = stringResource(R.string.settings_language_detail),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
            BurpRemoteSectionHeading(text = stringResource(R.string.settings_language_heading))
            BurpRemoteListItemGroup {
                LanguagePreference.entries.forEachIndexed { index, language ->
                    BurpRemoteListItem(
                        title = stringResource(language.labelResource),
                        trailing = {
                            if (language == uiState.language) {
                                BurpRemoteStatusPill(
                                    text = stringResource(R.string.settings_language_selected),
                                    tone = BurpRemoteStatusTone.Live,
                                )
                            }
                        },
                        showsDivider = index != LanguagePreference.entries.lastIndex,
                        onClick = {
                            haptics.select()
                            onIntent(LanguageUserInterfaceIntent.SelectLanguage(language))
                        },
                    )
                }
            }
        }
    }
}

// 语言名用各自的语言书写：让用户在不确定「Deutsch 是哪个」的时候也能认出来。
@get:StringRes
private val LanguagePreference.labelResource: Int
    get() =
        when (this) {
            LanguagePreference.Automatic -> R.string.settings_language_automatic
            LanguagePreference.English -> R.string.settings_language_english
            LanguagePreference.Chinese -> R.string.settings_language_chinese
            LanguagePreference.German -> R.string.settings_language_german
            LanguagePreference.Japanese -> R.string.settings_language_japanese
            LanguagePreference.Korean -> R.string.settings_language_korean
            LanguagePreference.Thai -> R.string.settings_language_thai
            LanguagePreference.Malay -> R.string.settings_language_malay
            LanguagePreference.Russian -> R.string.settings_language_russian
            LanguagePreference.French -> R.string.settings_language_french
        }

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun LanguageScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        LanguageScreen(uiState = LanguageUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun LanguageScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        LanguageScreen(
            uiState = LanguageUserInterfaceState(LanguagePreference.Chinese),
            onIntent = {},
        )
    }
}
