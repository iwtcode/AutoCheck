package com.autocheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autocheck.app.ui.AppRoot
import com.autocheck.app.ui.MainViewModel
import com.autocheck.app.ui.theme.AutoCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoCheckTheme {
                val vm: MainViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}
