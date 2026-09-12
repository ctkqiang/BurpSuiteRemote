package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent

/** 语言设置的状态持有者。写操作的唯一入口是 [handleIntent]，界面不直接碰仓库。 */
class LanguageViewModel(
    private val languagePreferenceRepository: LanguagePreferenceRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : ViewModel() {
    private val effectChannel = Channel<LanguageUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；界面重建这类动作走这里，不塞进状态。 */
    val effects: Flow<LanguageUserInterfaceEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<LanguageUserInterfaceState> =
        languagePreferenceRepository
            .observeLanguage()
            .map { language -> LanguageUserInterfaceState(language) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = LanguageUserInterfaceState(),
            )

    fun handleIntent(intent: LanguageUserInterfaceIntent) {
        when (intent) {
            is LanguageUserInterfaceIntent.SelectLanguage -> selectLanguage(intent.language)
        }
    }

    private fun selectLanguage(language: LanguagePreference) {
        record(
            message = "界面语言改为 ${language.name}，请求重建界面",
            attributes = mapOf("languageTag" to language.languageTag.orEmpty()),
        )
        viewModelScope.launch {
            languagePreferenceRepository.setLanguage(language)
            effectChannel.trySend(LanguageUserInterfaceEffect.RestartForLanguageChange)
        }
    }

    private fun record(
        message: String,
        attributes: Map<String, String> = emptyMap(),
    ) {
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.UserInterface,
                message = message,
                attributes = attributes,
            ),
        )
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
