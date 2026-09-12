package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

/** 历史记录的归属：实时投影还是用户显式存下的副本；归档之后不再随 Burp 变化（plan §22）。 */
enum class HistoryArchiveState {
    /** 来自插件的实时投影，可以被后续事件继续改写。 */
    Live,

    /** 用户显式保存的副本，之后只允许加注解与书签。 */
    Archived,
}
