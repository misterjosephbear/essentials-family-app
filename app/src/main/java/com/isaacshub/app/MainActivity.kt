package com.isaacshub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.isaacshub.app.navigation.IsaacsHubApp
import com.isaacshub.app.sleep.detection.SleepDetectionController
import com.isaacshub.app.ui.theme.IsaacsHubTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as App
        lifecycleScope.launch {
            val prefs = app.preferencesRepository.userPreferences.first()
            if (prefs.autoDetectEnabled) {
                SleepDetectionController.start(this@MainActivity)
            }
        }

        setContent {
            IsaacsHubTheme {
                IsaacsHubApp()
            }
        }
    }
}
