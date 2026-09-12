package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 连接状态指示：一个状态灯加本地化后的状态名（plan §44）。
 *
 * 只展示：状态从领域模型来，这里不改写它，也不触发重连之类的动作。
 */
@Composable
fun ConnectionStateIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(INDICATOR_SIZE)
                    .clip(CircleShape)
                    .background(indicatorColour(connectionState, tokens.colourScheme)),
        )
        Spacer(modifier = Modifier.width(INDICATOR_SPACING))
        BurpRemoteText(
            text = stringResource(connectionState.labelResource),
            style = tokens.typography.body,
            colour = tokens.colourScheme.contentPrimary,
        )
    }
}

// 颜色全部取自主题：全项目只有主题那一处写过色值。
private fun indicatorColour(
    connectionState: ConnectionState,
    colourScheme: BurpRemoteColourScheme,
): Color =
    when {
        connectionState.isConnected -> colourScheme.success
        connectionState == ConnectionState.Disconnected -> colourScheme.contentSecondary
        connectionState in FAILURE_STATES -> colourScheme.danger
        else -> colourScheme.warning
    }

/** 协议版本不符、超时这类状态都算故障，配色统一走危险色。 */
private val FAILURE_STATES: Set<ConnectionState> =
    setOf(
        ConnectionState.AuthenticationFailed,
        ConnectionState.ProtocolError,
        ConnectionState.TimedOut,
        ConnectionState.ServerUnavailable,
        ConnectionState.ResynchronisationRequired,
    )

@get:StringRes
private val ConnectionState.labelResource: Int
    get() =
        when (this) {
            ConnectionState.Disconnected -> R.string.connection_state_disconnected
            ConnectionState.Discovering -> R.string.connection_state_discovering
            ConnectionState.Connecting -> R.string.connection_state_connecting
            ConnectionState.Authenticating -> R.string.connection_state_authenticating
            ConnectionState.Synchronising -> R.string.connection_state_synchronising
            ConnectionState.Connected -> R.string.connection_state_connected
            ConnectionState.Reconnecting -> R.string.connection_state_reconnecting
            ConnectionState.ResynchronisationRequired -> R.string.connection_state_resynchronisation_required
            ConnectionState.AuthenticationFailed -> R.string.connection_state_authentication_failed
            ConnectionState.ProtocolError -> R.string.connection_state_protocol_error
            ConnectionState.TimedOut -> R.string.connection_state_timed_out
            ConnectionState.ServerUnavailable -> R.string.connection_state_server_unavailable
        }

private val INDICATOR_SIZE = 10.dp
private val INDICATOR_SPACING = 8.dp
