package com.bladderdiary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.bladderdiary.app.presentation.navigation.AppNavGraph
import com.bladderdiary.app.ui.theme.BladderDiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            BladderDiaryTheme {
                AppNavGraph()
            }
        }
    }
}
