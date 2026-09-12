package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/**
 * 一次配对尝试的结局（plan §55）。
 *
 * 只分「插件认了」和「插件没认」：具体是哪条拒绝理由属于插件的记账，客户端要的是要不要换个方式再来。
 */
enum class PairingOutcome {
    /** 插件接受了配对。 */
    Succeeded,

    /** 插件拒绝了配对；票据可能已失效，重新扫一张即可。 */
    Rejected,
}
