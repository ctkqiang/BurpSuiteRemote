// 设备配对：签发一次性配对会话，并校验移动端回传的配对码。

package xin.ctkqiang.burpsuite.remote.security

import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairingCode
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.HexFormat
import java.util.concurrent.atomic.AtomicReference

/**
 * 配对服务：签发配对票据、校验移动端回传的配对码。
 *
 * 只做判定与登记，不碰连接与线程，所以能脱离 Burp 单测；会话靠一次原子交换消费，同一张票据只会成功一次。
 */
class DevicePairingService(
    private val localNetworkAddressResolver: LocalNetworkAddressResolver,
    private val pairedDeviceRegistry: PairedDeviceRegistry,
    private val clock: Clock,
    private val remotePort: Int = DEFAULT_REMOTE_PORT,
) {
    private val secureRandom = SecureRandom()

    private val pendingPairingSession = AtomicReference<PendingPairingSession?>(null)

    /**
     * 签发新的配对票据，并让它立刻顶掉上一张。
     *
     * 必须覆盖旧票据：屏幕上只显示一张二维码，旧票据若还在后台有效，就是一张操作者看不见的活凭证。
     */
    fun openPairingSession(): PairingTicket {
        val pairingCode = generatePairingCode()
        val challengeIdentifier = PairingChallengeIdentifier("challenge_${randomHexText(IDENTIFIER_BYTE_LENGTH)}")
        val expiresAt = clock.instant().plus(pairingValidity)

        // 直接覆盖而不排队：同一时刻只允许一张有效票据。
        pendingPairingSession.set(PendingPairingSession(challengeIdentifier, pairingCode, expiresAt))

        return PairingTicket(
            protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
            // 探测不到时写回环而不是留空：字段保持非空，操作者也一眼看得出地址不对。
            host = localNetworkAddressResolver.resolveAdvertisedHostAddress() ?: LOOPBACK_HOST_ADDRESS,
            port = remotePort,
            challengeIdentifier = challengeIdentifier,
            pairingCode = pairingCode,
            expiresAt = expiresAt,
        )
    }

    /**
     * 校验移动端提交的会话标识与配对码，成功时分配设备身份。
     *
     * 判定顺序（存在 → 未过期 → 标识一致 → 码一致）不能调换，调换就会让失效会话有机会配对成功。
     */
    fun redeemPairingCode(
        submittedChallengeIdentifier: PairingChallengeIdentifier,
        submittedPairingCode: PairingCode,
    ): PairingOutcome {
        val candidateSession =
            pendingPairingSession.get() ?: return PairingOutcome.Rejected(RejectionReason.PairingSessionNotAvailable)

        val rejectionReason =
            findRejectionReason(candidateSession, submittedChallengeIdentifier, submittedPairingCode)
        if (rejectionReason != null) {
            return PairingOutcome.Rejected(rejectionReason)
        }

        // 比较并交换就是「消费」：并发提交时只有一个能取走会话，先判断再删除会留下窗口。
        if (!pendingPairingSession.compareAndSet(candidateSession, null)) {
            return PairingOutcome.Rejected(RejectionReason.PairingSessionNotAvailable)
        }

        val deviceIdentifier = issueDeviceIdentity()
        // 先登记再返回：身份一离开本方法就可能被执行命令，中间不能有登记处查不到它的窗口。
        pairedDeviceRegistry.recordPairedDevice(deviceIdentifier, clock.instant())
        return PairingOutcome.Succeeded(deviceIdentifier)
    }

    private fun findRejectionReason(
        candidateSession: PendingPairingSession,
        submittedChallengeIdentifier: PairingChallengeIdentifier,
        submittedPairingCode: PairingCode,
    ): RejectionReason? {
        if (hasExpired(candidateSession)) {
            return RejectionReason.PairingSessionExpired
        }
        if (candidateSession.challengeIdentifier != submittedChallengeIdentifier) {
            return RejectionReason.PairingSessionNotAvailable
        }
        if (!matchesInConstantTime(candidateSession.pairingCode, submittedPairingCode)) {
            return RejectionReason.PairingCodeMismatch
        }
        return null
    }

    private fun hasExpired(pairingSession: PendingPairingSession): Boolean =
        clock.instant().isAfter(pairingSession.expiresAt)

    // 字符串相等会在首个不同字符处提前返回，可以逐位试探；isEqual 的耗时与内容无关。
    private fun matchesInConstantTime(
        expectedPairingCode: PairingCode,
        submittedPairingCode: PairingCode,
    ): Boolean =
        MessageDigest.isEqual(
            expectedPairingCode.value.toByteArray(Charsets.UTF_8),
            submittedPairingCode.value.toByteArray(Charsets.UTF_8),
        )

    // 去掉 I、L、O、0、1：人工抄写时几乎分不出它们，出错也找不到原因。
    private fun generatePairingCode(): PairingCode {
        val pairingCodeCharacters = CharArray(PAIRING_CODE_LENGTH)
        for (position in pairingCodeCharacters.indices) {
            pairingCodeCharacters[position] = PAIRING_CODE_ALPHABET[secureRandom.nextInt(PAIRING_CODE_ALPHABET.length)]
        }
        return PairingCode(String(pairingCodeCharacters))
    }

    // 身份由插件生成，不能让移动端自报，否则任何设备都能自称已配对的那一台。
    private fun issueDeviceIdentity(): DeviceIdentifier =
        DeviceIdentifier("device_${randomHexText(DEVICE_IDENTIFIER_BYTE_LENGTH)}")

    // 配对码与设备身份都在安全边界上，可预测的伪随机序列能被直接算出下一个值。
    private fun randomHexText(byteLength: Int): String {
        val randomBytes = ByteArray(byteLength)
        secureRandom.nextBytes(randomBytes)
        return HexFormat.of().formatHex(randomBytes)
    }

    private companion object {
        private const val PAIRING_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789"

        private const val PAIRING_CODE_LENGTH = 8

        // 五分钟：短到被旁人拍下也几乎没有利用价值，长到够拿起手机扫完。
        private val pairingValidity: Duration = Duration.ofMinutes(5)

        // 标识会进日志和审计记录，只求唯一，不必像凭证那样难以猜测。
        private const val IDENTIFIER_BYTE_LENGTH = 12

        private const val DEVICE_IDENTIFIER_BYTE_LENGTH = 16

        private const val LOOPBACK_HOST_ADDRESS = "127.0.0.1"
    }
}

// 只存判定所需的最小事实，不存设备信息——身份要在配对成功那一刻才分配。
private data class PendingPairingSession(
    val challengeIdentifier: PairingChallengeIdentifier,
    val pairingCode: PairingCode,
    val expiresAt: Instant,
)
