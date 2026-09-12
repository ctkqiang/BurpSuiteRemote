package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R

/**
 * 一次配对尝试的结论。
 *
 * 结论分得这么细，是因为每一种都要给出不同的下一步：票据过期要重扫二维码，配对码不匹配要重输，
 * 协议不一致要换一端升级，不可达要查网络。统一成一句「配对失败」等于把排查工作全丢给用户。
 */
enum class RemotePairingConclusion(
    /** 这个结论说给用户听的文案。 */
    @StringRes val messageResource: Int,
) {
    /** 已配对，并且已在后台开始建立事件流连接。 */
    Succeeded(R.string.pairing_conclusion_succeeded),

    /** 二维码内容不是配对票据。 */
    MalformedTicket(R.string.pairing_conclusion_malformed_ticket),

    /** 票据已过期；本地拦下，没有发给插件。 */
    TicketExpired(R.string.pairing_conclusion_ticket_expired),

    /** 两端协议版本不一致。 */
    ProtocolVersionUnsupported(R.string.pairing_conclusion_protocol_version_unsupported),

    /** 插件不认识这个设备，需要先在插件面板发起配对。 */
    DeviceNotPaired(R.string.pairing_conclusion_device_not_paired),

    /** 配对码不匹配。 */
    PairingCodeMismatch(R.string.pairing_conclusion_pairing_code_mismatch),

    /** 插件侧的配对会话已过期。 */
    PairingSessionExpired(R.string.pairing_conclusion_pairing_session_expired),

    /** 插件不可达：地址、端口或网络三者之一不对。 */
    Unreachable(R.string.pairing_conclusion_unreachable),

    /** 其余失败；日志里有具体分类。 */
    Failed(R.string.pairing_conclusion_failed),
}
