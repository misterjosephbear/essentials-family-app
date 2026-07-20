package com.isaacshub.app.banking.domain

/**
 * Represents a connected bank account with its current balance.
 */
data class BankAccount(
    val id: String,
    val institutionName: String,
    val accountName: String,
    val accountType: AccountType,
    val balance: Double,
    val currency: String = "USD",
    val lastUpdated: Long
)

enum class AccountType {
    CHECKING,
    SAVINGS,
    CREDIT_CARD,
    INVESTMENT,
    LOAN,
    OTHER
}
