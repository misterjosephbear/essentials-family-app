package com.isaacshub.app.core.network

import com.isaacshub.app.vault.domain.VaultConnection

/**
 * Base class for API clients that need to try multiple base URLs with fallback.
 *
 * Handles the pattern where a client may have both a local URL (e.g., LAN) and a remote URL
 * (e.g., through a tunnel), and needs to try both to handle being on/off the local network.
 * The preferred URL is tried first to avoid timeout penalties on known-unreachable URLs.
 */
abstract class MultiUrlApiClient(
    protected val connection: VaultConnection,
    preferredBaseUrl: String? = null
) {

    var resolvedBaseUrl: String? = null
        protected set

    protected val candidateBaseUrls: List<String> = run {
        val all = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()
        val preferred = preferredBaseUrl?.takeIf { it in all }
        if (preferred != null) listOf(preferred) + all.filterNot { it == preferred } else all
    }

    /**
     * Tries [action] against each candidate base URL in order, returning the first one that
     * completes without throwing. Returns null only if every candidate was unreachable.
     */
    protected fun <T> tryEachBaseUrl(action: (baseUrl: String) -> T): T? {
        for (baseUrl in candidateBaseUrls) {
            val result = runCatching { action(baseUrl) }
            if (result.isSuccess) {
                resolvedBaseUrl = baseUrl
                return result.getOrThrow()
            }
        }
        return null
    }
}
