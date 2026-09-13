// 技术日志的严重级别。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

/**
 * 一条技术日志的严重级别。
 *
 * 级别与 [TechnicalLogCategory] 是两根互相独立的轴：分类回答「这是哪一段的事实」，
 * 级别回答「出事时该先看谁」。只按分类过滤，故障会淹没在正常的传输细节里；
 * 只按级别过滤，排查某一环时又会被别的环干扰。两根轴都要有，界面才能既窄又准。
 *
 * 取值刻意与 `android.util.Log` 的优先级一一对应，落地时不必再做一次映射：
 * 界面把级别当颜色用，logcat 把级别当过滤条件用，两边读的是同一个事实。
 */
enum class TechnicalLogSeverity {
    /** 调试：排查用的细节，正常运行时不值得占用人眼。 */
    Debug,

    /** 信息：一次正常完成的动作；这是读日志时的主干。 */
    Information,

    /** 警告：没有失败，但已经偏离预期，例如重试、断洞、回退到降级路径。 */
    Warning,

    /** 错误：本次动作失败了，必须有人看。 */
    Error,
}
