package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * 统计小部件提供者。
 *
 * 职责只在两件事：
 * 1. [onUpdate] 与 [refresh] 把最新统计快照画到 RemoteViews，再交给 [AppWidgetManager] 推到桌面。
 * 2. 给整张小部件挂一个 [PendingIntent]，点击后由系统替它启动 MainActivity，附带主面板路由。
 *
 * 不在这里订阅 [AppContainer] 的主面板汇总流：小部件进程不一定有主进程在跑，
 * 订阅需要活的作用域，进程被收掉那段时间里小部件就没有人刷新。改由 [AppContainer] 主进程
 * 在自己的进程级作用域里订阅，每次新值落盘 + 调一次 [refresh]。
 *
 * 线程：[onUpdate] 由系统在主线程调，[refresh] 由 [AppContainer] 在 IO 调度器调；
 * [AppWidgetManager.updateAppWidget] 内部是 IPC，线程安全，两边都直接调没问题。
 */
class BurpRemoteStatsWidgetProvider : AppWidgetProvider() {
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
        val views = RemoteViews(applicationContext.packageName, R.layout.widget_burp_remote_stats)

        val store = WidgetDashboardStore(applicationContext)
        val snapshot = store.read()

        // 目标主机：有就显示，没有就给一句「还没有目标」——比留空更清楚。
        views.setTextViewText(
            R.id.widget_stats_target_host,
            snapshot.targetHost ?: applicationContext.getString(R.string.widget_stats_no_target),
        )

        // 三个计数：等宽数字，变化时眼睛能跟上。
        views.setTextViewText(
            R.id.widget_stats_live_count,
            snapshot.liveRequestCount.toString(),
        )
        views.setTextViewText(
            R.id.widget_stats_intercepted_count,
            snapshot.interceptedCount.toString(),
        )
        views.setTextViewText(
            R.id.widget_stats_saved_count,
            snapshot.savedCount.toString(),
        )

        // 整张小部件可点：开主面板，统计是主面板的子集，用户点进来就看完整面板。
        views.setOnClickPendingIntent(
            R.id.widget_stats_open_hint,
            routePendingIntent(applicationContext, "dashboard", REQUEST_CODE_DASHBOARD),
        )

        return views
    }

    /**
     * 构造一个落地到 [route] 的 PendingIntent。
     *
     * FLAG_IMMUTABLE：Android 12 起强制要求未显式声明可变的 PendingIntent 必须是 IMMUTABLE。
     * FLAG_UPDATE_CURRENT：同一个 requestCode 复用时保留已存在的 PendingIntent，只更新其 extra。
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

    companion object {
        // 统计小部件只有一个入口（开主面板），因此只需要一个 requestCode。
        const val REQUEST_CODE_DASHBOARD = 0x0010

        /**
         * 由 [AppContainer] 在写入统计快照后调用：把所有已安装的同款小部件刷一次。
         *
         * 不需要 [Context] 的调用方自己去取 [AppWidgetManager]——
         * 系统对一个 [ComponentName] 上的所有小部件实例统一调度。
         */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component =
                android.content.ComponentName(
                    context.packageName,
                    BurpRemoteStatsWidgetProvider::class.java.name,
                )
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val provider = BurpRemoteStatsWidgetProvider()
            provider.onUpdate(context, manager, ids)
        }
    }
}
