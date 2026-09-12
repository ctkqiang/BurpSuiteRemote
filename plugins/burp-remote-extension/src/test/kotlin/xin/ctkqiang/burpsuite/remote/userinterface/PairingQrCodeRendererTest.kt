/**
 * Burp Remote —— 界面层 / 二维码渲染测试
 *
 * 验证二维码能把票据完整地编码进去、并原始地解码回来。只断言「矩阵非空」是不够的：一张
 * 结构错误或内容被截断的二维码同样是「生成了」，而它到真机上只会表现为扫不出来。
 * 因此这里把矩阵交回给解码器，要求它还原出与票据文本逐字节相同的字符串。
 *
 * @author 钟智强
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
        // 二维码左上角必定是定位图案的深色角点。它一旦不是深色，就说明矩阵上游被补过边：
        // 那样绘制层会再留一次静默区，图案因此变小，等于白白牺牲可扫描性。
        assertTrue(qrCodeModules.get(0, 0))
    }

    /**
     * 把模块矩阵还原成扫描器真正看到的黑白位图并解码。
     *
     * 有两处必须按真机画面补齐，否则用例验证的就不是扫描器会遇到的那张图：
     * 一是静默区——渲染器输出的矩阵刻意不含它，而绘制层会补上（见 `PairingQrCodeView`）；
     * 二是模块尺寸——屏幕上每个模块占据多个物理像素，把「1 像素 1 模块」交给解码器，
     * 等于要求它在远低于任何真实摄像头的分辨率下工作。
     */
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

    /**
     * 把其中一个模块涂成给定的像素值。
     *
     * @param pixels 目标像素缓冲区，按行优先排列。
     * @param pixelSide 位图的边长，单位为像素。
     * @param moduleRow 模块所在行。
     * @param moduleColumn 模块所在列。
     * @param modulePixel 该模块对应的像素值。
     */
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

        /** 每个模块在解码输入中占据的像素边长，用于模拟真机上的模块尺寸。 */
        private const val PIXELS_PER_MODULE = 4
    }
}
