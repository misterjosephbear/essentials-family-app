package com.isaacshub.essentials.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Admin Receiver for Essentials Family App.
 * This allows the app to:
 * - Lock the device screen
 * - Prevent app uninstallation
 * - Force user to complete chores
 */
class EssentialsDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "EssentialsDeviceAdmin"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Device Admin disable requested")
        return "Disabling Device Admin will allow uninstalling the Essentials app. Continue?"
    }
}
