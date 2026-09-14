// 写入 Burp 作用域的端口。

package xin.ctkqiang.burpsuite.remote.adapter

/** Burp 作用域的写入端口；适配器只依赖它，映射逻辑因此不依赖 Montoya。 */
interface BurpScopeWriter {
    /** 把某个主机地址加入作用域；主机一旦入域，其下全部路径随之入域。 */
    fun includeHostInScope(hostUrlText: String)
}
