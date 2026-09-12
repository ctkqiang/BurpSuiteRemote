package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

/**
 * 扫码弹窗当前的输入方式。
 *
 * 两种方式在同一个弹窗里切换：相机扫不出来（机型没有闪光灯、码太脏、权限被系统锁死）时，
 * 手输配对文本是唯一还能走下去的路，另开一屏会让用户以为功能不在同一个地方。
 */
internal enum class PairingScannerMode {
    /** 相机取景。 */
    Camera,

    /** 手输或粘贴配对文本。 */
    ManualTicketText,
}
