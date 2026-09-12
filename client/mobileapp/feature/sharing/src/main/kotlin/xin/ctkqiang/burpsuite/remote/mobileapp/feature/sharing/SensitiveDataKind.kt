package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

/** 扫描器认得出来的敏感数据种类（plan §27）。 */
enum class SensitiveDataKind {
    /** `Authorization` 头。 */
    AuthorizationHeader,

    /** `Cookie` 头。 */
    CookieHeader,

    /** `Set-Cookie` 头。 */
    SetCookieHeader,

    /** `X-API-Key` 一类自报的密钥头。 */
    ApiKeyHeader,

    /** `Bearer` 之后的访问令牌。 */
    BearerToken,

    /** 形如 `eyJ….….…` 的 JSON Web Token。 */
    JsonWebToken,

    /** 邮件地址。 */
    EmailAddress,
}
