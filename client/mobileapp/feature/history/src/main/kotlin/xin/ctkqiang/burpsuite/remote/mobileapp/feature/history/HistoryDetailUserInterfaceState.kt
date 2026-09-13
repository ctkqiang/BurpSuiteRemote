package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteHistoryMessage

/**
 * 历史详情状态（plan §20、plan §22）。
 *
 * 元数据与报文本体是两条独立的路：元数据来自本机投影，读得很快；本体要向插件按标识取回，
 * 可能失败。因此它们各自带一个「读过了没有」的标记，界面才能分别画骨架、缺口与内容。
 *
 * [hasLoaded] 把元数据那侧的「还没读出来」和「读出来是空」分开：仓储在记录不存在时发 null，
 * 不区分这两件事的话界面会把「不存在」画成一直转圈。
 *
 * 本体那一侧不需要额外的「读过了」标记：[message] 与 [messageFailure] 同时为空就是还在取，
 * 读到就是前者非空，读失败就是后者非空——三种处境互斥且穷尽，多一个布尔量只会多一处
 * 可能与事实不一致的地方。
 *
 * @property record 这条记录的元数据；尚未读到或不存在时为空。
 * @property hasLoaded 元数据是否已经从仓库读到过一次结果。
 * @property message 报文本体；尚未取到或取失败时为空。
 * @property messageFailure 本体读取失败的归类；成功或尚在读取时为空。
 */
data class HistoryDetailUserInterfaceState(
    val record: HistoryRecord? = null,
    val hasLoaded: Boolean = false,
    val message: RemoteHistoryMessage? = null,
    val messageFailure: HistoryMessageReadFailure? = null,
)
