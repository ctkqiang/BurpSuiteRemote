package xin.ctkqiang.burpsuite.remote.mobileapp

import android.content.Context
import androidx.core.content.edit
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.DashboardSummary

/**
 * 桌面小部件读取主面板统计的共享偏好桥。
 *
 * 与 [WidgetConnectionStateStore] 同源：小部件进程可能没有主进程在跑，统计数据要由主进程
 * 落盘，小部件刷新时只拿最近一次写入的值，不订阅、不挂作用域——统计只要最新快照，不要历史。
 *
 * 写入端在 [AppContainer.observeDashboardSummaryForWidget]：它收集
 * [xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository.observeDashboardSummary]，
 * 每次新值就写一次偏好，再触发一次 [android.appwidget.AppWidgetManager] 刷新。
 *
 * 读取端在 [BurpRemoteStatsWidgetProvider.render]：每次刷新时调一次。
 *
 * @property applicationContext 由 [Context.applicationContext] 派生，避免持有 Activity 上下文
 *   导致的小部件进程内存泄漏。
 */
class WidgetDashboardStore(
    private val applicationContext: Context,
) {
    /**
     * 写入最新主面板统计快照。
     *
     * targetHost 可能为 null（还没有任何历史记录），此时写空串；读端用 null 而不是空串，
     * 这样界面能区分「没目标主机」和「目标主机就是空串」——虽然后者不可能出现，但区分清楚更稳。
     */
    fun write(summary: DashboardSummary) {
        val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.edit {
            putString(KEY_TARGET_HOST, summary.targetHost ?: "")
            putInt(KEY_LIVE_REQUEST_COUNT, summary.liveRequestCount)
            putInt(KEY_INTERCEPTED_COUNT, summary.interceptedCount)
            putInt(KEY_SAVED_COUNT, summary.savedCount)
            putLong(KEY_UPDATED_AT_EPOCH_MILLIS, System.currentTimeMillis())
        }
    }

    /**
     * 读最近一次写入的统计快照；从未写入时返回全零 + null 主机。
     *
     * 小部件第一次被加到桌面时主进程可能还没起，给全零比给「未知」更有用——至少告诉用户
     * 「现在一条都没有」，而不是「我不知道有多少」。
     */
    fun read(): WidgetDashboardSnapshot {
        val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val rawTargetHost = preferences.getString(KEY_TARGET_HOST, null)
        return WidgetDashboardSnapshot(
            targetHost = rawTargetHost?.ifBlank { null },
            liveRequestCount = preferences.getInt(KEY_LIVE_REQUEST_COUNT, 0),
            interceptedCount = preferences.getInt(KEY_INTERCEPTED_COUNT, 0),
            savedCount = preferences.getInt(KEY_SAVED_COUNT, 0),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "burp_remote_widget_dashboard"
        const val KEY_TARGET_HOST = "target_host"
        const val KEY_LIVE_REQUEST_COUNT = "live_request_count"
        const val KEY_INTERCEPTED_COUNT = "intercepted_count"
        const val KEY_SAVED_COUNT = "saved_count"
        const val KEY_UPDATED_AT_EPOCH_MILLIS = "updated_at_epoch_millis"
    }
}

/**
 * 小部件读到的统计快照。
 *
 * 与 [DashboardSummary] 不同：它不带 connectionState（连接状态由另一个 store 管），
 * 也不带额外的语义——只是一份可空主机 + 三个计数，正好够统计小部件画三行。
 */
data class WidgetDashboardSnapshot(
    val targetHost: String?,
    val liveRequestCount: Int,
    val interceptedCount: Int,
    val savedCount: Int,
)
