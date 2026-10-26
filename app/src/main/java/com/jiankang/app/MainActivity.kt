package com.jiankang.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.runtime.mutableIntStateOf
import com.jiankang.app.ui.HealthApp
import com.jiankang.app.ui.JiankangTheme

class MainActivity : ComponentActivity() {
    private val resumeToken = mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeToken.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.rgb(248, 251, 253)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            JiankangTheme {
                HealthApp(resumeToken = resumeToken.intValue)
            }
        }
    }
}
