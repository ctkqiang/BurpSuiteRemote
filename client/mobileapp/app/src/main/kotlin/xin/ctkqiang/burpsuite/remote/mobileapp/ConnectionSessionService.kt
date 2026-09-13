package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState

/**
 * 会话前台服务：会话活着的时候挂一条常驻通知，用户把应用划掉时把会话收干净。
 *
 * 为什么非要一个前台服务：Android 8 起后台进程里的普通通知会被系统压掉，而 14 起
 * `setOngoing(true)` 也不再阻止用户划除。想让它「常驻且真的在跑」只有前台服务这一条路。
 *
 * 为什么这里不自己建连接：连接挂在装配容器的进程级作用域上，转屏与返回都不该把它掐断。
 * 这一层只做两件事——把「还在连着」说出来，以及在用户划掉应用时收尾。
 */
class ConnectionSessionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObservation: Job? = null

    // 只通过前台通知与外界交互，不需要绑定入口。
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val container = (application as RemoteControlApplication).container
        // 通道必须先存在，否则 26 起那条通知会被系统直接丢掉；建通道是幂等的，重复调没有代价。
        ConnectionSessionNotification.ensureChannel(this)
        // 前台服务被启动后五秒内必须进入前台，因此无论走哪条路都先满足这条契约再分派动作。
        startForeground(
            ConnectionSessionNotification.IDENTIFIER,
            ConnectionSessionNotification.build(context = this, state = stateOf(intent)),
        )

        if (intent?.action == ACTION_DISCONNECT) {
            container.endSession()
            stopSelf()
            return START_NOT_STICKY
        }

        observeConnectionState(container)
        // 不用 START_STICKY：会话是有起点的，被系统重启出一个没有会话的服务只会挂一条假通知。
        return START_NOT_STICKY
    }

    /**
     * 用户把应用从最近任务里划掉：会话到此为止，通知一并收掉。
     *
     * 连接挂在进程级作用域上，进程不会因为任务被划掉就立刻消失，所以必须在这里显式断开；
     * 不这么做的话事件流与重连退避会留在后台，而界面上已经什么都看不到了。
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        (application as RemoteControlApplication).container.endSession()
        stopSelf()
    }

    override fun onDestroy() {
        stateObservation?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeConnectionState(container: AppContainer) {
        if (stateObservation != null) return
        stateObservation =
            serviceScope.launch {
                container.connectionState.collect { state ->
                    ConnectionSessionNotification.publish(context = this@ConnectionSessionService, state = state)
                }
            }
    }

    // 启动方把当时的状态放进额外参数：状态流是异步的，而首帧通知必须同步画出来，
    // 少了它就只能先写一个猜出来的状态。
    private fun stateOf(intent: Intent?): ConnectionState =
        intent
            ?.getStringExtra(EXTRA_CONNECTION_STATE)
            ?.let { name -> ConnectionState.entries.firstOrNull { state -> state.name == name } }
            ?: ConnectionState.Disconnected

    companion object {
        private const val EXTRA_CONNECTION_STATE = "connectionState"

        private const val ACTION_DISCONNECT = "xin.ctkqiang.burpsuite.remote.mobileapp.action.DISCONNECT"

        /** 启动会话服务的意图；[state] 决定首帧通知写什么。 */
        fun startIntent(
            context: Context,
            state: ConnectionState,
        ): Intent =
            Intent(context, ConnectionSessionService::class.java)
                .putExtra(EXTRA_CONNECTION_STATE, state.name)

        /** 停止会话服务的意图。 */
        fun stopIntent(context: Context): Intent = Intent(context, ConnectionSessionService::class.java)

        /** 断开动作的意图；由通知里那条动作按前台服务方式发出。 */
        fun disconnectIntent(context: Context): Intent =
            Intent(context, ConnectionSessionService::class.java).setAction(ACTION_DISCONNECT)
    }
}
