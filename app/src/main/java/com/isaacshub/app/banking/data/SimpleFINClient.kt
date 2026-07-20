package com.isaacshub.app.banking.data

import com.isaacshub.app.banking.domain.AccountType
import com.isaacshub.app.banking.domain.BankAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Client for SimpleFIN Protocol (https://beta-bridge.simplefin.org/simplefin)
 *
 * SimpleFIN provides read-only access to thousands of US financial institutions
 * for $15/year. It's designed for personal finance apps.
 */
class SimpleFINClient {

    /**
     * Claims a setup token and returns an access URL.
     * The setup token is obtained from https://beta-bridge.simplefin.org/claim
     */
    suspend fun claimToken(setupToken: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://beta-bridge.simplefin.org/claim").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write("token=$setupToken".toByteArray())
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                throw Exception("Failed to claim token: HTTP $responseCode")
            }

            conn.inputStream.bufferedReader().use { it.readText() }
        }
    }

    /**
     * Fetches account information from SimpleFIN using an access URL.
     * Access URL format: https://username:password@beta-bridge.simplefin.org/simplefin
     */
    suspend fun fetchAccounts(accessUrl: String): Result<List<BankAccount>> = withContext(Dispatchers.IO) {
        runCatching {
            // Parse the access URL to extract credentials
            val url = URL(accessUrl)
            val userInfo = url.userInfo ?: throw Exception("Access URL missing credentials")
            val credentials = Base64.getEncoder().encodeToString(userInfo.toByteArray())

            val apiUrl = "${url.protocol}://${url.host}${url.path}/accounts"
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Basic $credentials")
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw Exception("Failed to fetch accounts: HTTP $responseCode - $error")
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            parseAccountsResponse(response)
        }
    }

    private fun parseAccountsResponse(jsonString: String): List<BankAccount> {
        val json = JSONObject(jsonString)
        val accountsArray = json.getJSONArray("accounts")
        val currentTime = System.currentTimeMillis()

        return (0 until accountsArray.length()).map { i ->
            val accountJson = accountsArray.getJSONObject(i)

            BankAccount(
                id = accountJson.getString("id"),
                institutionName = accountJson.optString("org", "Unknown"),
                accountName = accountJson.optString("name", "Account"),
                accountType = parseAccountType(accountJson.optString("type", "other")),
                balance = accountJson.optDouble("balance", 0.0) / 10000.0, // SimpleFIN uses fixed-point (10000 = $1.00)
                currency = accountJson.optString("currency", "USD"),
                lastUpdated = currentTime
            )
        }
    }

    private fun parseAccountType(type: String): AccountType {
        return when (type.lowercase()) {
            "checking" -> AccountType.CHECKING
            "savings" -> AccountType.SAVINGS
            "credit card", "creditcard" -> AccountType.CREDIT_CARD
            "investment", "brokerage" -> AccountType.INVESTMENT
            "loan", "mortgage" -> AccountType.LOAN
            else -> AccountType.OTHER
        }
    }
}
