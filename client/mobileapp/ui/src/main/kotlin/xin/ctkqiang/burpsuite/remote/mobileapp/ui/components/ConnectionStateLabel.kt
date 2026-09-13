package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R

/**
 * 连接状态对应的用户可见文案。
 *
 * 单独成一个文件而不是留在 [ConnectionStateIndicator] 里，是因为用它的地方不止那一处：
 * 协议里每多一个状态、界面上就多一个地方要把它念对，散在各个文件里迟早会有一处漏改。
 *
 * 取值刻意写全而不是留一个 `else`：插件将来加了状态，编译器会在这里拦下来。
 */
@get:StringRes
val ConnectionState.labelResource: Int
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
