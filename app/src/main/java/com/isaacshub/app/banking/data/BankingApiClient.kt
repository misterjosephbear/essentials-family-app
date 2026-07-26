package com.isaacshub.app.banking.data

import com.isaacshub.app.banking.domain.BankAccount
import com.isaacshub.app.banking.domain.AccountType
import com.isaacshub.app.core.network.BaseApiClient
import com.isaacshub.app.vault.domain.VaultConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PlaidLinkToken(val linkToken: String)
data class PlaidAccessToken(val accessToken: String, val itemId: String)

/**
 * Communicates with isaacs-hub-server's banking API endpoints for Plaid integration.
 */
class BankingApiClient(connection: VaultConnection) : BaseApiClient(connection) {

    /**
     * Get a Plaid Link token to initialize the Plaid Link flow.
     */
    suspend fun createLinkToken(): Result<PlaidLinkToken> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val json = post(baseUrl, "/api/banking/plaid/link-token")
            PlaidLinkToken(json.getString("link_token"))
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    /**
     * Exchange a Plaid public token for an access token.
     */
    suspend fun exchangePublicToken(publicToken: String): Result<PlaidAccessToken> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val body = JSONObject().put("public_token", publicToken)
            val json = post(baseUrl, "/api/banking/plaid/exchange-token", body)
            PlaidAccessToken(
                accessToken = json.getString("access_token"),
                itemId = json.getString("item_id")
            )
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
    }

    /**
     * Fetch account balances using an access token.
     */
    suspend fun getAccounts(accessToken: String): Result<List<BankAccount>> = withContext(Dispatchers.IO) {
        val result = tryEachBaseUrl { baseUrl ->
            val body = JSONObject().put("access_token", accessToken)
            val json = post(baseUrl, "/api/banking/plaid/accounts", body)
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
        }
        result?.let { Result.success(it) } ?: Result.failure(Exception("Couldn't reach the server."))
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
}
