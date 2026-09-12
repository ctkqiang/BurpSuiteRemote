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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
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
 */
@Composable
fun LanguageScreen(
    uiState: LanguageUserInterfaceState,
    onIntent: (LanguageUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()

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
                BurpRemoteSectionHeading(text = stringResource(R.string.settings_language_heading))
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.settings_language_headline),
                    detail = stringResource(R.string.settings_language_detail),
                )
                LanguagePreference.entries.forEach { language ->
                    LanguageOption(
                        language = language,
                        isSelected = language == uiState.language,
                        onSelect = {
                            haptics.select()
                            onIntent(LanguageUserInterfaceIntent.SelectLanguage(language))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    language: LanguagePreference,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    BurpRemoteCard(isInteractive = true, onClick = onSelect) {
        BurpRemoteStatusPill(
            text = stringResource(language.labelResource),
            tone = if (isSelected) BurpRemoteStatusTone.Live else BurpRemoteStatusTone.Neutral,
        )
        if (isSelected) {
            BurpRemoteStatusPill(
                text = stringResource(R.string.settings_language_selected),
                tone = BurpRemoteStatusTone.Live,
            )
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
