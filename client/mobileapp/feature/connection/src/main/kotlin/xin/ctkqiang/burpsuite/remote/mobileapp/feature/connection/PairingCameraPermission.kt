package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/**
 * 相机权限此刻的处境。
 *
 * 区分「还没问过」和「问过被拒绝」：两者在界面上要做的事不一样——前者自动申请一次，
 * 后者要给出原因说明与再次申请入口，而不是留一片空白。
 */
enum class PairingCameraPermission {
    /** 还没申请过。 */
    Unknown,

    /** 已获授权，可以打开预览。 */
    Granted,

    /** 被拒绝；界面给出说明与再次申请入口。 */
    Denied,
}
