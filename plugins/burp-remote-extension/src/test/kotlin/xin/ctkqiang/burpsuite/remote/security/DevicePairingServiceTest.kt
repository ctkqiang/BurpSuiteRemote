/**
 * Burp Remote —— 安全层 / 配对服务测试
 *
 * 验证配对服务的判定逻辑：票据内容、一次性消费、有效期、会话标识一致性与拒绝原因。
 * 用例不启动 Burp，也不触碰网络——网卡探测与时钟都是注入的，因此结果在任何机器上都一致。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairingCode
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DevicePairingServiceTest {
    @Test
    fun `a new pairing ticket carries the protocol version and the advertised address`() {
        val pairingService = createPairingService()

        val pairingTicket = pairingService.openPairingSession()

        assertEquals(RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION, pairingTicket.protocolVersion)
        assertEquals(ADVERTISED_HOST_ADDRESS, pairingTicket.host)
        assertEquals(DEFAULT_REMOTE_PORT, pairingTicket.port)
        assertTrue(pairingTicket.expiresAt.isAfter(TEST_INSTANT))
    }

    @Test
    fun `redeeming the pairing code once yields a device identity`() {
        val pairingService = createPairingService()
        val pairingTicket = pairingService.openPairingSession()

        val outcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = pairingTicket.challengeIdentifier,
                submittedPairingCode = pairingTicket.pairingCode,
            )

        val succeededOutcome = outcome as PairingOutcome.Succeeded
        assertTrue(succeededOutcome.deviceIdentifier.value.startsWith("device_"))
    }

    @Test
    fun `the same pairing code cannot be redeemed twice`() {
        val pairingService = createPairingService()
        val pairingTicket = pairingService.openPairingSession()
        pairingService.redeemPairingCode(pairingTicket.challengeIdentifier, pairingTicket.pairingCode)

        val secondOutcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = pairingTicket.challengeIdentifier,
                submittedPairingCode = pairingTicket.pairingCode,
            )

        assertEquals(PairingOutcome.Rejected(RejectionReason.PairingSessionNotAvailable), secondOutcome)
    }

    @Test
    fun `a pairing code is rejected once the pairing session expired`() {
        val adjustableClock = AdjustableClock(TEST_INSTANT)
        val pairingService = createPairingService(adjustableClock)
        val pairingTicket = pairingService.openPairingSession()
        adjustableClock.advanceBy(EXPIRED_BY)

        val outcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = pairingTicket.challengeIdentifier,
                submittedPairingCode = pairingTicket.pairingCode,
            )

        assertEquals(PairingOutcome.Rejected(RejectionReason.PairingSessionExpired), outcome)
    }

    @Test
    fun `a submitted pairing code that does not match is rejected without consuming the session`() {
        val pairingService = createPairingService()
        val pairingTicket = pairingService.openPairingSession()

        val rejectedOutcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = pairingTicket.challengeIdentifier,
                submittedPairingCode = PairingCode(otherPairingCode(pairingTicket.pairingCode)),
            )

        assertEquals(PairingOutcome.Rejected(RejectionReason.PairingCodeMismatch), rejectedOutcome)

        val acceptedOutcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = pairingTicket.challengeIdentifier,
                submittedPairingCode = pairingTicket.pairingCode,
            )

        assertTrue(acceptedOutcome is PairingOutcome.Succeeded)
    }

    @Test
    fun `a stale challenge identifier is rejected even when the pairing code matches`() {
        val pairingService = createPairingService()
        val pairingTicket = pairingService.openPairingSession()

        val outcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = PairingChallengeIdentifier("challenge_stale"),
                submittedPairingCode = pairingTicket.pairingCode,
            )

        assertEquals(PairingOutcome.Rejected(RejectionReason.PairingSessionNotAvailable), outcome)
    }

    @Test
    fun `opening a new pairing session invalidates the previous pairing ticket`() {
        val pairingService = createPairingService()
        val previousTicket = pairingService.openPairingSession()
        val currentTicket = pairingService.openPairingSession()

        assertNotEquals(previousTicket.pairingCode, currentTicket.pairingCode)

        val outcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = previousTicket.challengeIdentifier,
                submittedPairingCode = previousTicket.pairingCode,
            )

        assertEquals(PairingOutcome.Rejected(RejectionReason.PairingSessionNotAvailable), outcome)
    }

    @Test
    fun `a successful pairing registers the device as a paired device`() {
        val pairedDeviceRegistry = PairedDeviceRegistry()
        val pairingService = createPairingService(pairedDeviceRegistry = pairedDeviceRegistry)
        val pairingTicket = pairingService.openPairingSession()

        val outcome =
            pairingService.redeemPairingCode(
                submittedChallengeIdentifier = pairingTicket.challengeIdentifier,
                submittedPairingCode = pairingTicket.pairingCode,
            )

        val succeededOutcome = outcome as PairingOutcome.Succeeded
        val pairedDevices = pairedDeviceRegistry.snapshot()
        assertEquals(1, pairedDevices.size)
        assertEquals(succeededOutcome.deviceIdentifier, pairedDevices.single().deviceIdentifier)
        assertEquals(TEST_INSTANT, pairedDevices.single().pairedAt)
    }

    @Test
    fun `a rejected pairing registers no device`() {
        val pairedDeviceRegistry = PairedDeviceRegistry()
        val pairingService = createPairingService(pairedDeviceRegistry = pairedDeviceRegistry)
        val pairingTicket = pairingService.openPairingSession()

        pairingService.redeemPairingCode(
            submittedChallengeIdentifier = pairingTicket.challengeIdentifier,
            submittedPairingCode = PairingCode(otherPairingCode(pairingTicket.pairingCode)),
        )

        assertTrue(pairedDeviceRegistry.snapshot().isEmpty())
    }

    /**
     * 造出一个与给定配对码不同的配对码。
     *
     * 翻转首位字符而不是写死一个字面量：写死的取值一旦恰好等于随机结果，用例就会偶发失败，
     * 而那种失败与本用例要验证的规则毫无关系。
     */
    private fun otherPairingCode(pairingCode: PairingCode): String {
        val characters = pairingCode.value.toCharArray()
        characters[0] = if (characters[0] == ALTERNATIVE_CHARACTER) ORIGINAL_CHARACTER else ALTERNATIVE_CHARACTER
        return String(characters)
    }

    private fun createPairingService(
        clock: Clock = AdjustableClock(TEST_INSTANT),
        pairedDeviceRegistry: PairedDeviceRegistry = PairedDeviceRegistry(),
    ): DevicePairingService =
        DevicePairingService(
            localNetworkAddressResolver = FixedLocalNetworkAddressResolver(ADVERTISED_HOST_ADDRESS),
            pairedDeviceRegistry = pairedDeviceRegistry,
            clock = clock,
        )

    private companion object {
        private const val ADVERTISED_HOST_ADDRESS = "192.168.1.12"

        private const val ORIGINAL_CHARACTER = 'A'

        private const val ALTERNATIVE_CHARACTER = 'B'

        private val TEST_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")

        /** 超出配对有效期，用于验证过期之后的拒绝行为。 */
        private val EXPIRED_BY: Duration = Duration.ofHours(1)
    }
}

/**
 * 可推进的测试时钟。
 *
 * 用例绝不能依赖真实时间：一旦依赖，验证过期的用例就只能真的等待五分钟，或者在某台机器上
 * 偶发失败，而失败原因与被测逻辑毫无关系。这里让时间由测试显式推进，判定因此完全确定。
 *
 * @param initialInstant 起始时间点。
 */
private class AdjustableClock(private var initialInstant: Instant) : Clock() {
    override fun instant(): Instant = initialInstant

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    /**
     * 把时钟向前推进指定时长。
     *
     * @param duration 推进的时长。
     */
    fun advanceBy(duration: Duration) {
        initialInstant = initialInstant.plus(duration)
    }
}

/**
 * 返回固定地址的地址解析器，使配对逻辑不依赖运行测试那台机器的网卡状态。
 *
 * @param advertisedHostAddress 每次解析都返回的地址。
 */
private class FixedLocalNetworkAddressResolver(private val advertisedHostAddress: String) :
    LocalNetworkAddressResolver {
    override fun resolveAdvertisedHostAddress(): String = advertisedHostAddress
}
