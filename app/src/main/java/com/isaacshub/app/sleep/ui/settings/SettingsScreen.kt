package com.isaacshub.app.sleep.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.isaacshub.app.App
import com.isaacshub.app.sleep.detection.SleepDetectionController

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.preferencesRepository)
    )
    val prefs by viewModel.preferences.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setAutoDetectEnabled(true)
            SleepDetectionController.start(context)
        }
    }

    var sliderValue by remember(prefs.sleepNeedMinutes) { mutableFloatStateOf(prefs.sleepNeedMinutes.toFloat()) }

    val powerManager = context.getSystemService(PowerManager::class.java)
    val ignoringOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Sleep tracking", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-detect sleep")
                Text(
                    "Uses screen and motion signals to detect sleep automatically",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = prefs.autoDetectEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setAutoDetectEnabled(true)
                            SleepDetectionController.start(context)
                        }
                    } else {
                        viewModel.setAutoDetectEnabled(false)
                        SleepDetectionController.stop(context)
                    }
                }
            )
        }

        Column {
            Text("Sleep need: ${sliderValue.toInt() / 60}h ${sliderValue.toInt() % 60}m")
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { viewModel.setSleepNeedMinutes(sliderValue.toInt()) },
                valueRange = 360f..600f,
                steps = 23
            )
        }

        if (!ignoringOptimizations) {
            Column {
                OutlinedButton(onClick = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }) {
                    Text("Improve detection reliability")
                }
                Text(
                    "Sleep detection runs more reliably if battery optimization is disabled for this app.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
