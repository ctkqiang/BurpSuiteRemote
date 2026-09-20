package xin.ctkqiang.burpsuite.remote.mobileapp.ui.localisation

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** 已经构造过的格式化器。构造要查语言数据，列表里每条记录都重建一次没必要。 */
private val FORMATTER_CACHE = ConcurrentHashMap<String, DateTimeFormatter>()

/**
 * 按当前默认语言取一个本地化时间格式化器。
 *
 * 不能写成文件级 `val` 缓存：那会在首次访问时按当时的 [Locale.getDefault] 构造，并随类加载器一起
 * 永久留下，于是换语言后界面文案换了、日期时间却还停在旧语言。这里按「样式 + 时区 + 语言」记忆，
 * 语言一变自然换用新的一份。
 */
fun localisedDateTimeFormatter(
    style: FormatStyle,
    zone: ZoneId = ZoneId.systemDefault(),
): DateTimeFormatter {
    val locale = Locale.getDefault()
    return FORMATTER_CACHE.computeIfAbsent("$style|$zone|${locale.toLanguageTag()}") {
        DateTimeFormatter.ofLocalizedDateTime(style).withLocale(locale).withZone(zone)
    }
}
