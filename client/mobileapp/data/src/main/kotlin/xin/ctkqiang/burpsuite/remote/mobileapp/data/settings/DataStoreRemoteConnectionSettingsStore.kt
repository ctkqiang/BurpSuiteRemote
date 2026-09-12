package xin.ctkqiang.burpsuite.remote.mobileapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.RemoteServerEndpoint

// 放顶层而不是类内伴生对象里：下面的属性委托在类外，看不到类内的成员。
private const val CONNECTION_SETTINGS_FILE_NAME = "remote_connection_settings"

private val Context.remoteConnectionDataStore by preferencesDataStore(name = CONNECTION_SETTINGS_FILE_NAME)

/**
 * DataStore 实现的连接配置端口。
 *
 * 主机、端口这类配置本身不敏感，直接存在偏好里；设备身份是凭据，写进去之前先经密钥库加密
 * （plan §55、rules.md §12）。
 */
class DataStoreRemoteConnectionSettingsStore(
    private val context: Context,
    private val secretCipher: AndroidKeystoreSecretCipher,
) : RemoteConnectionSettingsStore {
    override fun observeConnectionConfiguration(): Flow<RemoteConnectionConfiguration?> =
        context.remoteConnectionDataStore.data.map { preferences -> connectionConfigurationOf(preferences) }

    override suspend fun readConnectionConfiguration(): RemoteConnectionConfiguration? =
        connectionConfigurationOf(context.remoteConnectionDataStore.data.first())

    override suspend fun saveConnectionConfiguration(configuration: RemoteConnectionConfiguration) {
        context.remoteConnectionDataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = configuration.host
            preferences[SERVER_PORT_KEY] = configuration.port
            preferences[DEVICE_NAME_KEY] = configuration.deviceName
            preferences[IS_TLS_ENABLED_KEY] = configuration.isTlsEnabled
        }
    }

    override suspend fun saveServerEndpoint(endpoint: RemoteServerEndpoint) {
        context.remoteConnectionDataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = endpoint.host
            preferences[SERVER_PORT_KEY] = endpoint.port
        }
    }

    override suspend fun clearServerEndpoint() {
        context.remoteConnectionDataStore.edit { preferences ->
            preferences.remove(SERVER_ADDRESS_KEY)
            preferences.remove(SERVER_PORT_KEY)
        }
    }

    override fun observeDeviceIdentifier(): Flow<DeviceIdentifier?> =
        context.remoteConnectionDataStore.data.map { preferences -> deviceIdentifierOf(preferences) }

    override suspend fun readDeviceIdentifier(): DeviceIdentifier? =
        deviceIdentifierOf(context.remoteConnectionDataStore.data.first())

    override suspend fun saveDeviceIdentifier(deviceIdentifier: DeviceIdentifier) {
        context.remoteConnectionDataStore.edit { preferences ->
            preferences[DEVICE_IDENTIFIER_KEY] = secretCipher.encrypt(deviceIdentifier.value)
        }
    }

    override suspend fun clearDeviceIdentifier() {
        context.remoteConnectionDataStore.edit { preferences -> preferences.remove(DEVICE_IDENTIFIER_KEY) }
    }

    // 地址与端口缺一不可：只有其中一个时按「尚未配置」处理，而不是编一个默认端口出来。
    private fun connectionConfigurationOf(preferences: Preferences): RemoteConnectionConfiguration? {
        val host = preferences[SERVER_ADDRESS_KEY]
        val port = preferences[SERVER_PORT_KEY]
        if (host.isNullOrBlank() || port == null) return null

        return RemoteConnectionConfiguration(
            host = host,
            port = port,
            deviceName = preferences[DEVICE_NAME_KEY].orEmpty(),
            isTlsEnabled = preferences[IS_TLS_ENABLED_KEY] ?: false,
        )
    }

    // 解不开的密文等于没有身份：密钥被换掉之后，如实回到「未配对」，不要拿半截凭据去连。
    private fun deviceIdentifierOf(preferences: Preferences): DeviceIdentifier? {
        val encodedCipherText = preferences[DEVICE_IDENTIFIER_KEY] ?: return null
        val plainText = secretCipher.decrypt(encodedCipherText) ?: return null
        return DeviceIdentifier(value = plainText)
    }

    private companion object {
        val SERVER_ADDRESS_KEY = stringPreferencesKey("server_address")

        val SERVER_PORT_KEY = intPreferencesKey("server_port")

        val DEVICE_NAME_KEY = stringPreferencesKey("device_name")

        val IS_TLS_ENABLED_KEY = booleanPreferencesKey("is_tls_enabled")

        val DEVICE_IDENTIFIER_KEY = stringPreferencesKey("device_identifier_ciphertext")
    }
}
