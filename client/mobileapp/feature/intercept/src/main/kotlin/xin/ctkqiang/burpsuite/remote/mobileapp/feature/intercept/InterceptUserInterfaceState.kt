package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord

/**
 * 拦截队列状态（plan §43 的 Live/Intercept）。
 *
 * 列表只带元数据：报文本体按标识另取，队列长起来时列表不该为每一行加载大报文（plan §20）。
 */
data class InterceptUserInterfaceState(
    /** 队列里的拦截项，按事件序号升序。 */
    val records: List<InterceptRecord> = emptyList(),
)
