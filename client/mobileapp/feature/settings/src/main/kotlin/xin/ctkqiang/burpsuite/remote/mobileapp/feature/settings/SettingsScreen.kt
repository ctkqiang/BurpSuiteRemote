package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeFlavor
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItemGroup
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteFlavourPalettes
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 外观与主题（plan §45、plan §47 的 theme 一项）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。
 *
 * 分两段：
 * 1. 明暗（Automatic / Light / Dark）—— 沿用原来的列表行 + 色块。
 * 2. 口味（八套配色）—— 用两列网格，每块展示该口味的浅色与深色各一半，
 *    点一下即切换，选中项画一圈强调色描边。
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
                                ThemeSwatch(
                                    themeMode = themeMode,
                                    themeFlavor = uiState.themeFlavor,
                                )
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

        Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
            BurpRemoteSectionHeading(text = stringResource(R.string.settings_flavor_heading))
            BurpRemoteListItemGroup {
                BurpRemoteListItem(
                    title = stringResource(R.string.settings_flavor_headline),
                    subtitle = stringResource(R.string.settings_flavor_detail),
                )
            }
            FlavorGrid(
                selectedFlavor = uiState.themeFlavor,
                onSelect = { flavor ->
                    haptics.select()
                    onIntent(SettingsUserInterfaceIntent.SelectThemeFlavor(flavor))
                },
            )
        }
    }
}

/**
 * 八套口味的两列网格。
 *
 * 每项是一块卡片：上半浅色、下半深色，中间一道细分割线，让用户一次看清这套口味在两种明暗下的样子。
 * 选中项用强调色描边 + 右上角小对勾；未选中只用主题描边。
 */
@Composable
private fun FlavorGrid(
    selectedFlavor: ThemeFlavor,
    onSelect: (ThemeFlavor) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
        maxItemsInEachRow = FLAVOR_GRID_COLUMNS,
    ) {
        ThemeFlavor.entries.forEach { flavor ->
            val isSelected = flavor == selectedFlavor
            FlavorCard(
                flavor = flavor,
                isSelected = isSelected,
                onClick = { onSelect(flavor) },
            )
        }
    }
}

@Composable
private fun FlavorCard(
    flavor: ThemeFlavor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val lightScheme = BurpRemoteFlavourPalettes.schemeFor(flavor, isDark = false)
    val darkScheme = BurpRemoteFlavourPalettes.schemeFor(flavor, isDark = true)
    val shape = RoundedCornerShape(BurpRemoteRadius.Card)
    val borderColour = if (isSelected) tokens.colourScheme.accent else tokens.colourScheme.outline

    Column(
        modifier =
            Modifier
                .fillMaxWidth(0.5f)
                .aspectRatio(FLAVOR_CARD_ASPECT_RATIO)
                .clip(shape)
                .border(
                    width = if (isSelected) BurpRemoteSizingSelected else BurpRemoteSizingDefault,
                    color = borderColour,
                    shape = shape,
                )
                .background(tokens.colourScheme.surface, shape)
                .clickableNoRipple(onClick),
    ) {
        // 浅色半区
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(lightScheme.background),
        ) {
            FlavorCardAccentDot(
                accent = lightScheme.accent,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        // 分割线
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(tokens.colourScheme.outline.copy(alpha = 0.5f)),
        )
        // 深色半区
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(darkScheme.background),
        ) {
            FlavorCardAccentDot(
                accent = darkScheme.accent,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun FlavorCardAccentDot(
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(FLAVOR_DOT_SIZE)
                .clip(RoundedCornerShape(50))
                .background(accent),
    )
}

/**
 * 主题色块：把一套配色缩成一小块。
 *
 * 只画三样——底色、强调色、次要字色。这三样正好是「这套主题长什么样」的全部结论，
 * 多画一项就变成装饰，而装饰在小尺寸上只会糊成一团。
 *
 * 「跟随系统」没法用一块色块表示两种可能，因此画的是**此刻实际会生效**的那一套：
 * 系统现在是深色就画深色。看到的仍然是真实结果，而不是两个半块的折中。
 *
 * 色块用当前选中的口味（[themeFlavor]）的配色，而不是固定经典橙——这样明暗切换和口味切换
 * 在预览里是联动的，用户看到的就是选完之后实际得到的。
 */
@Composable
private fun ThemeSwatch(
    themeMode: ThemeMode,
    themeFlavor: ThemeFlavor,
) {
    val isDark =
        when (themeMode) {
            ThemeMode.Automatic -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    val colourScheme = BurpRemoteFlavourPalettes.schemeFor(themeFlavor, isDark)
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

private const val FLAVOR_GRID_COLUMNS = 2
private const val FLAVOR_CARD_ASPECT_RATIO = 1.4f
private val FLAVOR_DOT_SIZE = 12.dp
private val BurpRemoteSizingSelected = 2.dp
private val BurpRemoteSizingDefault = 1.dp

/** 无涟漪点击：口味卡片是色块，涟漪会糊掉展示效果。 */
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )

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
