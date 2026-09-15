package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 应用外壳：顶栏、内容、浮在内容之上的液态玻璃底栏。
 *
 * 内容铺到系统栏下面，留白由 [WindowInsets] 算出来（edge-to-edge）。整个内容 Column 标记为 Haze
 * 采集域（Modifier.haze），底栏的容器标记为 Haze 玻璃消费者（Modifier.hazeChild）—— Haze 会在
 * 底栏那块区域把内容实时模糊再叠上半透明底色与高光，形成液态玻璃的漂浮感。
 *
 * 内容必须知道底栏占了多高，否则最后一行永远被压在底栏底下——底栏高度在这里量出来再传给内容。
 *
 * [hazeState] 传给底栏容器用 hazeChild(hazeState) 消费；Scaffold 自己创建，不用外部管。
 */
@Composable
fun BurpRemoteScaffold(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable (HazeState) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var bottomBarHeightPixels by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bottomBarHeight: Dp = with(density) { bottomBarHeightPixels.toDp() }
    val hazeState = remember { HazeState() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(tokens.colourScheme.background),
    ) {
        // 内容 Column 是 Haze 的采集域：里面所有东西（顶栏 + NavHost）都能被底栏那块玻璃模糊。
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .haze(hazeState),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.statusBars.asPaddingValues()),
            ) {
                topBar()
            }
            Box(modifier = Modifier.weight(weight = 1f, fill = true).fillMaxWidth()) {
                content(PaddingValues(bottom = bottomBarHeight + navigationBarInset + BurpRemoteSpacing.Small))
            }
        }

        // 底栏区域：内边距只加在手势条那一段上，因此量到的高度就是底栏自身的高度，
        // 不会再和导航栏内边距重复算一次（重复算会让每屏底部多出一段空白）。
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = navigationBarInset)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { measuredSize -> bottomBarHeightPixels = measuredSize.height },
                ) {
                    bottomBar(hazeState)
                }
            }
        }
    }
}
