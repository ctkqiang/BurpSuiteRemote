package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.Application
import android.content.Context
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreferenceRepository

/** 应用进程入口。只负责建装配容器、启动后台链路与语言偏好，不做别的事。 */
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

    override fun onCreate() {
        super.onCreate()
        // 事件摄入与连接都活在进程级作用域里，因此进程一起来就得把它们点着（plan §61）。
        container.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        // 真机上进程是被系统直接收掉的，这条路只在模拟器与仪器测试里走到；
        // 留着它是为了让「停」这件事有唯一出口，而不是散落在各处的 cancel。
        container.stop()
    }

    private val languageContext: Context get() = applicationContext
}
