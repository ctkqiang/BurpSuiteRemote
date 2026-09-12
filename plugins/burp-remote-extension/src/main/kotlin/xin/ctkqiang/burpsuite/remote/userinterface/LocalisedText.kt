package xin.ctkqiang.burpsuite.remote.userinterface

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

// 不自建缓存：ResourceBundle 已由 JVM 缓存，再叠一层只会让「换了语言还显示旧文案」成为可能。
internal class LocalisedText(
    val locale: Locale,
    private val messages: ResourceBundle,
) {
    // 不走 MessageFormat：它会把单引号当转义吃掉，含撇号的文案会悄悄少一个字符。
    fun text(resourceKey: String): String = messages.getString(resourceKey)

    // 整句交给资源文件并显式绑定 locale：语序随语言变，默认语言环境还会把数字重新排版。
    fun format(
        resourceKey: String,
        vararg arguments: Any,
    ): String {
        val messageFormat = MessageFormat(messages.getString(resourceKey), locale)
        return messageFormat.format(arguments)
    }

    internal companion object {
        // 测试要拿它取同一批资源束比对键集合，因此不设为私有。
        const val MESSAGES_BUNDLE_BASE_NAME = "messages"

        // 显式传本类的类加载器：Burp 为每个扩展建独立类加载器，内部推断会让资源束从 Burp 自身类路径上找。
        // 同时关掉默认语言环境兜底，否则「请求英语」会在中文系统上命中中文资源，回落到英文也不再稳定。
        fun forLocale(locale: Locale): LocalisedText {
            val messages =
                ResourceBundle.getBundle(
                    MESSAGES_BUNDLE_BASE_NAME,
                    locale,
                    LocalisedText::class.java.classLoader,
                    ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT),
                )
            return LocalisedText(locale, messages)
        }
    }
}
