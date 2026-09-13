package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteDarkColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteLightColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 外观与主题（plan §45、plan §47 的 theme 一项）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。三种主题各自配一块真实预览块——预览用的是该主题
 * 真正的那套配色，而不是三张画出来的示意图，因此选之前看到什么，选之后就得到什么。
 *
 * 每一档都写明它影响什么：主题、语言这类偏好一旦选了，界面立刻按新值重绘，用户得先知道这一点。
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUserInterfaceState,
    onIntent: (SettingsUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()

    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = BurpRemoteSpacing.ScreenEdge,
                        vertical = BurpRemoteSpacing.ScreenEdge,
                    ),
            verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
        ) {
            BurpRemoteSectionHeading(text = stringResource(R.string.settings_theme_heading))
            BurpRemoteEmptyState(
                headline = stringResource(R.string.settings_theme_headline),
                detail = stringResource(R.string.settings_theme_detail),
            )
            ThemeMode.entries.forEach { themeMode ->
                ThemeModeOption(
                    themeMode = themeMode,
                    isSelected = themeMode == uiState.themeMode,
                    onSelect = {
                        haptics.select()
                        onIntent(SettingsUserInterfaceIntent.SelectThemeMode(themeMode))
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    themeMode: ThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    BurpRemoteCard(isInteractive = true, onClick = onSelect) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BurpRemoteStatusPill(
                text = stringResource(themeMode.labelResource),
                tone = if (isSelected) BurpRemoteStatusTone.Live else BurpRemoteStatusTone.Neutral,
            )
            if (isSelected) {
                BurpRemoteStatusPill(
                    text = stringResource(R.string.settings_theme_selected),
                    tone = BurpRemoteStatusTone.Live,
                )
            }
        }
        ThemeModePreview(themeMode = themeMode)
        BurpRemoteEmptyState(
            headline = stringResource(themeMode.headlineResource),
            detail = stringResource(themeMode.detailResource),
        )
    }
}

/** 真实预览：取该主题真正的那套配色画一块缩小的界面，深色档不会拿浅色示意。 */
@Composable
private fun ThemeModePreview(themeMode: ThemeMode) {
    when (themeMode) {
        ThemeMode.Light -> ColourSchemePreview(colourScheme = BurpRemoteLightColourScheme)
        ThemeMode.Dark -> ColourSchemePreview(colourScheme = BurpRemoteDarkColourScheme)
        ThemeMode.Automatic ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            ) {
                Box(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    ColourSchemePreview(colourScheme = BurpRemoteLightColourScheme)
                }
                Box(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    ColourSchemePreview(colourScheme = BurpRemoteDarkColourScheme)
                }
            }
    }
}

// 预览块只画三样：底色、一块表面、一个强调色圆点——配色方案的三个关键档位都在这里，多画就是装饰。
@Composable
private fun ColourSchemePreview(colourScheme: BurpRemoteColourScheme) {
    val shape = RoundedCornerShape(BurpRemoteRadius.Control)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colourScheme.background)
                .border(width = 1.dp, color = colourScheme.outline, shape = shape)
                .padding(BurpRemoteSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(PREVIEW_ACCENT_WIDTH)
                        .height(PREVIEW_ACCENT_HEIGHT)
                        .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                        .background(colourScheme.accent),
            )
            Box(
                modifier =
                    Modifier
                        .width(PREVIEW_TEXT_WIDTH)
                        .height(PREVIEW_TEXT_HEIGHT)
                        .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                        .background(colourScheme.contentSecondary),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(PREVIEW_SURFACE_HEIGHT)
                    .clip(RoundedCornerShape(BurpRemoteRadius.Control))
                    .background(colourScheme.surface),
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
private val ThemeMode.headlineResource: Int
    get() =
        when (this) {
            ThemeMode.Automatic -> R.string.settings_theme_automatic_headline
            ThemeMode.Light -> R.string.settings_theme_light_headline
            ThemeMode.Dark -> R.string.settings_theme_dark_headline
        }

@get:StringRes
private val ThemeMode.detailResource: Int
    get() =
        when (this) {
            ThemeMode.Automatic -> R.string.settings_theme_automatic_detail
            ThemeMode.Light -> R.string.settings_theme_light_detail
            ThemeMode.Dark -> R.string.settings_theme_dark_detail
        }

private val PREVIEW_ACCENT_WIDTH = 28.dp
private val PREVIEW_ACCENT_HEIGHT = 10.dp
private val PREVIEW_TEXT_WIDTH = 56.dp
private val PREVIEW_TEXT_HEIGHT = 10.dp
private val PREVIEW_SURFACE_HEIGHT = 28.dp

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
