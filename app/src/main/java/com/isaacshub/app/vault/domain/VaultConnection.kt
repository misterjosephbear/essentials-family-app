package com.isaacshub.app.vault.domain

import org.json.JSONException
import org.json.JSONObject

data class VaultConnection(val baseUrl: String, val apiKey: String)

private const val PAIRING_TYPE = "isaacs-hub-storage-pairing"

/** Parses the JSON payload encoded in isaacs-hub-storage's "Pair a device" QR code. */
fun parsePairingPayload(raw: String): VaultConnection? = try {
    val json = JSONObject(raw)
    val baseUrl = json.optString("baseUrl").trimEnd('/')
    val apiKey = json.optString("apiKey")
    if (json.optString("type") != PAIRING_TYPE || baseUrl.isBlank() || apiKey.isBlank()) {
        null
    } else {
        VaultConnection(baseUrl, apiKey)
    }
} catch (e: JSONException) {
    null
}
