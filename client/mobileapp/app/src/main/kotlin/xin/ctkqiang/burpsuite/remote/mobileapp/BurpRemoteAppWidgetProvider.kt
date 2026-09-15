package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState

/**
 * 桌面小部件提供者。
 *
 * 职责只在两件事：
 * 1. [onUpdate] 与 [refresh] 把最新状态画到 RemoteViews，再交给 [AppWidgetManager] 推到桌面。
 * 2. 给四个按钮各挂一个 [PendingIntent]，点击后由系统替它启动 MainActivity，附带路由额外参数。
 *
 * 不在这里订阅 [AppContainer.connectionState]：小部件进程不一定有主进程在跑，
 * 订阅需要活的作用域，进程被收掉那段时间里小部件就没有人刷新。改由 [AppContainer] 主进程
 * 在自己的进程级作用域里订阅，每次新值落盘 + 调一次 [refresh]，进程被杀时小部件显示
 * 最近一次写入的状态，等下次系统调度或用户点开应用时再续上。
 *
 * 线程：[onUpdate] 由系统在主线程调，[refresh] 由 [AppContainer] 在 Default 调度器调；
 * [AppWidgetManager.updateAppWidget] 内部是 IPC，线程安全，两边都直接调没问题。
 */
class BurpRemoteAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { identifier ->
            appWidgetManager.updateAppWidget(identifier, render(context))
        }
    }

    private fun render(context: Context): RemoteViews {
        val applicationContext = context.applicationContext
        val views = RemoteViews(applicationContext.packageName, R.layout.widget_burp_remote)

        val stateStore = WidgetConnectionStateStore(applicationContext)
        val state = stateStore.read()

        // 状态文本：每种 ConnectionState 对应一段短语，长不超过 12 字符，免得顶栏把它截掉。
        val stateTextRes = labelResourceFor(state)
        views.setTextViewText(
            R.id.widget_connection_state,
            applicationContext.getString(stateTextRes),
        )

        // 标题颜色在已连上时提一档亮度：状态在变化时用户的眼睛先扫到顶栏右侧，亮度差比色相差更容易看见。
        // 直接用 state.isConnected 会在 K2 编译期里把 enum 的扩展属性当成未解析符号；改成显式比较稳。
        val isLive = state == ConnectionState.Connected
        val stateTextColor = if (isLive) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt()
        views.setTextColor(R.id.widget_connection_state, stateTextColor)

        // 四个按钮：每个挂一个 PendingIntent，extra 带路由，MainActivity 收下后按白名单落地。
        views.setOnClickPendingIntent(
            R.id.widget_button_dashboard,
            routePendingIntent(applicationContext, "dashboard", REQUEST_CODE_DASHBOARD),
        )
        views.setOnClickPendingIntent(
            R.id.widget_button_history,
            routePendingIntent(applicationContext, "live/history", REQUEST_CODE_HISTORY),
        )
        views.setOnClickPendingIntent(
            R.id.widget_button_intercept,
            routePendingIntent(applicationContext, "live/intercept", REQUEST_CODE_INTERCEPT),
        )
        views.setOnClickPendingIntent(
            R.id.widget_button_repeater,
            routePendingIntent(applicationContext, "live/repeater", REQUEST_CODE_REPEATER),
        )

        return views
    }

    /**
     * 构造一个落地到 [route] 的 PendingIntent。
     *
     * FLAG_IMMUTABLE：Android 12 起强制要求未显式声明可变的 PendingIntent 必须是 IMMUTABLE，
     * 否则抛 BadParcelableException。这里不需要后续修改额外参数，因此选 IMMUTABLE 最稳。
     * FLAG_UPDATE_CURRENT：同一个 requestCode 复用时保留已存在的 PendingIntent，只更新其 extra，
     * 因此小部件被刷新时不会出现旧 PendingIntent 仍带旧路由的情况。
     */
    private fun routePendingIntent(
        context: Context,
        route: String,
        requestCode: Int,
    ): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BurpRemoteIntents.EXTRA_START_ROUTE, route)
            }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    @StringRes
    private fun labelResourceFor(state: ConnectionState): Int =
        when (state) {
            ConnectionState.Disconnected -> R.string.widget_status_offline
            ConnectionState.Discovering -> R.string.widget_status_discovering
            ConnectionState.Connecting -> R.string.widget_status_connecting
            ConnectionState.Authenticating -> R.string.widget_status_authenticating
            ConnectionState.Synchronising -> R.string.widget_status_synchronising
            ConnectionState.Connected -> R.string.widget_status_live
            ConnectionState.Reconnecting -> R.string.widget_status_reconnecting
            ConnectionState.ResynchronisationRequired -> R.string.widget_status_resyncing
            ConnectionState.AuthenticationFailed -> R.string.widget_status_auth_failed
            ConnectionState.ProtocolError -> R.string.widget_status_protocol_error
            ConnectionState.TimedOut -> R.string.widget_status_timed_out
            ConnectionState.ServerUnavailable -> R.string.widget_status_unreachable
        }

    companion object {
        // 四个按钮各用一个固定的 requestCode：复用同一个会让最后一次 setOnClickPendingIntent
        // 把前面那几个全顶掉（同一个 requestCode 在系统里只对应一个 PendingIntent）。
        const val REQUEST_CODE_DASHBOARD = 0x0001
        const val REQUEST_CODE_HISTORY = 0x0002
        const val REQUEST_CODE_INTERCEPT = 0x0003
        const val REQUEST_CODE_REPEATER = 0x0004

        /**
         * 由 [AppContainer] 在写入连接状态后调用：把所有已安装的同款小部件刷一次。
         *
         * 不需要 [Context] 的调用方自己去取 [AppWidgetManager]——
         * 系统对一个 [ComponentName] 上的所有小部件实例统一调度。
         */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component =
                android.content.ComponentName(
                    context.packageName,
                    BurpRemoteAppWidgetProvider::class.java.name,
                )
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val provider = BurpRemoteAppWidgetProvider()
            provider.onUpdate(context, manager, ids)
        }
    }
}
