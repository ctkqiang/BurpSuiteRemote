package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 顶栏：标题、可选副标题、可选返回、可选品牌标记，以及调用方给的右侧动作。
 *
 * 全应用只有外壳持有它，因此这里把「不变」写死：固定 60dp 高度、固定内边距、固定分割线。
 * 高度若跟着副标题有无而变，正文就会在切页时整体上下跳一次。
 *
 * 副标题留给「主机:端口」这类上下文，赏金猎人一眼就知道自己在看哪台机器；没有上下文时留空。
 * 左右内边距不对称：图标一侧 12dp、标题一侧 20dp，图标因此不会把标题挤离版心。
 *
 * [showsBrandMark] 只在一级目的地置真：品牌标记和返回箭头是同一块位置上的两件事，
 * 同时出现既挤又让人分不清哪个能点。它占的宽度与返回箭头那一档一致，标题在两种页面里都不会跳。
 *
 * @param title 当前目的地的名字。
 * @param subtitle 一行上下文；没有时留空。
 * @param onNavigateBack 返回行为；为 null 时不画返回箭头。
 * @param showsBrandMark 是否在标题左侧画品牌标记；一级目的地用它，子页面用返回箭头。
 * @param actions 右侧动作，由外壳按目的地固定。
 */
@Composable
fun BurpRemoteTopBar(
    title: String,
    subtitle: String?,
    onNavigateBack: (() -> Unit)?,
    showsBrandMark: Boolean = false,
    actions: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    // 图标侧的内边距比标题侧小一档，两者的差就是标题在无返回箭头时的补偿量。
    val titleLeadingInset = BurpRemoteSizing.TopBarTitlePadding - BurpRemoteSizing.TopBarIconPadding

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(BurpRemoteSizing.TopBarHeight)
                    .padding(horizontal = BurpRemoteSizing.TopBarIconPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                onNavigateBack != null ->
                    BackAction(
                        onNavigateBack = onNavigateBack,
                        colourFilter = ColorFilter.tint(tokens.colourScheme.contentPrimary),
                        onTap = haptics::tap,
                    )

                showsBrandMark -> {
                    BurpRemoteBrandMark(
                        modifier = Modifier.size(BurpRemoteSizing.Icon),
                        colour = tokens.colourScheme.accent,
                    )
                    Spacer(modifier = Modifier.width(BurpRemoteSpacing.Small))
                }

                // 没有返回箭头也没有标记时补上这一档，标题正好落在 20dp 的版心线上。
                else -> Spacer(modifier = Modifier.width(titleLeadingInset))
            }

            Column(
                modifier = Modifier.weight(weight = 1f, fill = true),
                verticalArrangement = Arrangement.Center,
            ) {
                BurpRemoteText(
                    text = title,
                    style = tokens.typography.title,
                    colour = tokens.colourScheme.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    BurpRemoteText(
                        text = subtitle,
                        style = tokens.typography.technical,
                        colour = tokens.colourScheme.contentSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }

        // 分割线恒定存在：它一旦跟着页面出现或消失，滚动时顶栏下沿就会闪。
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(BurpRemoteSizing.Divider)
                    .background(tokens.colourScheme.outline),
        )
    }
}

// 图标 24dp、触控区 48dp：图标能画小，手指不能。
@Composable
private fun BackAction(
    onNavigateBack: () -> Unit,
    colourFilter: ColorFilter,
    onTap: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(BurpRemoteSizing.MinimumTouchTarget)
                .clickable {
                    onTap()
                    onNavigateBack()
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        Image(
            painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
            contentDescription = stringResource(R.string.components_navigate_back),
            colorFilter = colourFilter,
            modifier = Modifier.size(BurpRemoteSizing.Icon),
        )
    }
}
