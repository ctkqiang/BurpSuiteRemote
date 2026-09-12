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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 应用外壳：顶栏、内容、浮在内容之上的底栏。
 *
 * 内容铺到系统栏下面，留白由 [WindowInsets] 算出来（edge-to-edge）；底栏浮在内容之上，
 * 因此内容必须知道底栏占了多高，否则最后一行永远被压在底栏底下——底栏高度在这里量出来再传给内容。
 *
 * 底栏那一条区域只填页面底色，不画渐变：渐变在深色下会在手势条上方留出一条比底栏浅的色带，
 * 而底栏本身是玻璃质感，两者对不上。整块区域填底色之后，手势条那一段与底栏视觉上就是连续的。
 */
@Composable
fun BurpRemoteScaffold(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var bottomBarHeightPixels by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bottomBarHeight: Dp = with(density) { bottomBarHeightPixels.toDp() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(tokens.colourScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    .fillMaxWidth()
                    .background(tokens.colourScheme.background),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = navigationBarInset)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { measuredSize -> bottomBarHeightPixels = measuredSize.height },
                ) {
                    bottomBar()
                }
            }
        }
    }
}
