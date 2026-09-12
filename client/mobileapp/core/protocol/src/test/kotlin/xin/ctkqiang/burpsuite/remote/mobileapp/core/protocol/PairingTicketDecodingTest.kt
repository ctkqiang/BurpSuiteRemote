package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode
import java.time.Instant

class PairingTicketDecodingTest {
    @Test
    fun `a ticket encoded by the extension decodes field by field`() {
        val pairingTicket = PairingTicketDecoder.decodeFromText(EXTENSION_ENCODED_TICKET)

        assertEquals(RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION, pairingTicket.protocolVersion)
        assertEquals(ADVERTISED_HOST_ADDRESS, pairingTicket.host)
        assertEquals(DEFAULT_REMOTE_PORT, pairingTicket.port)
        assertEquals(PAIRING_CHALLENGE_IDENTIFIER, pairingTicket.challengeIdentifier)
        assertEquals(PAIRING_CODE, pairingTicket.pairingCode)
        assertEquals(EXPIRES_AT, pairingTicket.expiresAt)
    }

    @Test
    fun `the fixed sample is byte identical to what the extension encoder emits`() {
        val pairingTicket =
            PairingTicket(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                host = ADVERTISED_HOST_ADDRESS,
                port = DEFAULT_REMOTE_PORT,
                challengeIdentifier = PAIRING_CHALLENGE_IDENTIFIER,
                pairingCode = PAIRING_CODE,
                expiresAt = EXPIRES_AT,
            )

        val encodedTicket = json.encodeToString(PairingTicket.serializer(), pairingTicket)

        assertEquals(EXTENSION_ENCODED_TICKET, encodedTicket)
    }

    @Test
    fun `a text that is not a ticket is rejected instead of guessed`() {
        assertThrows<SerializationException> { PairingTicketDecoder.decodeFromText("not-a-ticket") }
    }

    private companion object {
        private val json = Json

        private const val ADVERTISED_HOST_ADDRESS = "192.168.1.10"

        private const val EXPIRES_AT_EPOCH_MILLISECONDS = 1757660000000L

        private val EXPIRES_AT: Instant = Instant.ofEpochMilli(EXPIRES_AT_EPOCH_MILLISECONDS)

        private val PAIRING_CHALLENGE_IDENTIFIER = PairingChallengeIdentifier("challenge_01JABC")

        private val PAIRING_CODE = PairingCode("482913")

        // 插件侧 PairingTicketEncoder 产出的就是这一行；分多行只为读得清，比较前先把换行去掉
        private val EXTENSION_ENCODED_TICKET =
            """
            {"protocolVersion":1,"host":"$ADVERTISED_HOST_ADDRESS","port":$DEFAULT_REMOTE_PORT,
            "challengeIdentifier":"challenge_01JABC","pairingCode":"482913",
            "expiresAt":$EXPIRES_AT_EPOCH_MILLISECONDS}
            """.trimIndent().replace("\n", "")
    }
}
