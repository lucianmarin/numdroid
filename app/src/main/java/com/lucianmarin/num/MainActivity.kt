package com.lucianmarin.num

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lucianmarin.num.ui.CalculatorScreen
import com.lucianmarin.num.ui.CalculatorViewModel
import com.lucianmarin.num.ui.theme.NumTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val state by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.attachContext(applicationContext)
            }

            NumTheme(themeMode = state.themeMode) {
                CalculatorScreen(viewModel = viewModel)
            }
        }
    }
}
