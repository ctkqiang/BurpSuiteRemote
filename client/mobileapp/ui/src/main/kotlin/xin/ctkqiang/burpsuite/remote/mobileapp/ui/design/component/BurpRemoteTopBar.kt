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
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 顶栏：标题、可选副标题、可选返回、以及调用方给的右侧动作。
 *
 * 全应用只有外壳持有它，因此这里把「不变」写死：固定高度、固定内边距、固定分割线。
 * 高度若跟着副标题有无而变，正文就会在切页时整体上下跳一次——这正是「顶栏一直在跳」的成因。
 *
 * 副标题留给「主机:端口」这类上下文，赏金猎人一眼就知道自己在看哪台机器；没有上下文时留空，不占额外高度。
 */
@Composable
fun BurpRemoteTopBar(
    title: String,
    subtitle: String?,
    onNavigateBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(TOP_BAR_HEIGHT)
                    .padding(horizontal = BurpRemoteSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onNavigateBack != null) {
                Image(
                    painter = rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack),
                    contentDescription = stringResource(R.string.components_navigate_back),
                    colorFilter = ColorFilter.tint(tokens.colourScheme.contentPrimary),
                    modifier =
                        Modifier
                            .size(BACK_ICON_SIZE)
                            .clickable {
                                haptics.tap()
                                onNavigateBack()
                            },
                )
                Spacer(modifier = Modifier.width(BurpRemoteSpacing.Medium))
            }

            Column(
                modifier = Modifier.weight(weight = 1f, fill = true),
                verticalArrangement = Arrangement.Center,
            ) {
                BurpRemoteText(
                    text = title,
                    style = tokens.typography.display,
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
                } else {
                    // 副标题那一行无论有没有内容都占着：不占的话标题会在有副标题的页面里下移半行，
                    // 切页时肉眼可见地跳一下。
                    BurpRemoteText(
                        text = "",
                        style = tokens.typography.technical,
                        colour = tokens.colourScheme.contentSecondary,
                        maxLines = 1,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }

        // 分割线恒定存在：它一旦跟着页面出现或消失，滚动时顶栏下沿就会闪。
        Box(modifier = Modifier.fillMaxWidth().height(DIVIDER_HEIGHT).background(tokens.colourScheme.outline))
    }
}

private val TOP_BAR_HEIGHT = 72.dp
private val BACK_ICON_SIZE = 24.dp
private val DIVIDER_HEIGHT = 1.dp
