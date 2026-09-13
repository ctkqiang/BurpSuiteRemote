// 日志面板里的一行。

package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEntry
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTintedLabel
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日志面板里的一行。
 *
 * 一行回答四件事：什么时候、多严重、属于哪一段、发生了什么。时刻与级别固定在左侧，
 * 纵向扫视时才对得齐；结构化取值排在消息之后并退到次要字色，因为它只在细看时才需要。
 *
 * @param entry 要画的日志；序号与时刻由日志存储在记录时盖好，这里不重新取时钟。
 * @param modifier 由调用方决定摆放。
 */
@Composable
fun BurpRemoteLogLine(
    entry: TechnicalLogEntry,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val colourScheme = tokens.colourScheme
    val event = entry.event

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = BurpRemoteSpacing.Small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BurpRemoteText(
                text = LOG_TIME_FORMATTER.format(entry.recordedAt),
                style = tokens.typography.technical,
                colour = colourScheme.contentSecondary,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(BurpRemoteSpacing.Small))
            BurpRemoteTintedLabel(
                text = stringResource(event.severity.labelResource),
                colour = event.severity.colourIn(colourScheme),
            )
            Spacer(modifier = Modifier.width(BurpRemoteSpacing.Small))
            BurpRemoteText(
                text = stringResource(event.category.labelResource),
                style = tokens.typography.label,
                colour = colourScheme.contentSecondary,
                maxLines = 1,
            )
        }
        BurpRemoteText(
            text = event.message,
            style = tokens.typography.body,
            colour = colourScheme.contentPrimary,
            modifier = Modifier.padding(top = BurpRemoteSpacing.ExtraSmall),
        )
        if (event.attributes.isNotEmpty()) {
            BurpRemoteText(
                text = renderAttributes(entry),
                style = tokens.typography.technical,
                colour = colourScheme.contentSecondary,
                modifier = Modifier.padding(top = BurpRemoteSpacing.ExtraSmall),
            )
        }
    }
}

/**
 * 把结构化取值拼成 `键=值` 的等宽串。
 *
 * 取值本身已经由落地实现在写入前脱敏过（rules.md §12），这里只负责排版，不做二次判断——
 * 两处各判一次，迟早会判得不一样。
 *
 * @param entry 要渲染的日志。
 * @return 一行以空格分隔的键值串。
 */
private fun renderAttributes(entry: TechnicalLogEntry): String =
    entry.event.attributes.entries.joinToString(separator = " ") { attribute ->
        attribute.key + "=" + attribute.value
    }

// 毫秒必须留着：一轮重连里会连着记好几条，只到秒就分不出先后。
private val LOG_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
