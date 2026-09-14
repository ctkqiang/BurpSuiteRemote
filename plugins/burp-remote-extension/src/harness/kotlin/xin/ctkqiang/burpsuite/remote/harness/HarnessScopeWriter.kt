// 夹具用的作用域写入器：没有 Burp 可改，写入换成一条可见的记录。

package xin.ctkqiang.burpsuite.remote.harness

import xin.ctkqiang.burpsuite.remote.adapter.BurpScopeWriter
import java.util.concurrent.CopyOnWriteArrayList

/** 夹具使用的作用域写入器；[includeHostInScope] 记录并打印被要求加入的主机，联调输出因此能看见这一步真的走到了。 */
class HarnessScopeWriter : BurpScopeWriter {
    private val recordedHostTexts = CopyOnWriteArrayList<String>()

    /** 已被要求加入作用域的主机地址，按写入顺序返回一份快照。 */
    val includedHostTexts: List<String>
        get() = recordedHostTexts.toList()

    override fun includeHostInScope(hostUrlText: String) {
        recordedHostTexts.add(hostUrlText)
        println("作用域：手机请求加入主机 $hostUrlText")
    }
}
