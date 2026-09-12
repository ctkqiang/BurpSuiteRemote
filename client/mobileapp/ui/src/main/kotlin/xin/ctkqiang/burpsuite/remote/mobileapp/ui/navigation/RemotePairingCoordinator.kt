package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

/**
 * 配对入口。
 *
 * 端口由界面层声明、在装配层实现：界面只认得「把这段文本走完」，至于解析、校验、持久化、
 * 建连各自住在哪一层，是装配层的事（rules.md §6.2）。
 */
fun interface RemotePairingCoordinator {
    /**
     * 走完一次配对：解析票据 → 本地校验过期与协议版本 → 向插件配对 → 持久化身份与地址 → 建立事件流。
     *
     * 扫码来的文本与手输的文本走同一条路，因此不存在「扫码能连、手输连不上」这种差异。
     */
    suspend fun pairWithEncodedTicket(encodedTicketText: String): RemotePairingConclusion
}
