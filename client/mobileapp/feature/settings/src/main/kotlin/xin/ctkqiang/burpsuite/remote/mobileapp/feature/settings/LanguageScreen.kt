package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/** 语言设置（plan §46）。无状态：状态由外面传进来，用户意图往外抛。 */
@Composable
fun LanguageScreen(
    uiState: LanguageUserInterfaceState,
    onIntent: (LanguageUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(HORIZONTAL_PADDING)) {
            ScreenHeading(titleResource = R.string.settings_language_title)
            Spacer(modifier = Modifier.height(SECTION_SPACING))
            Text(
                text = stringResource(R.string.settings_language_heading),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(ROW_SPACING))
            // selectableGroup 让读屏把整组选项当成单选来念，而不是四个各自独立的控件。
            Column(modifier = Modifier.selectableGroup()) {
                LanguagePreference.entries.forEach { language ->
                    LanguageOptionRow(
                        labelResource = language.labelResource,
                        isSelected = language == uiState.language,
                        onSelect = { onIntent(LanguageUserInterfaceIntent.SelectLanguage(language)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    @StringRes labelResource: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect)
                .padding(vertical = ROW_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(ROW_SPACING))
        Text(text = stringResource(labelResource), style = MaterialTheme.typography.bodyLarge)
    }
}

// 语言名用各自的语言书写：让用户在不确定「Deutsch 是哪个」的时候也能认出来。
@get:StringRes
private val LanguagePreference.labelResource: Int
    get() =
        when (this) {
            LanguagePreference.Automatic -> R.string.settings_language_automatic
            LanguagePreference.English -> R.string.settings_language_english
            LanguagePreference.Chinese -> R.string.settings_language_chinese
            LanguagePreference.German -> R.string.settings_language_german
        }

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun LanguageScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        LanguageScreen(uiState = LanguageUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun LanguageScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        LanguageScreen(
            uiState = LanguageUserInterfaceState(LanguagePreference.Chinese),
            onIntent = {},
        )
    }
}

private val HORIZONTAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
