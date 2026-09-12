// 协议报文的 JSON 编解码配置，插件内唯一一份。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.json.Json

/**
 * 协议报文的 JSON 配置。
 *
 * 只此一份：编解码规则一旦散落，就会出现「某条路径忽略未知字段、另一条不忽略」这种只在特定接口上复现的兼容性差异。
 */
object RemoteProtocolJson {
    /** 编解码实例；开启未知字段容忍是为了让新版本客户端加字段不会打断旧版插件。 */
    val instance: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}
