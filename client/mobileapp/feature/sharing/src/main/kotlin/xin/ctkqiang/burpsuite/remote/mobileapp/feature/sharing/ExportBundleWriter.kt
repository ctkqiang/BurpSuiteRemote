package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 导出包组装（plan §25）。
 *
 * 打成 ZIP，只写这一次导出真正拥有的东西：清单、脱敏报告、以及一段说明导出范围的文字。
 * 归档本体不参与组装，也就不可能被改写（rules.md §12）。
 */
object ExportBundleWriter {
    /** 清单在包里的文件名。 */
    const val MANIFEST_ENTRY_NAME: String = "manifest.json"

    /** 脱敏报告在包里的文件名。 */
    const val REDACTION_REPORT_ENTRY_NAME: String = "redaction-report.json"

    /** 说明导出范围的文本在包里的文件名。 */
    const val NOTICE_ENTRY_NAME: String = "notice.txt"

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    /** 组装导出包；返回的字节可以直接写进用户选定的位置。 */
    fun build(
        manifest: ExportManifest,
        redactionReport: ExportRedactionReport,
        noticeText: String,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { bundle ->
            bundle.writeEntry(MANIFEST_ENTRY_NAME, json.encodeToString(ExportManifest.serializer(), manifest))
            bundle.writeEntry(
                REDACTION_REPORT_ENTRY_NAME,
                json.encodeToString(ExportRedactionReport.serializer(), redactionReport),
            )
            bundle.writeEntry(NOTICE_ENTRY_NAME, noticeText)
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.writeEntry(
        entryName: String,
        content: String,
    ) {
        putNextEntry(ZipEntry(entryName))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
