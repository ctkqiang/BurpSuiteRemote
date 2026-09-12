// 一台已完成配对的设备。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 一台已与插件完成配对的设备。这是状态快照，回答「此刻该拒绝谁」。
 *
 * @property deviceIdentifier 该设备的身份。
 * @property pairedAt 配对完成时刻，毫秒整数。
 */
@Serializable
data class PairedDevice(
    val deviceIdentifier: DeviceIdentifier,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val pairedAt: Instant,
)
