/**
 * Burp Remote —— 界面层 / 本地化
 *
 * 按操作系统语言解析插件界面的文案。界面语言由操作系统的语言设置决定，因此本文件唯一
 * 的输入是一个 [java.util.Locale]，而不是任何硬编码的语言常量。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

/**
 * 插件界面文案的查询入口。
 *
 * 资源束以 `messages` 为基准名，随 JAR 一同分发：
 *
 * - `messages.properties` —— 英文，基础资源束；
 * - `messages_zh.properties` —— 简体中文；
 * - `messages_de.properties` —— 德语。
 *
 * 未提供翻译的语言（例如系统语言为法语）会由 JVM 自动回落到基础资源束的英文，
 * 而不是显示资源键或空白，因此新增语言只需增加一份资源文件。
 *
 * 属性文件由 JDK 9 起的 `ResourceBundle` 以 UTF-8 读取，中文与德语变音符号因此可以直接
 * 写原文；但这也意味着不能改用 `Properties.load` 去读同一份文件——那个入口仍按
 * ISO-8859-1 解码，同一份资源会被读成乱码。
 *
 * 本类型不缓存已解析的结果：`ResourceBundle` 自身由 JVM 缓存，重复取用的成本可以忽略，
 * 而自己再叠一层缓存只会让「换了语言却仍显示旧文案」变成可能。
 *
 * @param locale 期望的界面语言。它与实际命中的资源束可能不同（回落到英文时就是如此），
 *   保留期望值是为了让时间、数字等格式仍按操作者的语言习惯呈现。
 * @param messages 已按 [locale] 解析好的资源束。
 */
internal class LocalisedText(
    val locale: Locale,
    private val messages: ResourceBundle,
) {
    /**
     * 取一段不需要参数替换的文案。
     *
     * 刻意不走 [MessageFormat]：`MessageFormat` 会把单引号当作转义字符吃掉，一段本应原样
     * 显示的文案（例如含撇号的英文句子）会因此悄悄少掉一个字符，而这种缺陷极难在评审中被
     * 发现。
     *
     * @param resourceKey 资源键，取值来自 [TextKey]。
     * @return 当前语言下的文案。
     */
    fun text(resourceKey: String): String = messages.getString(resourceKey)

    /**
     * 取一段需要参数替换的文案。
     *
     * 参数一律使用位置形式（`{0}`、`{1}`），而不是在代码里拼字符串：语序在不同语言之间
     * 会变，中文的「剩余 4 分 12 秒」在德语里是另一个词序，只有把整句交给资源文件，
     * 译者才有调整语序的余地。
     *
     * 格式化显式绑定 [locale]，而不是用 `MessageFormat` 的默认语言环境：默认语言环境会
     * 把数字按系统语言重新排版（某些语言下连数字字形都不同），于是同一段文案在开发机上
     * 与用户机器上显示不一致，测试也无法稳定复现。
     *
     * @param resourceKey 资源键，取值来自 [TextKey]。
     * @param arguments 按位置填充的参数。
     * @return 当前语言下已完成替换的文案。
     */
    fun format(
        resourceKey: String,
        vararg arguments: Any,
    ): String {
        val messageFormat = MessageFormat(messages.getString(resourceKey), locale)
        return messageFormat.format(arguments)
    }

    internal companion object {
        /**
         * 资源束的基准名，对应 `src/main/resources/messages*.properties` 的前缀。
         *
         * 测试需要用它取同一批资源束来比对键集合，因此不设为私有。
         */
        const val MESSAGES_BUNDLE_BASE_NAME = "messages"

        /**
         * 按给定语言解析资源束。
         *
         * 显式传入本类的类加载器，而不是依赖 `ResourceBundle.getBundle` 内部的「调用者
         * 类加载器」推断：Burp 会为每个扩展建立独立的类加载器，一旦推断落在别处，资源束
         * 就会从 Burp 自身的类路径上找，表现为插件界面全是资源键。
         *
         * 同时显式关闭「默认语言环境兜底」，理由有两条：
         *
         * 一是确定性。默认候选链会把 JVM 的默认语言环境插进目标语言与基础资源束之间，于是
         * 「请求英语」在一个系统语言为中文的机器上会命中中文资源——因为 `messages_en` 并不
         * 存在，候选链先撞上兜底语言，根本轮不到基础资源束。同一份代码在开发机与用户机上
         * 显示不同语言，这类缺陷几乎无法复现。
         * 二是可预期。关掉兜底之后候选链只剩「目标语言 → 其上级语言 → 基础资源束」，
         * 未提供翻译的语言稳定回落到英文，也就是 rules.md §9 声明的默认语言。
         *
         * @param locale 期望的界面语言。通常来自操作系统的语言设置。
         * @return 面向该语言的查询入口。
         * @throws java.util.MissingResourceException 连基础资源束都找不到时抛出，即 JAR
         *   缺失自身资源。这属于打包缺陷，应当让扩展加载直接失败，而不是让界面显示键名。
         */
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
