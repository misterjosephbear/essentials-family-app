package com.isaacshub.essentials.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Manages Device Admin functionality for app blocking.
 */
class DeviceAdminManager(private val context: Context) {

    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val componentName = ComponentName(context, EssentialsDeviceAdminReceiver::class.java)

    companion object {
        private const val TAG = "DeviceAdminManager"
        const val REQUEST_CODE_ENABLE_ADMIN = 1001
    }

    /**
     * Check if device admin is enabled
     */
    fun isAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(componentName)
    }

    /**
     * Request device admin activation
     */
    fun requestAdminActivation(activity: Activity) {
        if (isAdminActive()) {
            Log.d(TAG, "Device admin already active")
            return
        }

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Essentials needs Device Admin to ensure chores are completed before device use."
            )
        }

        activity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    /**
     * Lock the device screen
     */
    fun lockDevice() {
        if (!isAdminActive()) {
            Log.w(TAG, "Cannot lock device: admin not active")
            return
        }

        try {
            devicePolicyManager.lockNow()
            Log.d(TAG, "Device locked")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock device", e)
        }
    }

    /**
     * Remove device admin (for uninstallation)
     */
    fun removeAdmin() {
        if (!isAdminActive()) {
            return
        }

        try {
            devicePolicyManager.removeActiveAdmin(componentName)
            Log.d(TAG, "Device admin removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove device admin", e)
        }
    }
}
