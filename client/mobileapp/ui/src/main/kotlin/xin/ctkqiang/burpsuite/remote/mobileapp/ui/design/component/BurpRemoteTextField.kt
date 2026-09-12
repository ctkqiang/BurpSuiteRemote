package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 单行输入框。
 *
 * [isMonospace] 给技术值用：主机、端口、配对码这类内容要能逐字符比对，比例字体做不到这件事。
 */
@Composable
fun BurpRemoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isMonospace: Boolean = false,
    isEnabled: Boolean = true,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val shape = RoundedCornerShape(BurpRemoteRadius.Control)
    val textStyle: TextStyle =
        if (isMonospace) tokens.typography.technical else tokens.typography.body
    val textColour =
        if (isEnabled) tokens.colourScheme.contentPrimary else tokens.colourScheme.contentSecondary

    Column(modifier = Modifier.fillMaxWidth()) {
        BurpRemoteText(
            text = label,
            style = tokens.typography.label,
            colour = tokens.colourScheme.contentSecondary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(BurpRemoteSpacing.Small))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEnabled,
            singleLine = true,
            textStyle = textStyle.copy(color = textColour),
            cursorBrush = SolidColor(tokens.colourScheme.accent),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(color = tokens.colourScheme.surfaceElevated, shape = shape)
                    .border(width = 1.dp, color = tokens.colourScheme.outline, shape = shape),
            decorationBox = { innerTextField ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = BurpRemoteSpacing.Medium,
                                vertical = BurpRemoteSpacing.Medium,
                            ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    innerTextField()
                }
            },
        )
    }
}
