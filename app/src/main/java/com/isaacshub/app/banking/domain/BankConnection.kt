package com.isaacshub.app.banking.domain

/**
 * Represents a connection to a financial institution or aggregation service.
 */
data class BankConnection(
    val id: String,
    val provider: BankProvider,
    val accessToken: String,
    val institutionName: String?,
    val createdAt: Long,
    val lastSynced: Long?
)

enum class BankProvider {
    SIMPLEFIN,
    PLAID,
    TELLER,
    MANUAL
}
