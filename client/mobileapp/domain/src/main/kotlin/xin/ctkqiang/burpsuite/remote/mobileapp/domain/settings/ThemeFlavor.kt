package xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings

/**
 * 主题口味：一套完整的配色身份。
 *
 * 「口味」与「明暗」是两件事：口味决定强调色与整体色调，明暗决定底色深浅。
 * 每个口味都同时有浅色与深色两套配色，因此「跟随系统」在任意口味下都能正确翻转。
 *
 * 取值写进偏好文件时用 [storageValue]，不用枚举名——以后重命名条目时用户已有偏好不该失效。
 */
enum class ThemeFlavor {
    /** 品牌橙，应用的默认口味。 */
    BurpClassic,

    /** 青色强调，偏赛博/终端的冷调科技感。 */
    CyberCyan,

    /** 翡翠绿强调，自然沉稳。 */
    ForestEmerald,

    /** 紫罗兰强调，优雅神秘。 */
    RoyalViolet,

    /** 玫瑰金强调，温暖柔和。 */
    RoseGold,

    /** 海洋蓝强调，冷静通透。 */
    OceanBlue,

    /** 琥珀金强调，落日暖调。 */
    SolarAmber,

    /** 无彩色，仅靠明度分层，最克制。 */
    Monochrome,
    ;

    /** 写进偏好文件的值。 */
    val storageValue: String
        get() =
            when (this) {
                BurpClassic -> "burp_classic"
                CyberCyan -> "cyber_cyan"
                ForestEmerald -> "forest_emerald"
                RoyalViolet -> "royal_violet"
                RoseGold -> "rose_gold"
                OceanBlue -> "ocean_blue"
                SolarAmber -> "solar_amber"
                Monochrome -> "monochrome"
            }

    companion object {
        /**
         * 把偏好文件里的值转回枚举。
         *
         * 认不出来的一律当 [BurpClassic]：偏好文件用户能直接改，一个读不懂的口味值不该让应用起不来。
         */
        fun fromStorageValue(storageValue: String?): ThemeFlavor =
            entries.firstOrNull { flavor -> flavor.storageValue.equals(storageValue, ignoreCase = true) }
                ?: BurpClassic

        /** 解析时用到的全部取值，供设置界面与测试共用。 */
        val storageValues: List<String> get() = entries.map { flavor -> flavor.storageValue }
    }
}
