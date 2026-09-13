package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.runtime.Composable
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical.colourIn
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 状态胶囊：LIVE / OFFLINE / 2xx 这类一眼判读的短标签。
 *
 * 只负责把语气翻成颜色，再交给 [BurpRemoteTintedLabel] 画。胶囊的画法只有那一处，
 * 这里再多写一遍，两处迟早会漂移成两个样子。
 *
 * @param text 标签文本。
 * @param tone 标签语气。
 */
@Composable
fun BurpRemoteStatusPill(
    text: String,
    tone: BurpRemoteStatusTone,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    BurpRemoteTintedLabel(
        text = text,
        colour = tone.colourIn(tokens.colourScheme),
    )
}
