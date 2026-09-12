package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 用内容解析器写导出包。写盘放到注入的调度器上，不占主线程（rules.md §7.5）。 */
class ContentResolverExportDestinationWriter(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExportDestinationWriter {
    override suspend fun write(
        destinationUri: Uri,
        bundleBytes: ByteArray,
    ) {
        withContext(ioDispatcher) {
            val outputStream =
                requireNotNull(context.contentResolver.openOutputStream(destinationUri)) {
                    "无法打开导出目标：" + destinationUri
                }
            outputStream.use { stream -> stream.write(bundleBytes) }
        }
    }
}
