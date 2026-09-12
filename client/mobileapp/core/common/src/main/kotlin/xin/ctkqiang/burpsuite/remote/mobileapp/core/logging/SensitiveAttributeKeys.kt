// 日志里取值的脱敏规则。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

/**
 * 属性名到「是否敏感」的判定。
 *
 * 这是最后一道闸：调用方本不该把这些取值传进来，但一道只能靠自觉的规矩等于没有规矩，
 * 所以在落地前再抹一次（rules.md §12）。判重不看大小写，插件与客户端两侧的写法并不统一。
 */
internal object SensitiveAttributeKeys {
    /** 取值必须被抹掉的属性名。 */
    private val SENSITIVE_KEY_NAMES =
        setOf(
            "authorization",
            "proxyauthorization",
            "cookie",
            "setcookie",
            "token",
            "accesstoken",
            "refreshtoken",
            "pairingcode",
            "challengeidentifier",
            "password",
            "apikey",
            "secret",
            "credential",
            "requestbody",
            "responsebody",
            "body",
        )

    /** 属性名是否敏感；先去掉分隔符再比，`set-cookie` 与 `setCookie` 判成同一个。 */
    fun isSensitive(attributeName: String): Boolean =
        SENSITIVE_KEY_NAMES.contains(attributeName.lowercase().filter { character -> character.isLetterOrDigit() })
}
