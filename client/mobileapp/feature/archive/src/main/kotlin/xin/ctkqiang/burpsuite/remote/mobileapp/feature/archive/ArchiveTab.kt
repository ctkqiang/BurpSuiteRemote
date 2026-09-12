package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

/** 归档屏的三个页签（plan §43 的 Archive）。 */
enum class ArchiveTab {
    /** 用户显式保存下来的历史副本。 */
    SavedHistory,

    /** 加了书签的历史记录。 */
    Bookmarks,

    /** 截图。 */
    Screenshots,
}
