package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.labelResource

/**
 * 会话通知：通道、内容，以及那一条「断开」动作。
 *
 * 通道重要性取 LOW 而不是默认档：这是一条常驻的状态条，不是提醒。默认档会让每次状态变化都响一声，
 * 而重连退避时状态会来回变好几次，那就从「告诉你还在连着」变成了骚扰。
 *
 * 状态文字取 :ui 里那一份连接状态文案，与连接屏、重放屏读的是同一套取值；同一件事在三处说法
 * 必须一致，因此不在这里另写一份。
 */
object ConnectionSessionNotification {
    const val IDENTIFIER = 4101

    private const val CHANNEL_IDENTIFIER = "connection_session"

    /** 建通道。重复调用是幂等的，因此每次服务启动都可以直接调。 */
    fun ensureChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_IDENTIFIER,
                context.getString(R.string.connection_session_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
        channel.description = context.getString(R.string.connection_session_channel_description)
        // 常驻状态条不该在图标上堆角标：它一直是 1，没有信息量。
        channel.setShowBadge(false)
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** 造一条通知；[state] 决定副标题写什么。 */
    fun build(
        context: Context,
        state: ConnectionState,
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_IDENTIFIER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.connection_session_title))
            .setContentText(context.getString(state.labelResource))
            .setContentIntent(openApplicationIntent(context))
            .addAction(
                0,
                context.getString(R.string.connection_session_disconnect_action),
                disconnectPendingIntent(context),
            )
            // 常驻：这条通知本身就是「应用还在替你保持这个会话」的凭据，不该被顺手划掉。
            .setOngoing(true)
            // 只有状态真的变了才重画，否则每次刷新都会把通知栏重新拉一下。
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .build()

    /**
     * 把最新状态画到已发出的那条通知上。
     *
     * Android 13 起没有通知权限时这一句是空操作，而不是抛异常；服务本身照常运行，
     * 因此这里不需要额外判断权限，判断了也只是把同一件事写两遍。
     */
    fun publish(
        context: Context,
        state: ConnectionState,
    ) {
        NotificationManagerCompat.from(context).notify(IDENTIFIER, build(context = context, state = state))
    }

    // 点通知回到应用本身；清栈回顶，避免同一次会话被叠成好几层。
    private fun openApplicationIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            context,
            OPEN_APPLICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    // 走 getForegroundService 而不是 getService：从通知栏发起的启动同样受后台启动限制，
    // 声明成前台服务启动才不会被系统拒掉。
    private fun disconnectPendingIntent(context: Context): PendingIntent =
        PendingIntent.getForegroundService(
            context,
            DISCONNECT_REQUEST_CODE,
            ConnectionSessionService.disconnectIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private const val OPEN_APPLICATION_REQUEST_CODE = 1
    private const val DISCONNECT_REQUEST_CODE = 2
}
