// 日志级别与分类在界面上的呈现。

package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogSeverity
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme

/**
 * 日志级别到颜色。
 *
 * 四档各给一个颜色，但不把颜色当成唯一线索：级别标签始终与颜色一起画出来，
 * 色觉不同的读者读标签即可（Apple HIG 明确要求不以颜色作为唯一的信息载体）。
 *
 * 调试档刻意用次要字色而不是另取一色：它本来就该退到后面，抢眼反而会让高频的调试行
 * 盖住真正要看的那几条。
 *
 * @param colourScheme 当前主题的配色。
 * @return 该级别对应的颜色。
 */
fun TechnicalLogSeverity.colourIn(colourScheme: BurpRemoteColourScheme): Color =
    when (this) {
        TechnicalLogSeverity.Debug -> colourScheme.contentSecondary
        TechnicalLogSeverity.Information -> colourScheme.information
        TechnicalLogSeverity.Warning -> colourScheme.warning
        TechnicalLogSeverity.Error -> colourScheme.danger
    }

/** 级别标签的文案资源。 */
@get:StringRes
val TechnicalLogSeverity.labelResource: Int
    get() =
        when (this) {
            TechnicalLogSeverity.Debug -> R.string.components_severity_debug
            TechnicalLogSeverity.Information -> R.string.components_severity_information
            TechnicalLogSeverity.Warning -> R.string.components_severity_warning
            TechnicalLogSeverity.Error -> R.string.components_severity_error
        }

/**
 * 分类标签的文案资源。
 *
 * 分类在 logcat 侧由 [xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.AndroidTechnicalLog]
 * 写成中文短词；界面这一侧必须走资源，否则德语与英语用户读到的仍是中文。
 */
@get:StringRes
val TechnicalLogCategory.labelResource: Int
    get() =
        when (this) {
            TechnicalLogCategory.Pairing -> R.string.components_category_pairing
            TechnicalLogCategory.Transport -> R.string.components_category_transport
            TechnicalLogCategory.EventStream -> R.string.components_category_event_stream
            TechnicalLogCategory.EventIngestion -> R.string.components_category_event_ingestion
            TechnicalLogCategory.Navigation -> R.string.components_category_navigation
            TechnicalLogCategory.UserInterface -> R.string.components_category_user_interface
            TechnicalLogCategory.Failure -> R.string.components_category_failure
        }
