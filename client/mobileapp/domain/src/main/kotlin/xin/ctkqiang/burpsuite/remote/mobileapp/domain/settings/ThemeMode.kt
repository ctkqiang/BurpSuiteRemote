package xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings

/** 主题模式。三种，没有更多。 */
enum class ThemeMode {
    Automatic,
    Light,
    Dark,
    ;

    /**
     * 写进偏好文件的值。
     *
     * 不用枚举名：以后重命名枚举条目时，用户机器上已有的偏好不该失效。
     */
    val storageValue: String
        get() =
            when (this) {
                Automatic -> "automatic"
                Light -> "light"
                Dark -> "dark"
            }

    companion object {
        /**
         * 把偏好文件里的值转回枚举。
         *
         * 认不出来的一律当 Automatic：偏好文件用户能直接改，一个读不懂的主题值不该让应用起不来。
         */
        fun fromStorageValue(storageValue: String?): ThemeMode =
            entries.firstOrNull { themeMode -> themeMode.storageValue.equals(storageValue, ignoreCase = true) }
                ?: Automatic

        /** 解析时用到的全部取值，供设置界面与测试共用。 */
        val storageValues: List<String> get() = entries.map { themeMode -> themeMode.storageValue }
    }
}
