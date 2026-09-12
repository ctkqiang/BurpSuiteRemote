/**
 * Burp Remote —— 界面层 / 二维码
 *
 * 把配对票据变成二维码模块矩阵。编码规则来自协议层，绘制交给界面层：本文件只负责
 * 「票据文本到矩阵」这一步，因此它既不认识 Swing，也不认识网络。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicketEncoder

/**
 * 把配对票据编码为二维码模块矩阵。
 *
 * 明确要求「一像素一模块、且不带静默区」：ZXing 默认会把结果放大到调用者指定的尺寸并
 * 补齐静默区，那种矩阵一旦再被界面缩放一次，模块边缘就会落在半个像素上，放大后是灰边，
 * 扫描器会把它当成噪声。这里只取最原始的模块矩阵，缩放与静默区全部交给绘制层处理，
 * 于是任何缩放下模块都是实心方块。
 */
internal object PairingQrCodeRenderer {
    /**
     * 编码提示。
     *
     * 静默区设为 0 是因为绘制层会自行留白（见 `PairingQrCodeView`）；字符集显式声明为
     * UTF-8，而不是依赖二维码默认的 ISO-8859-1：当前票据的字段都是 ASCII，但一旦将来
     * 加入含非 ASCII 字符的字段，默认字符集会把它写成乱码，而乱码在扫描成功的假象下
     * 更难被发现。
     */
    private val encodingHints: Map<EncodeHintType, Any> =
        mapOf(
            EncodeHintType.MARGIN to NO_QUIET_ZONE,
            EncodeHintType.CHARACTER_SET to TEXT_ENCODING,
        )

    /**
     * 把票据编码为二维码模块矩阵。
     *
     * @param pairingTicket 待编码的配对票据。
     * @return 只含模块的矩阵，深色模块为真，尺寸为二维码规范允许的最小版本。
     * @throws com.google.zxing.WriterException 当票据内容超出二维码容量上限时抛出。
     *   当前票据远小于上限，因此这属于代码缺陷而非运行时状况，此处不做兜底。
     */
    fun render(pairingTicket: PairingTicket): BitMatrix =
        QRCodeWriter().encode(
            PairingTicketEncoder.encodeToText(pairingTicket),
            BarcodeFormat.QR_CODE,
            MINIMAL_MATRIX_SIDE,
            MINIMAL_MATRIX_SIDE,
            encodingHints,
        )

    /**
     * 请求尺寸。传入最小值即可：真正的缩放发生在绘制层，而 ZXing 会把小于二维码自身
     * 尺寸的请求向上补齐，因此这里得到的永远是原始模块矩阵。
     */
    private const val MINIMAL_MATRIX_SIDE = 1

    private const val NO_QUIET_ZONE = 0

    private const val TEXT_ENCODING = "UTF-8"
}
