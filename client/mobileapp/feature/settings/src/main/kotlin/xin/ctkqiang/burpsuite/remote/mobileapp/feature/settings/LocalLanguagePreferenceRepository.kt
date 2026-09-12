package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 语言偏好的端口由装配层注入。
 *
 * 语言要在 Activity 重建时立刻生效，而导航壳的依赖表是固定的五个领域端口，因此这里走
 * CompositionLocal：装配层在最外层提供一次，语言设置屏取用。
 */
val LocalLanguagePreferenceRepository: ProvidableCompositionLocal<LanguagePreferenceRepository> =
    staticCompositionLocalOf { error("LanguagePreferenceRepository 未提供；应由装配层注入。") }
