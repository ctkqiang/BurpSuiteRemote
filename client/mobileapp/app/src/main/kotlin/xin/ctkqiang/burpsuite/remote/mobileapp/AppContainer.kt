package xin.ctkqiang.burpsuite.remote.mobileapp

import android.content.Context
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.DataStoreSettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository

/** 依赖装配处。全应用只有这里知道接口背后的实现是谁。 */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context.applicationContext)
}
