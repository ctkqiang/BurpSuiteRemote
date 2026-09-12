/**
 * 配对服务测试：票据内容、一次性消费、有效期、会话标识一致性与拒绝原因。
 * 时钟与网卡地址都是注入的，用例不启动 Burp、不碰网络，跑哪台机器结果都一样。
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

    // 翻转首位而不是写死字面量：写死的值撞上随机结果就是偶发失败
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

        // 超过有效期就行，整小时是随手挑的
        private val EXPIRED_BY: Duration = Duration.ofHours(1)
    }
}

// 时间交给用例显式推进，不然验证过期的用例要么真等五分钟，要么偶发失败
private class AdjustableClock(private var initialInstant: Instant) : Clock() {
    override fun instant(): Instant = initialInstant

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    fun advanceBy(duration: Duration) {
        initialInstant = initialInstant.plus(duration)
    }
}

// 地址写死，用例就不会随跑测机器的网卡状态变化
private class FixedLocalNetworkAddressResolver(private val advertisedHostAddress: String) :
    LocalNetworkAddressResolver {
    override fun resolveAdvertisedHostAddress(): String = advertisedHostAddress
}
