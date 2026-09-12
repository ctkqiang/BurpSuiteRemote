package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

/** 状态胶囊的语气。赏金猎人靠颜色一眼判读，因此语气必须与语义色一一对应，不额外发明颜色。 */
enum class BurpRemoteStatusTone {
    /** 中性：只是说明，不带判断。 */
    Neutral,

    /** 在线：会话此刻真的可用。 */
    Live,

    /** 警告：还能继续，但需要留意。 */
    Warning,

    /** 危险：已经出事或马上要出事。 */
    Danger,
}
