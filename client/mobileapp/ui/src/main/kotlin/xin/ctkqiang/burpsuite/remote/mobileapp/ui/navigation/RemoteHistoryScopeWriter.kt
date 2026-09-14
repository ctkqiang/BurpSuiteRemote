package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult

/**
 * 把历史记录的主机加入 Burp 作用域的写入端口。
 *
 * 与 [RemoteHistoryMessageReader] 同源：端口由界面层声明、在装配层实现，因为连接配置只活在本机
 * 偏好里，而那份偏好的读端口住在数据层，界面层看不到数据层（feature:history 只依赖 :domain 与 :ui）。
 *
 * 之所以只传标识、不传主机串：主机串由插件按它当下的事实拼出，客户端手上那条元数据可能已经过期，
 * 拿它去写作用域会把一个错误的地址写进去——写错的证据链比没写更糟（rules.md §5.1）。
 */
fun interface RemoteHistoryScopeWriter {
    /**
     * 把这条记录所属的主机加入作用域。
     *
     * 结局由 [RemoteResult] 原样带出，界面据此把「没配对」「连不上」「插件还没做」「记录已不在」
     * 分开说：加作用域既可能因为通路没接上而失败，也可能因为插件还没实现该端点而失败，两者对用户
     * 的下一步完全不同。
     */
    suspend fun addHistoryHostToScope(historyIdentifier: String): RemoteResult<Unit>
}
