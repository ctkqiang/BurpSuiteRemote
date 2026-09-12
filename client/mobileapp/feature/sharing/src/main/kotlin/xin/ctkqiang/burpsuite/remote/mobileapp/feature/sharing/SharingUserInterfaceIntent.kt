package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

/**
 * 用户在导出流程里的意图。
 *
 * 闸门是显式的：[ConfirmExport] 之前什么都没写出去，因此用户在预览里看到的就是最终会被导出的内容（plan §75）。
 */
sealed interface SharingUserInterfaceIntent {
    /** 用户看过脱敏预览，确认导出。 */
    data object ConfirmExport : SharingUserInterfaceIntent

    /** 界面拿到了用户选定的写入位置。 */
    data class WriteExport(val destinationUri: String) : SharingUserInterfaceIntent
}
