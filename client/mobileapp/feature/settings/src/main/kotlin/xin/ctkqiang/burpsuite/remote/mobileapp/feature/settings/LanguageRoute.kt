package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 装配层：把语言端口交给 ViewModel，把状态与意图处理交给无状态的界面。
 *
 * 换语言要重建 Activity 才生效；重建是界面层的事（ViewModel 不该碰 Activity），因此效果在这里落地。
 */
@Composable
fun LanguageRoute(modifier: Modifier = Modifier) {
    val languagePreferenceRepository = LocalLanguagePreferenceRepository.current
    val viewModel: LanguageViewModel = viewModel { LanguageViewModel(languagePreferenceRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LanguageUserInterfaceEffect.RestartForLanguageChange ->
                    (context as? Activity)?.recreate()
            }
        }
    }

    LanguageScreen(uiState = uiState, onIntent = viewModel::handleIntent, modifier = modifier)
}
