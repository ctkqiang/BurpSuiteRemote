package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.graphics.Color
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeFlavor

/**
 * 八套主题口味的调色板。
 *
 * 每套口味同时给出浅色与深色两份 [BurpRemoteColourScheme]：明暗由系统或用户决定，
 * 口味只决定强调色与整体色调倾向，两者正交（rules.md §10）。
 *
 * 设计原则：
 * - 强调色是整套口味的身份；背景、表面、描边的明度分层保持一致，只在色偏上做微调。
 * - 语义色（success/warning/danger/information）跨口味保持相同色相，只在深浅两档间调整亮度，
 *   确保「绿色=成功、红色=危险」的肌肉记忆不会被某套口味打破。
 * - 代码高亮色跨口味统一，避免换口味时代码阅读节奏被打乱。
 */
object BurpRemoteFlavourPalettes {
    /** 浅色基底：纯白背景，极浅灰表面，细描边。 */
    private val lightBase =
        LightBase(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFAFAFA),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFE7E8EA),
            contentPrimary = Color(0xFF0B0D0F),
            contentSecondary = Color(0xFF5B6169),
        )

    /** 深色基底：纯黑背景，两档深灰表面，细描边。 */
    private val darkBase =
        DarkBase(
            background = Color(0xFF000000),
            surface = Color(0xFF0E0F11),
            surfaceElevated = Color(0xFF17181B),
            outline = Color(0xFF26272B),
            contentPrimary = Color(0xFFF2F4F6),
            contentSecondary = Color(0xFF9AA1A9),
        )

    /**
     * 取一套口味的配色。
     *
     * @param flavor 用户选的口味。
     * @param isDark 取浅色还是深色那一份。
     */
    fun schemeFor(
        flavor: ThemeFlavor,
        isDark: Boolean,
    ): BurpRemoteColourScheme =
        when (flavor) {
            ThemeFlavor.BurpClassic -> if (isDark) burpClassicDark else burpClassicLight
            ThemeFlavor.CyberCyan -> if (isDark) cyberCyanDark else cyberCyanLight
            ThemeFlavor.ForestEmerald -> if (isDark) forestEmeraldDark else forestEmeraldLight
            ThemeFlavor.RoyalViolet -> if (isDark) royalVioletDark else royalVioletLight
            ThemeFlavor.RoseGold -> if (isDark) roseGoldDark else roseGoldLight
            ThemeFlavor.OceanBlue -> if (isDark) oceanBlueDark else oceanBlueLight
            ThemeFlavor.SolarAmber -> if (isDark) solarAmberDark else solarAmberLight
            ThemeFlavor.Monochrome -> if (isDark) monochromeDark else monochromeLight
        }

    // ── 1. Burp Classic：品牌橙 ──────────────────────────────────────────────

    private val burpClassicLight =
        lightBase.toScheme(
            accent = Color(0xFFFF6633),
            onAccent = Color(0xFFFFFFFF),
        )

    private val burpClassicDark =
        darkBase.toScheme(
            accent = Color(0xFFFF6633),
            onAccent = Color(0xFF000000),
        )

    // ── 2. Cyber Cyan：青色赛博 ──────────────────────────────────────────────

    private val cyberCyanLight =
        lightBase.copy(
            // 冷调白底：极轻微的青蓝偏色，让青色强调不显得孤立。
            background = Color(0xFFF7FBFC),
            surface = Color(0xFFF1F6F8),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFDDE7EC),
        ).toScheme(
            accent = Color(0xFF06B6D4),
            onAccent = Color(0xFFFFFFFF),
        )

    private val cyberCyanDark =
        darkBase.copy(
            // 深海军蓝底，不是纯黑——青色在纯黑上会发飘，深底让它立得住。
            background = Color(0xFF05080F),
            surface = Color(0xFF0A1018),
            surfaceElevated = Color(0xFF111A24),
            outline = Color(0xFF1E2A36),
        ).toScheme(
            accent = Color(0xFF22D3EE),
            onAccent = Color(0xFF04111A),
        )

    // ── 3. Forest Emerald：翡翠绿 ────────────────────────────────────────────

    private val forestEmeraldLight =
        lightBase.copy(
            // 暖调白底：极轻微的黄绿偏色，呼应绿色强调。
            background = Color(0xFFF8FAF7),
            surface = Color(0xFFF2F5F1),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFDFE6DE),
        ).toScheme(
            accent = Color(0xFF059669),
            onAccent = Color(0xFFFFFFFF),
        )

    private val forestEmeraldDark =
        darkBase.copy(
            // 深森林底，绿调比 Cyber 更暖。
            background = Color(0xFF060B08),
            surface = Color(0xFF0B120E),
            surfaceElevated = Color(0xFF121B15),
            outline = Color(0xFF1E2B23),
        ).toScheme(
            accent = Color(0xFF34D399),
            onAccent = Color(0xFF04130D),
        )

    // ── 4. Royal Violet：紫罗兰 ──────────────────────────────────────────────

    private val royalVioletLight =
        lightBase.copy(
            background = Color(0xFFFBFAFC),
            surface = Color(0xFFF6F4F8),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFE6E2EB),
        ).toScheme(
            accent = Color(0xFF7C3AED),
            onAccent = Color(0xFFFFFFFF),
        )

    private val royalVioletDark =
        darkBase.copy(
            background = Color(0xFF0A0710),
            surface = Color(0xFF120D1A),
            surfaceElevated = Color(0xFF1A1426),
            outline = Color(0xFF2A2038),
        ).toScheme(
            accent = Color(0xFFA78BFA),
            onAccent = Color(0xFF0E0820),
        )

    // ── 5. Rose Gold：玫瑰金 ─────────────────────────────────────────────────

    private val roseGoldLight =
        lightBase.copy(
            // 暖白底，带极淡的粉调。
            background = Color(0xFFFCFAFA),
            surface = Color(0xFFF8F4F4),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFEBE3E3),
        ).toScheme(
            accent = Color(0xFFE11D48),
            onAccent = Color(0xFFFFFFFF),
        )

    private val roseGoldDark =
        darkBase.copy(
            // 深酒红底，暖而不艳。
            background = Color(0xFF0E0708),
            surface = Color(0xFF160C0E),
            surfaceElevated = Color(0xFF1F1216),
            outline = Color(0xFF332026),
        ).toScheme(
            accent = Color(0xFFFB7185),
            onAccent = Color(0xFF1A0810),
        )

    // ── 6. Ocean Blue：海洋蓝 ────────────────────────────────────────────────

    private val oceanBlueLight =
        lightBase.copy(
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFF1F5F9),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFDDE5EE),
        ).toScheme(
            accent = Color(0xFF2563EB),
            onAccent = Color(0xFFFFFFFF),
        )

    private val oceanBlueDark =
        darkBase.copy(
            background = Color(0xFF050810),
            surface = Color(0xFF0A0F1A),
            surfaceElevated = Color(0xFF111827),
            outline = Color(0xFF1E293B),
        ).toScheme(
            accent = Color(0xFF60A5FA),
            onAccent = Color(0xFF05101F),
        )

    // ── 7. Solar Amber：琥珀金 ───────────────────────────────────────────────

    private val solarAmberLight =
        lightBase.copy(
            // 暖白底，带极淡的黄调。
            background = Color(0xFFFCFAF7),
            surface = Color(0xFFF8F4EE),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFEBE3D8),
        ).toScheme(
            accent = Color(0xFFD97706),
            onAccent = Color(0xFFFFFFFF),
        )

    private val solarAmberDark =
        darkBase.copy(
            // 深棕底，落日暖调。
            background = Color(0xFF0E0B05),
            surface = Color(0xFF161108),
            surfaceElevated = Color(0xFF1F180C),
            outline = Color(0xFF332A1A),
        ).toScheme(
            accent = Color(0xFFFBBF24),
            onAccent = Color(0xFF1A1205),
        )

    // ── 8. Monochrome：无彩色 ────────────────────────────────────────────────

    private val monochromeLight =
        lightBase.copy(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5),
            surfaceElevated = Color(0xFFFFFFFF),
            outline = Color(0xFFE0E0E0),
        ).toScheme(
            // 强调色用最深的近黑，不靠色相靠对比。
            accent = Color(0xFF1A1A1A),
            onAccent = Color(0xFFFFFFFF),
        )

    private val monochromeDark =
        darkBase.copy(
            background = Color(0xFF000000),
            surface = Color(0xFF0F0F0F),
            surfaceElevated = Color(0xFF1A1A1A),
            outline = Color(0xFF2E2E2E),
        ).toScheme(
            // 强调色用最亮的近白。
            accent = Color(0xFFEDEDED),
            onAccent = Color(0xFF000000),
        )

    // ── 基底与装配 ───────────────────────────────────────────────────────────

    /**
     * 浅色共用基底字段；不同口味只改色偏，明度分层保持一致。
     */
    private data class LightBase(
        val background: Color,
        val surface: Color,
        val surfaceElevated: Color,
        val outline: Color,
        val contentPrimary: Color,
        val contentSecondary: Color,
    )

    /**
     * 深色共用基底字段。
     */
    private data class DarkBase(
        val background: Color,
        val surface: Color,
        val surfaceElevated: Color,
        val outline: Color,
        val contentPrimary: Color,
        val contentSecondary: Color,
    )

    /**
     * 把基底 + 强调色装成完整配色。
     *
     * 语义色与代码高亮色跨口味统一，只在深浅两档间调整亮度，保证阅读肌肉记忆不被打破。
     */
    private fun LightBase.toScheme(
        accent: Color,
        onAccent: Color,
    ): BurpRemoteColourScheme =
        BurpRemoteColourScheme(
            background = background,
            surface = surface,
            surfaceElevated = surfaceElevated,
            outline = outline,
            contentPrimary = contentPrimary,
            contentSecondary = contentSecondary,
            accent = accent,
            onAccent = onAccent,
            success = Color(0xFF16A34A),
            warning = Color(0xFFD97706),
            danger = Color(0xFFDC2626),
            information = Color(0xFF2563EB),
            scrim = Color(0xB3000000),
            onScrim = Color(0xFFFFFFFF),
            codeKey = Color(0xFF7C3AED),
            codeString = Color(0xFF15803D),
            codeNumber = Color(0xFFB45309),
            codeLiteral = Color(0xFF0E7490),
            codeComment = Color(0xFF6B7280),
        )

    private fun DarkBase.toScheme(
        accent: Color,
        onAccent: Color,
    ): BurpRemoteColourScheme =
        BurpRemoteColourScheme(
            background = background,
            surface = surface,
            surfaceElevated = surfaceElevated,
            outline = outline,
            contentPrimary = contentPrimary,
            contentSecondary = contentSecondary,
            accent = accent,
            onAccent = onAccent,
            success = Color(0xFF4ADE80),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFF87171),
            information = Color(0xFF60A5FA),
            scrim = Color(0xCC000000),
            onScrim = Color(0xFFFFFFFF),
            codeKey = Color(0xFFC4B5FD),
            codeString = Color(0xFF86EFAC),
            codeNumber = Color(0xFFFCD34D),
            codeLiteral = Color(0xFF67E8F9),
            codeComment = Color(0xFF7D8590),
        )
}
