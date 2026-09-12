package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 一次远程调用的结局。
 *
 * 只分成功与失败两种：插件侧把「拒绝」与「失败」分开是它的记账需要，客户端要的是
 * 「有没有拿到东西、没拿到是为什么」，多一层分支只会让每条调用路径都多一个用不上的分支。
 */
sealed interface RemoteResult<out Value> {
    /** 调用成功。 */
    data class Succeeded<out Value>(val value: Value) : RemoteResult<Value>

    /** 调用失败，原因在 [failure] 里。 */
    data class Failed(val failure: RemoteFailure) : RemoteResult<Nothing>
}
