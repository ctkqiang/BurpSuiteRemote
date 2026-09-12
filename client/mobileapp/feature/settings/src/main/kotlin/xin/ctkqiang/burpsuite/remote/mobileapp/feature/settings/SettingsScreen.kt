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
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/** 设置界面。无状态：状态由外面传进来，选择动作往外抛。 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(HORIZONTAL_PADDING)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(SECTION_SPACING))
            Text(
                text = stringResource(R.string.settings_theme_heading),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(ROW_SPACING))
            // selectableGroup 让读屏把整组选项当成单选来念，而不是三个各自独立的控件。
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { themeMode ->
                    ThemeModeOptionRow(
                        labelResource = themeMode.labelResource,
                        isSelected = themeMode == uiState.themeMode,
                        onSelect = { onThemeModeSelected(themeMode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOptionRow(
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

@get:StringRes
private val ThemeMode.labelResource: Int
    get() =
        when (this) {
            ThemeMode.Automatic -> R.string.settings_theme_automatic
            ThemeMode.Light -> R.string.settings_theme_light
            ThemeMode.Dark -> R.string.settings_theme_dark
        }

private val HORIZONTAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
