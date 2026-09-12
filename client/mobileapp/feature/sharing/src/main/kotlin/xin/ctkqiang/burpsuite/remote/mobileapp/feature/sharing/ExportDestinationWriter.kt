package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import android.net.Uri

/**
 * 导出副本的写入位置。
 *
 * 位置由用户在系统文件选择器里指定，因此这一层只认 Uri，不关心是本地存储还是云盘。
 */
fun interface ExportDestinationWriter {
    /** 把导出包写到指定位置；失败时抛异常，由调用方翻译成界面上的一句话。 */
    suspend fun write(
        destinationUri: Uri,
        bundleBytes: ByteArray,
    )
}
