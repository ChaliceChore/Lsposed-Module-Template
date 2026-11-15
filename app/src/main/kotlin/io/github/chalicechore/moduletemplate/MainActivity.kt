package io.github.chalicechore.moduletemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.chalicechore.moduletemplate.ui.MainViewModel
import io.github.chalicechore.moduletemplate.ui.SettingsApp
import io.github.chalicechore.moduletemplate.ui.UiState

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.uiState.value is UiState.Loading }
        setContent { SettingsApp(viewModel) }
    }
}
