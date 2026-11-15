package com.example.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.demo.ui.navigation.AppNavHost
import com.example.demo.ui.theme.DemoProjectTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single-activity app: all screens are composables hosted by [AppNavHost]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoProjectTheme {
                AppNavHost()
            }
        }
    }
}
