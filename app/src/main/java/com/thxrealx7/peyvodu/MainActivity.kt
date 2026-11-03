package com.thxrealx7.peyvodu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thxrealx7.peyvodu.ui.theme.PeyVoduTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: PeyVoduViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val themeName = if (state.isSimple) "purple" else state.theme
            PeyVoduTheme(themeName) {
                PeyVoduApp(vm = vm, state = state)
            }
        }
    }
}
