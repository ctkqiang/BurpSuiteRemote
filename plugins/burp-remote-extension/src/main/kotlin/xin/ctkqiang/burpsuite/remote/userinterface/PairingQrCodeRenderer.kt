package xin.ctkqiang.burpsuite.remote.userinterface

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicketEncoder

// 只取原始模块矩阵：ZXing 若按调用尺寸放大或补静默区，模块边缘会落到半像素上，缩放后成灰边。
internal object PairingQrCodeRenderer {
    // 静默区交给绘制层留白；字符集显式声明 UTF-8，不依赖二维码默认的 ISO-8859-1。
    private val encodingHints: Map<EncodeHintType, Any> =
        mapOf(
            EncodeHintType.MARGIN to NO_QUIET_ZONE,
            EncodeHintType.CHARACTER_SET to TEXT_ENCODING,
        )

    fun render(pairingTicket: PairingTicket): BitMatrix =
        QRCodeWriter().encode(
            PairingTicketEncoder.encodeToText(pairingTicket),
            BarcodeFormat.QR_CODE,
            MINIMAL_MATRIX_SIDE,
            MINIMAL_MATRIX_SIDE,
            encodingHints,
        )

    // 传 1 即可：ZXing 会把小于二维码自身尺寸的请求向上补齐，返回的始终是原始模块矩阵。
    private const val MINIMAL_MATRIX_SIDE = 1

    private const val NO_QUIET_ZONE = 0

    private const val TEXT_ENCODING = "UTF-8"
}
