package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.json.Json

/**
 * 线上报文的编解码配置。
 *
 * 容忍未知字段：新版插件加字段时旧客户端仍要能读完整条报文，而不是整条解析失败。
 * 不开 encodeDefaults：出站报文里只有真正赋值的字段才会出现，取默认值的可选字段自然被省略。
 */
object RemoteWireJson {
    /** 客户端唯一的线上编解码实例。 */
    val instance: Json = Json { ignoreUnknownKeys = true }
}
