package com.isaacshub.app.vault.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesJsonTest {

    @Test
    fun `serializes scalar preference values by key name`() {
        val prefs = preferencesOf(
            intPreferencesKey("sleep_need_minutes") to 480,
            booleanPreferencesKey("auto_detect_enabled") to true,
            stringPreferencesKey("base_url") to "http://10.0.0.5:4000"
        )
        val json = JSONObject(preferencesToJson(prefs))
        assertEquals(480, json.getInt("sleep_need_minutes"))
        assertTrue(json.getBoolean("auto_detect_enabled"))
        assertEquals("http://10.0.0.5:4000", json.getString("base_url"))
    }

    @Test
    fun `serializes a string set as a json array`() {
        val prefs = preferencesOf(stringSetPreferencesKey("tags") to setOf("a", "b"))
        val json = JSONObject(preferencesToJson(prefs))
        val array = json.getJSONArray("tags")
        val values = (0 until array.length()).map { array.getString(it) }.toSet()
        assertEquals(setOf("a", "b"), values)
    }

    @Test
    fun `empty preferences produce an empty json object`() {
        val json = preferencesToJson(preferencesOf())
        assertEquals("{}", json)
    }
}
