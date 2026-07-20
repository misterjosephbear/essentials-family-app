package com.isaacshub.app.banking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.isaacshub.app.banking.domain.AccountType
import com.isaacshub.app.banking.domain.BankAccount

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey val id: String,
    val connectionId: String,
    val institutionName: String,
    val accountName: String,
    val accountType: AccountType,
    val balance: Double,
    val currency: String,
    val lastUpdated: Long
) {
    fun toDomain() = BankAccount(
        id = id,
        institutionName = institutionName,
        accountName = accountName,
        accountType = accountType,
        balance = balance,
        currency = currency,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromDomain(account: BankAccount, connectionId: String) = BankAccountEntity(
            id = account.id,
            connectionId = connectionId,
            institutionName = account.institutionName,
            accountName = account.accountName,
            accountType = account.accountType,
            balance = account.balance,
            currency = account.currency,
            lastUpdated = account.lastUpdated
        )
    }
}
