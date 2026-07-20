package com.isaacshub.app.banking.data

import com.isaacshub.app.banking.domain.BankAccount
import com.isaacshub.app.banking.domain.AccountType
import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PlaidLinkToken(val linkToken: String)
data class PlaidAccessToken(val accessToken: String, val itemId: String)

/**
 * Communicates with isaacs-hub-server's banking API endpoints for Plaid integration.
 */
class BankingApiClient(private val connection: VaultConnection) {

    /**
     * Get a Plaid Link token to initialize the Plaid Link flow.
     */
    suspend fun createLinkToken(): Result<PlaidLinkToken> = withContext(Dispatchers.IO) {
        val candidates = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()
        for (baseUrl in candidates) {
            val result = runCatching { postCreateLinkToken(baseUrl) }
            result.getOrNull()?.let { return@withContext Result.success(it) }
        }
        Result.failure(Exception("Couldn't reach the server."))
    }

    /**
     * Exchange a Plaid public token for an access token.
     */
    suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken> = withContext(Dispatchers.IO) {
        val candidates = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()
        for (baseUrl in candidates) {
            val result = runCatching { postExchangePublicToken(baseUrl, publicToken) }
            result.getOrNull()?.let { return@withContext Result.success(it) }
        }
        Result.failure(Exception("Couldn't reach the server."))
    }

    /**
     * Fetch account balances using an access token.
     */
    suspend fun getAccounts(accessToken: String): Result<List<BankAccount>> = withContext(Dispatchers.IO) {
        val candidates = listOfNotNull(connection.baseUrl, connection.remoteBaseUrl).distinct()
        for (baseUrl in candidates) {
            val result = runCatching { postGetAccounts(baseUrl, accessToken) }
            result.getOrNull()?.let { return@withContext Result.success(it) }
        }
        Result.failure(Exception("Couldn't reach the server."))
    }

    private fun postCreateLinkToken(baseUrl: String): PlaidLinkToken {
        val conn = URL("$baseUrl/api/banking/plaid/link-token").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = false
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        return try {
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                PlaidLinkToken(json.getString("link_token"))
            } else {
                throw Exception(errorMessageFrom(conn))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun postExchangePublicToken(baseUrl: String, publicToken: String): PlaidAccessToken {
        val conn = URL("$baseUrl/api/banking/plaid/exchange-token").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        return try {
            val requestBody = JSONObject().put("public_token", publicToken).toString()
            conn.outputStream.use { it.write(requestBody.toByteArray()) }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                PlaidAccessToken(
                    accessToken = json.getString("access_token"),
                    itemId = json.getString("item_id")
                )
            } else {
                throw Exception(errorMessageFrom(conn))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun postGetAccounts(baseUrl: String, accessToken: String): List<BankAccount> {
        val conn = URL("$baseUrl/api/banking/plaid/accounts").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer ${connection.apiKey}")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        return try {
            val requestBody = JSONObject().put("access_token", accessToken).toString()
            conn.outputStream.use { it.write(requestBody.toByteArray()) }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val accountsArray = json.getJSONArray("accounts")
                List(accountsArray.length()) { i ->
                    val acc = accountsArray.getJSONObject(i)
                    val balances = acc.getJSONObject("balances")
                    BankAccount(
                        id = acc.getString("account_id"),
                        institutionName = json.optString("institution_name", "Unknown"),
                        accountName = acc.getString("name"),
                        accountType = parseAccountType(acc.getString("subtype")),
                        balance = balances.getDouble("current"),
                        currency = balances.optString("iso_currency_code", "USD"),
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            } else {
                throw Exception(errorMessageFrom(conn))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseAccountType(subtype: String): AccountType {
        return when (subtype.lowercase()) {
            "checking" -> AccountType.CHECKING
            "savings" -> AccountType.SAVINGS
            "credit card", "credit" -> AccountType.CREDIT_CARD
            "brokerage", "ira", "401k", "roth", "investment" -> AccountType.INVESTMENT
            "loan", "mortgage", "student" -> AccountType.LOAN
            else -> AccountType.OTHER
        }
    }

    private fun errorMessageFrom(conn: HttpURLConnection): String {
        val body = (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }
        val parsed = body?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() }?.takeIf { it.isNotBlank() }
        return parsed ?: "Request failed (HTTP ${conn.responseCode})"
    }
}
