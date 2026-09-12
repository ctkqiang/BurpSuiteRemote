package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

/** 导出流程发出的一次性效果；不进状态，免得转屏后重放一次文件选择器（rules.md §8.1）。 */
sealed interface SharingUserInterfaceEffect {
    /** 请求装配层打开系统文件选择器，让用户指定导出位置。 */
    data class RequestExportDestination(val suggestedFileName: String) : SharingUserInterfaceEffect
}
