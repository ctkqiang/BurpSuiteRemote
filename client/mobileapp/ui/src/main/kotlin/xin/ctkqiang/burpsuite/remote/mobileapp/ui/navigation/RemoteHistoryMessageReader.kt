package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteHistoryMessage
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult

/**
 * 报文本体读取端口。
 *
 * 端口由界面层声明、在装配层实现：详情屏只认得「把这条记录的本体取回来」，至于地址端口存在哪里、
 * 请求发给谁、失败怎么分类，是装配层的事（rules.md §6.2）。
 *
 * 之所以非要这样一个端口：连接配置只活在本机偏好里，而那份偏好的读端口住在数据层，
 * 界面层看不到数据层（feature:history 只依赖 :domain 与 :ui）。没有这个端口，
 * 详情屏就永远只能显示元数据，只能如实说明「客户端还没有这条通路」——那正是要补上的缺口。
 */
fun interface RemoteHistoryMessageReader {
    /**
     * 按标识取回本体。
     *
     * 失败原因由 [RemoteResult] 原样带出，界面据此把「没配对」「连不上」「插件不支持」「记录不存在」
     * 分开说，而不是统一成一句「读取失败」。
     */
    suspend fun readHistoryMessage(historyIdentifier: String): RemoteResult<RemoteHistoryMessage>
}
