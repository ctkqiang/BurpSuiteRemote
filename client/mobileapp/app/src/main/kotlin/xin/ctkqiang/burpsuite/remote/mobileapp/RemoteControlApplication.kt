package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.Application

/** 应用进程入口。只负责建装配容器，不做别的事。 */
class RemoteControlApplication : Application() {
    // lazy 而不是构造期创建：DataStore 的文件在首次读取时才打开，别拖慢冷启动。
    val container: AppContainer by lazy { AppContainer(this) }
}
