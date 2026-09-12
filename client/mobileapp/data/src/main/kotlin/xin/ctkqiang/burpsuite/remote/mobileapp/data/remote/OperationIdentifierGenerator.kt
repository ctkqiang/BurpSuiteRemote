package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import java.util.UUID

/**
 * 控制命令的操作标识来源。
 *
 * 每条命令都必须带一个操作标识，插件据此保证幂等（rules.md §5.5）；生成方式因此可注入，
 * 测试才能拿到确定的值，而不是每次都撞见一个新的随机串。
 */
fun interface OperationIdentifierGenerator {
    /** 生成一个全新标识；同一个标识只允许产生一次副作用，重试必须复用原值。 */
    fun generate(): OperationIdentifier

    companion object {
        /** 默认实现：随机 UUID。标识符格式原文未定义，此处选定——它的碰撞概率可忽略，且不需要两端对表。 */
        fun random(): OperationIdentifierGenerator =
            OperationIdentifierGenerator { OperationIdentifier(UUID.randomUUID().toString()) }
    }
}
