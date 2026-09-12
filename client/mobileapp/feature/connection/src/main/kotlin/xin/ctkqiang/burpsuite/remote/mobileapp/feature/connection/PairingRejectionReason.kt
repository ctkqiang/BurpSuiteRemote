

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/**
 * 插件拒绝一次配对时的归类。
 *
 * 领域层的 [xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure] 有二十来种取值，
 * 全都翻成界面文案既没有意义也翻译不完；这里按「用户下一步要做什么」归成几类，
 * 归类发生在 ViewModel，界面只负责取文案。
 */
enum class PairingRejectionReason {
    /** 配对码不对。 */
    PairingCodeRejected,

    /** 配对会话已过期；票据得重新生成。 */
    PairingSessionExpired,

    /** 插件不认识这张票据对应的配对会话。 */
    PairingSessionUnavailable,

    /** 两端协议版本不一致。 */
    ProtocolVersionUnsupported,

    /** 设备身份被拒绝。 */
    AuthenticationRejected,

    /** 连不上或超时。 */
    ServerUnreachable,

    /** 插件侧该端点尚未实现。 */
    ActionNotSupported,

    /** 插件报了别的错，客户端不认识。 */
    Unknown,
}
