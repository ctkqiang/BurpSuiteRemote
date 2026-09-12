package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

/**
 * 界面语言（plan §46）。四种，没有更多。
 *
 * 与主题一样，写进偏好文件的是 [storageValue] 而不是枚举名：以后重命名枚举条目时，
 * 用户机器上已有的偏好不该失效。
 */
enum class LanguagePreference {
    /** 跟随系统语言。 */
    Automatic,

    /** 英语。 */
    English,

    /** 简体中文。 */
    Chinese,

    /** 德语。 */
    German,
    ;

    /** 写进偏好文件的值。 */
    val storageValue: String
        get() =
            when (this) {
                Automatic -> "automatic"
                English -> "en"
                Chinese -> "zh"
                German -> "de"
            }

    /** 对应的语言标签；跟随系统时为空。 */
    val languageTag: String?
        get() = if (this == Automatic) null else storageValue

    companion object {
        /**
         * 把偏好文件里的值转回枚举。
         *
         * 认不出来的一律当 Automatic：偏好文件用户能直接改，一个读不懂的语言值不该让应用起不来。
         */
        fun fromStorageValue(storageValue: String?): LanguagePreference =
            entries.firstOrNull { language -> language.storageValue.equals(storageValue, ignoreCase = true) }
                ?: Automatic
    }
}
