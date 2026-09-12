/**
 * 二维码渲染测试：把矩阵交回解码器，要求还原出与票据文本逐字节相同的字符串。
 * 只断言「矩阵非空」不够——结构错或内容被截断的二维码同样是「生成了」，到真机上只是扫不出来。
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairingCode
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicketEncoder
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import java.awt.Color
import java.time.Instant

class PairingQrCodeRendererTest {
    @Test
    fun `the rendered qr code decodes back to the exact pairing ticket text`() {
        val pairingTicket = createPairingTicket()

        val qrCodeModules = PairingQrCodeRenderer.render(pairingTicket)

        assertEquals(PairingTicketEncoder.encodeToText(pairingTicket), decodeQrCode(qrCodeModules))
    }

    @Test
    fun `the rendered matrix is a square that starts at the first code module`() {
        val qrCodeModules = PairingQrCodeRenderer.render(createPairingTicket())

        assertEquals(qrCodeModules.width, qrCodeModules.height)
        // 左上角必然是深色角点；若不是，说明矩阵上游被补过边，绘制层会再补一次静默区、图案白变小
        assertTrue(qrCodeModules.get(0, 0))
    }

    // 真机上前有静默区、每个模块占多个像素，不补齐就是拿远低于摄像头的分辨率去解码
    private fun decodeQrCode(qrCodeModules: BitMatrix): String {
        val whitePixel = Color.WHITE.rgb
        val blackPixel = Color.BLACK.rgb
        val matrixSide = qrCodeModules.width + QUIET_ZONE_MODULES * QUIET_ZONE_SIDES_PER_AXIS
        val pixelSide = matrixSide * PIXELS_PER_MODULE
        val pixels = IntArray(pixelSide * pixelSide) { whitePixel }

        for (rowIndex in 0 until qrCodeModules.height) {
            for (columnIndex in 0 until qrCodeModules.width) {
                if (qrCodeModules.get(columnIndex, rowIndex)) {
                    fillModule(pixels, pixelSide, rowIndex, columnIndex, blackPixel)
                }
            }
        }

        val luminanceSource = RGBLuminanceSource(pixelSide, pixelSide, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(luminanceSource))).text
    }

    private fun fillModule(
        pixels: IntArray,
        pixelSide: Int,
        moduleRow: Int,
        moduleColumn: Int,
        modulePixel: Int,
    ) {
        val firstPixelRow = (moduleRow + QUIET_ZONE_MODULES) * PIXELS_PER_MODULE
        val firstPixelColumn = (moduleColumn + QUIET_ZONE_MODULES) * PIXELS_PER_MODULE
        for (pixelRow in firstPixelRow until firstPixelRow + PIXELS_PER_MODULE) {
            for (pixelColumn in firstPixelColumn until firstPixelColumn + PIXELS_PER_MODULE) {
                pixels[pixelRow * pixelSide + pixelColumn] = modulePixel
            }
        }
    }

    private fun createPairingTicket(): PairingTicket =
        PairingTicket(
            protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
            host = "192.168.1.12",
            port = DEFAULT_REMOTE_PORT,
            challengeIdentifier = PairingChallengeIdentifier("challenge_0f3a9c2b7d41"),
            pairingCode = PairingCode("K7QM2XTP"),
            expiresAt = Instant.parse("2026-01-01T00:05:00Z"),
        )

    private companion object {
        private const val QUIET_ZONE_MODULES = 4

        private const val QUIET_ZONE_SIDES_PER_AXIS = 2

        // 真机上模块占多个物理像素，给解码器「1 像素 1 模块」会解不出来
        private const val PIXELS_PER_MODULE = 4
    }
}
