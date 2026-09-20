package xin.ctkqiang.burpsuite.remote.mobileapp.ui.localisation

import android.content.res.Resources
import java.util.Locale

/**
 * 设备当前语言。
 *
 * 取 [Resources.getSystem] 而不是任意 Context 的 resources：后者可能已经被应用内选择的语言覆写过
 * （见 `MainActivity.withLanguage`），拿它当「系统语言」会在界面重建时把覆写值读回来当成系统值。
 */
fun systemLocale(): Locale {
    val systemLocales = Resources.getSystem().configuration.locales
    // 列表理论上非空；真为空时退回进程默认值，总比抛异常好。
    return if (systemLocales.isEmpty) Locale.getDefault() else systemLocales[0]
}
