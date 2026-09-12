package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion

class RemoteResumeRequestTest {
    @Test
    fun `a resume request declares exactly the sequence number it was given`() {
        val resumeRequest = RemoteResumeRequest.build(SEQUENCE_NUMBER_ALREADY_RECEIVED)

        assertEquals(RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION, resumeRequest.protocolVersion)
        assertEquals(RemoteClientMessageType.Resume, resumeRequest.messageType)
        assertEquals(SEQUENCE_NUMBER_ALREADY_RECEIVED, resumeRequest.sequenceNumber)
        // 身份与配对字段属于握手的前两步与配对报文，续传不该把它们再带上。
        assertNull(resumeRequest.deviceIdentifier)
        assertNull(resumeRequest.challengeIdentifier)
        assertNull(resumeRequest.pairingCode)
    }

    @Test
    fun `a resume request is encoded with the message type the plugin matches on`() {
        val encoded =
            RemoteWireJson.instance.encodeToString(
                RemoteClientMessage.serializer(),
                RemoteResumeRequest.build(SEQUENCE_NUMBER_ALREADY_RECEIVED),
            )

        val encodedObject = RemoteWireJson.instance.parseToJsonElement(encoded).jsonObject
        assertEquals("resume", encodedObject["messageType"]?.jsonPrimitive?.content)
        assertEquals(SEQUENCE_NUMBER_ALREADY_RECEIVED, encodedObject["sequenceNumber"]?.jsonPrimitive?.long)
    }

    @Test
    fun `an empty journal resumes from sequence zero`() {
        assertEquals(0L, RemoteResumeRequest.build(EMPTY_JOURNAL_SEQUENCE_NUMBER).sequenceNumber)
    }

    private companion object {
        const val SEQUENCE_NUMBER_ALREADY_RECEIVED = 12L

        const val EMPTY_JOURNAL_SEQUENCE_NUMBER = 0L
    }
}
