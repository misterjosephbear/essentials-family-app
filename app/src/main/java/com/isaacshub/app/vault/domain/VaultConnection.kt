package com.isaacshub.app.vault.domain

import org.json.JSONException
import org.json.JSONObject

/**
 * [remoteBaseUrl] is an optional second address (e.g. a playit.plus tunnel hostname) tried when
 * [baseUrl] can't be reached - typically because [baseUrl] is a LAN-only address and the phone is
 * off that network. Without it, a phone paired with a LAN address simply can't sync or back up
 * remotely at all.
 */
data class VaultConnection(val baseUrl: String, val apiKey: String, val remoteBaseUrl: String? = null)

private const val PAIRING_TYPE = "isaacs-hub-storage-pairing"

/** Parses the JSON payload encoded in isaacs-hub-storage's "Pair a device" QR code. */
fun parsePairingPayload(raw: String): VaultConnection? = try {
    val json = JSONObject(raw)
    val baseUrl = json.optString("baseUrl").trimEnd('/')
    val apiKey = json.optString("apiKey")
    val remoteBaseUrl = json.optString("remoteBaseUrl").trimEnd('/').ifBlank { null }
    if (json.optString("type") != PAIRING_TYPE || baseUrl.isBlank() || apiKey.isBlank()) {
        null
    } else {
        VaultConnection(baseUrl, apiKey, remoteBaseUrl)
    }
} catch (e: JSONException) {
    null
}
