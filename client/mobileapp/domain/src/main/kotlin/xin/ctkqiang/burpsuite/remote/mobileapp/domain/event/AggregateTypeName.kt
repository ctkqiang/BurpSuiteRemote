package xin.ctkqiang.burpsuite.remote.mobileapp.domain.event

/**
 * 聚合类型的线上名字。
 *
 * 用值类型而不是枚举：新版插件可能引入客户端还不认识的聚合类型，那时事件必须能原样保留。
 * 取值由协议模块拥有，领域层只当它是文本。
 *
 * @property value 点分或下划线形式的类型串，例如 history。
 */
@JvmInline
value class AggregateTypeName(val value: String)
