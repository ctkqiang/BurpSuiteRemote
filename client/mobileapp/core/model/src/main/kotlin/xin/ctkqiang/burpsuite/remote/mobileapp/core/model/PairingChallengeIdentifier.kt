// 一次配对尝试的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识一次配对尝试；能和配对码分开，是因为它可以进日志而配对码不行。
 *
 * @property value 文本取值，示例约定 challenge_ 前缀。
 */
@JvmInline
value class PairingChallengeIdentifier(val value: String)
