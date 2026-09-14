package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R

/**
 * 一次配对尝试的结论。
 *
 * 结论分得这么细，是因为每一种都要给出不同的下一步：票据过期要重扫二维码，配对码不匹配要重输，
 * 协议不一致要换一端升级，不可达要查网络。统一成一句「配对失败」等于把排查工作全丢给用户。
 *
 * 因此每个结论除了一句「哪里不对」，还可以带一句「现在做什么」（[nextStepResource]）。
 * 前者是一枚胶囊，只能短；后者是一整句话，要写出具体动作——例如去插件面板按哪个按钮。
 * 只说「会话已过期」而不说去按「刷新配对码」，用户就会对着同一张作废的二维码反复扫。
 */
enum class RemotePairingConclusion(
    /** 这个结论说给用户听的文案；用作胶囊，因此要短。 */
    @StringRes val messageResource: Int,
    /** 这个结论对应的下一步；没有已知下一步时为 null，界面只显示结论本身。 */
    @StringRes val nextStepResource: Int? = null,
) {
    /** 已配对，并且已在后台开始建立事件流连接。 */
    Succeeded(R.string.pairing_conclusion_succeeded),

    /** 二维码内容不是配对票据。 */
    MalformedTicket(
        messageResource = R.string.pairing_conclusion_malformed_ticket,
        nextStepResource = R.string.pairing_next_step_refresh_ticket,
    ),

    /** 票据已过期；本地拦下，没有发给插件。 */
    TicketExpired(
        messageResource = R.string.pairing_conclusion_ticket_expired,
        nextStepResource = R.string.pairing_next_step_refresh_ticket,
    ),

    /** 两端协议版本不一致。 */
    ProtocolVersionUnsupported(R.string.pairing_conclusion_protocol_version_unsupported),

    /** 插件不认识这个设备，需要先在插件面板发起配对。 */
    DeviceNotPaired(R.string.pairing_conclusion_device_not_paired),

    /** 配对码不匹配。 */
    PairingCodeMismatch(
        messageResource = R.string.pairing_conclusion_pairing_code_mismatch,
        nextStepResource = R.string.pairing_next_step_refresh_ticket,
    ),

    /** 插件侧的配对会话已过期。 */
    PairingSessionExpired(
        messageResource = R.string.pairing_conclusion_pairing_session_expired,
        nextStepResource = R.string.pairing_next_step_refresh_ticket,
    ),

    /** 插件不可达：地址、端口或网络三者之一不对。 */
    Unreachable(R.string.pairing_conclusion_unreachable),

    /** 其余失败；日志里有具体分类。 */
    Failed(R.string.pairing_conclusion_failed),
}
