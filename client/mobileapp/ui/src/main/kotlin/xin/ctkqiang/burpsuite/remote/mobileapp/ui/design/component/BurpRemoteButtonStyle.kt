package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

/** 按钮的四种形态。一屏之内同一种意图只用同一种形态，否则用户要靠试才知道哪个按钮重要。 */
enum class BurpRemoteButtonStyle {
    /** 主操作：品牌橙实底。 */
    Primary,

    /** 次操作：描边浅底。 */
    Secondary,

    /** 轻量操作：无底色，只用强调色文字。 */
    Ghost,

    /** 破坏性操作：危险语义色实底。 */
    Danger,
}
