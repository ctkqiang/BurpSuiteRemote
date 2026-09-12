package xin.ctkqiang.burpsuite.remote.mobileapp.data.serialization

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 事件载荷与元数据的 JSON 对象编解码。
 *
 * 两者形状相同（字段名到标量取值），因此共用一处：各写一份迟早会分叉（rules.md §7.6）。
 */
object JsonObjectAttributes {
    /**
     * 摊平成标量字段。
     *
     * 读不出 JSON 对象时返回空表而不是抛错：事件本身仍是事实，不能因为载荷读不懂就把它丢掉。
     */
    fun parse(encodedText: String): Map<String, String> {
        val jsonObject =
            try {
                Json.parseToJsonElement(encodedText) as? JsonObject
            } catch (serializationException: SerializationException) {
                null
            } ?: return emptyMap()

        return jsonObject.entries
            .mapNotNull { (fieldName, fieldValue) ->
                when (fieldValue) {
                    is JsonNull -> null
                    is JsonPrimitive -> fieldName to fieldValue.content
                    // 嵌套对象与数组不是标量，投影读不到它们，留在日志原文里即可。
                    else -> null
                }
            }
            .toMap()
    }

    /** 编码成 JSON 对象文本；取值一律按文本写。 */
    fun encode(attributes: Map<String, String>): String =
        buildJsonObject {
            attributes.forEach { (fieldName, fieldValue) -> put(fieldName, fieldValue) }
        }.toString()
}
