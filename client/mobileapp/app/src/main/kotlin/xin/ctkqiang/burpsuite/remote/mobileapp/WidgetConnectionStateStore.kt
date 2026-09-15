package xin.ctkqiang.burpsuite.remote.mobileapp

import android.content.Context
import androidx.core.content.edit
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState

/**
 * 桌面小部件读取连接状态的共享偏好桥。
 *
 * 背景：小部件跑在与主进程隔离的 RemoteViews 上下文里，[android.appwidget.AppWidgetManager]
 * 调用 [BurpRemoteAppWidgetProvider.onUpdate] 时，主进程可能根本没起来——也就拿不到
 * [AppContainer] 的 [ConnectionState] 流。要「画一条会变的状态」，主进程就必须把状态
 * 写到一个两者都看得到的地方：进程被收掉以后，最近一次写入仍然在磁盘上，小部件还能画出来。
 *
 * 写入端在 [AppContainer.observeConnectionStateForWidget]：它收集 [AppContainer.connectionState]，
 * 每次新值就写一次偏好，再触发一次 [android.appwidget.AppWidgetManager] 刷新。
 *
 * 读取端在 [BurpRemoteAppWidgetProvider.render]：每次系统或 AppContainer 触发刷新时调一次，
 * 不订阅、不挂作用域——状态只要最新值，不要历史，也不需要线程安全的可观察流。
 *
 * 线程安全：SharedPreferences 的 [android.content.SharedPreferences.edit] 是进程内同步的，
 * 主进程的写入与小部件进程的读取之间通过文件锁保证一致性；不需要再加显式锁。
 *
 * @property applicationContext 由 [Context.applicationContext] 派生，避免持有 Activity 上下文
 *   导致的小部件进程内存泄漏。
 */
class WidgetConnectionStateStore(
    private val applicationContext: Context,
) {
    /**
     * 写入最新连接状态。状态名字取自 [ConnectionState.name]，反过来用 [ConnectionState.entries]
     * 反查，避免在持久层重新定义一份枚举——任何一边新增状态，另一边都自动看见。
     */
    fun write(state: ConnectionState) {
        val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit {
            putString(KEY_CONNECTION_STATE, state.name)
            putLong(KEY_UPDATED_AT_EPOCH_MILLIS, System.currentTimeMillis())
        }
    }

    /**
     * 读最近一次写入的连接状态；从未写入时返回 [ConnectionState.Disconnected]，
     * 而不是 null：小部件第一次被加到桌面时主进程可能还没起，给一条已知态比给「未知」
     * 更有用，而且「断开」与「未知」对用户而言是同一回事——都不在用。
     */
    fun read(): ConnectionState {
        val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val storedName = preferences.getString(KEY_CONNECTION_STATE, null)
        return ConnectionState.entries.firstOrNull { it.name == storedName }
            ?: ConnectionState.Disconnected
    }

    private companion object {
        const val PREFERENCES_NAME = "burp_remote_widget"
        const val KEY_CONNECTION_STATE = "connection_state"
        const val KEY_UPDATED_AT_EPOCH_MILLIS = "updated_at_epoch_millis"
    }
}
