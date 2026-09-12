

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/**
 * 扫到的二维码为什么用不了。
 *
 * 三种原因对用户要做的事完全不同：换一张码、去 Burp 重新生成、升级其中一端，因此不能合并成
 * 一句「扫码失败」（rules.md §5.1：客户端不编造事实，也不含糊其辞）。
 */
enum class PairingTicketRejection {
    /** 扫到了二维码，但其中的文本不符合票据契约。 */
    Undecodable,

    /** 票据本身合法，但已经过了失效时刻。 */
    Expired,

    /** 票据声明的协议版本与本客户端支持的不一致。 */
    ProtocolVersionUnsupported,
}
