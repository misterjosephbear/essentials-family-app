package com.isaacshub.app.banking.domain

import androidx.compose.ui.graphics.Color
import com.isaacshub.app.banking.data.BudgetCategoryEntity

/**
 * Represents a budget category with a threshold amount.
 */
data class BudgetCategory(
    val id: String,
    val name: String,
    val threshold: Double,
    val order: Int,
    val color: Color,
    val icon: String
)

// Extension functions for conversion
fun BudgetCategoryEntity.toDomain(): BudgetCategory {
    return BudgetCategory(
        id = id,
        name = name,
        threshold = threshold,
        order = order,
        color = Color(android.graphics.Color.parseColor(colorHex)),
        icon = icon
    )
}

fun BudgetCategory.toEntity(): BudgetCategoryEntity {
    return BudgetCategoryEntity(
        id = id,
        name = name,
        threshold = threshold,
        order = order,
        colorHex = String.format("#%08X", color.value.toInt()),
        icon = icon
    )
}
