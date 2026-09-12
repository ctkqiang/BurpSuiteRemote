package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/**
 * 连接屏发出的一次性效果；不进状态，免得转屏后重放一次权限弹窗或一次连接尝试（rules.md §8.1）。
 */
sealed interface BurpConnectionUserInterfaceEffect {
    /** 请求装配层弹出相机权限申请。 */
    data object LaunchCameraPermissionRequest : BurpConnectionUserInterfaceEffect
}
