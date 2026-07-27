package com.isaacshub.app.banking.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_account_selections",
    foreignKeys = [
        ForeignKey(
            entity = BankAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetAccountSelectionEntity(
    @PrimaryKey val accountId: String,  // References BankAccountEntity.id
    val isIncluded: Boolean              // true if this account counts toward budget
)
