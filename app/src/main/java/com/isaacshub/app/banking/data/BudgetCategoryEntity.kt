package com.isaacshub.app.banking.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_categories")
data class BudgetCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,           // "Crucial Budget", "Utility Budget", etc.
    val threshold: Double,      // Dollar amount needed to FILL this category
    val order: Int,             // 0=bottom (Crucial), 3=top (Frivolous)
    val colorHex: String,       // Material3 color as hex string
    val icon: String            // Icon identifier (emoji or icon name)
)
