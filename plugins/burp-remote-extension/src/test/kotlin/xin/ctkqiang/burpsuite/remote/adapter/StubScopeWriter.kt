// 测试替身：记录被写入作用域的主机地址。

package xin.ctkqiang.burpsuite.remote.adapter

/** 记录写入内容的作用域写入替身；测试只断言「写没写、写了什么」，不构造任何 Montoya 实现类（rules.md §13）。 */
class StubScopeWriter : BurpScopeWriter {
    private val recordedHostTexts = mutableListOf<String>()

    /** 已写入作用域的主机地址，按写入顺序返回一份快照。 */
    val includedHostTexts: List<String>
        get() = recordedHostTexts.toList()

    override fun includeHostInScope(hostUrlText: String) {
        recordedHostTexts.add(hostUrlText)
    }
}
