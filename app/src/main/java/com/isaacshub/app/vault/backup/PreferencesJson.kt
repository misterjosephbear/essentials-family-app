package com.isaacshub.app.vault.backup

import androidx.datastore.preferences.core.Preferences
import org.json.JSONArray
import org.json.JSONObject

/** Serializes every preference key/value as-is, so newly added prefs are backed up automatically without this needing an update. */
fun preferencesToJson(preferences: Preferences): String {
    val json = JSONObject()
    for ((key, value) in preferences.asMap()) {
        json.put(key.name, if (value is Set<*>) JSONArray(value.toList()) else value)
    }
    return json.toString()
}
