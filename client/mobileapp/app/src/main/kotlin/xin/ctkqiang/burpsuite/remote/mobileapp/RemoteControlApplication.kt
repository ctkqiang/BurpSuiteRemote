package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.Application
import android.content.Context
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreferenceRepository

/** 应用进程入口。只负责建装配容器与语言偏好，不做别的事。 */
class RemoteControlApplication : Application() {
    // lazy 而不是构造期创建：DataStore 的文件在首次读取时才打开，别拖慢冷启动。
    val container: AppContainer by lazy { AppContainer(this) }

    /**
     * 语言偏好在容器之外单独持有：Activity 建界面之前就要同步读它，
     * 走容器会把 Room 那一串一起拉起来。
     */
    val languagePreferenceRepository: LanguagePreferenceRepository by lazy {
        SharedPreferencesLanguagePreferenceRepository(languageContext)
    }

    private val languageContext: Context get() = applicationContext
}
