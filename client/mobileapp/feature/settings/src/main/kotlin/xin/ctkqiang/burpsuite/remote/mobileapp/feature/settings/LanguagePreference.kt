package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import java.util.Locale

/**
 * 界面语言（plan §46）。
 *
 * 与主题一样，写进偏好文件的是 [storageValue] 而不是枚举名：以后重命名枚举条目时，
 * 用户机器上已有的偏好不该失效。
 *
 * 语言列表会随着 `values-*` 资源目录一起长大，因此这里不写死「有多少种」：加了资源目录却没加枚举，
 * 选择器里就选不到；反过来加了枚举却没资源目录，选了会回落到默认语言。两者必须成对出现。
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

    /** 日语。 */
    Japanese,

    /** 蒙古文（西里尔书写；传统蒙古文是竖排文字，需要额外字体，见资源层说明）。 */
    Mongolian,
    ;

    /** 写进偏好文件的值，同时也是 Android 资源的语言限定符。 */
    val storageValue: String
        get() =
            when (this) {
                Automatic -> "automatic"
                English -> "en"
                Chinese -> "zh"
                German -> "de"
                Japanese -> "ja"
                Mongolian -> "mn"
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

        /**
         * 由设备语言推断界面会实际使用哪一种。
         *
         * 「跟随系统」这一行要用它：用户在点它之前应该知道它此刻会解析成什么，否则那四个字没有传达
         * 任何信息。只比语言代码不比地区——`zh-TW` 与 `zh-CN` 都落在 [Chinese]；认不出的语言回落
         * [English]，与资源层的回落规则一致（`values/` 就是英文）。
         */
        fun fromSystemLocale(systemLocale: Locale): LanguagePreference =
            entries.firstOrNull { language ->
                language != Automatic && language.storageValue.equals(systemLocale.language, ignoreCase = true)
            } ?: English
    }
}
