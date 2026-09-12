package xin.ctkqiang.burpsuite.remote.mobileapp.domain.event

/**
 * 事件载荷里的标量字段。
 *
 * 载荷的大报文不走事件流（plan §82），因此这里只会出现短文本；把 JSON 摊平成键值对是适配层的活，
 * 领域层因此不必依赖任何序列化库。
 *
 * @property values 字段名到文本取值的映射。
 */
@JvmInline
value class EventPayloadAttributes(private val values: Map<String, String>) {
    /** 取文本字段；缺失或为空都算没有。 */
    fun text(fieldName: String): String? = values[fieldName]?.takeIf { fieldValue -> fieldValue.isNotBlank() }

    /** 取整数字段；缺失或不是整数都算没有，不猜默认值。 */
    fun integer(fieldName: String): Int? = values[fieldName]?.toIntOrNull()

    /** 取长整数字段；缺失或不是整数都算没有。 */
    fun long(fieldName: String): Long? = values[fieldName]?.toLongOrNull()

    /** 取布尔字段；只认 true 与 false 两个写法。 */
    fun boolean(fieldName: String): Boolean? = values[fieldName]?.toBooleanStrictOrNull()

    companion object {
        /** 空载荷；事件只报元数据时用它，免得各处重复构造空表。 */
        val EMPTY: EventPayloadAttributes = EventPayloadAttributes(emptyMap())
    }
}
