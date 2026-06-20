package com.cybersaad.hackstreak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cybersaad.hackstreak.ui.HackStreakMainScreen
import com.cybersaad.hackstreak.ui.theme.HackStreakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HackStreakTheme {
                HackStreakMainScreen()
            }
        }
    }
}
