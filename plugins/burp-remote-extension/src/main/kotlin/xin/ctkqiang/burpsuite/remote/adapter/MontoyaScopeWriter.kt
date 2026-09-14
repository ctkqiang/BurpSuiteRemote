// Montoya 版作用域写入实现。

package xin.ctkqiang.burpsuite.remote.adapter

import burp.api.montoya.scope.Scope

/**
 * 用 Montoya 的作用域实现写入端口，是全仓唯一触碰 `includeInScope` 的地方。
 *
 * 只做主机级写入：Burp 的作用域规则按前缀匹配，加入 `https://host` 即覆盖该主机下的全部路径，
 * 因此不需要（也不应该）逐条路径写规则。
 */
class MontoyaScopeWriter(private val scope: Scope) : BurpScopeWriter {
    override fun includeHostInScope(hostUrlText: String) {
        scope.includeInScope(hostUrlText)
    }
}
