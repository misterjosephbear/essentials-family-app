package com.isaacshub.app.banking.domain

import kotlin.math.min

/**
 * Represents the current state of the budget tower based on available balance.
 */
data class BudgetState(
    val totalBalance: Double,
    val categories: List<CategoryFillState>
)

/**
 * Represents how much a single category is filled.
 */
data class CategoryFillState(
    val category: BudgetCategory,
    val amountAllocated: Double,  // How much money is "in" this category
    val fillPercentage: Float      // 0.0 to 1.0
)

/**
 * Calculates the budget state by filling categories from bottom to top.
 *
 * The algorithm:
 * 1. Sort categories by order (0 = bottom/crucial, 3 = top/frivolous)
 * 2. For each category, allocate as much of the remaining balance as possible (up to threshold)
 * 3. Calculate fill percentage for each category
 *
 * Example:
 * - Total balance: $5000
 * - Crucial threshold: $2000 → Gets $2000 (100% filled), $3000 remaining
 * - Utility threshold: $1500 → Gets $1500 (100% filled), $1500 remaining
 * - Convenience threshold: $1000 → Gets $1000 (100% filled), $500 remaining
 * - Frivolous threshold: $500 → Gets $500 (100% filled), $0 remaining
 */
fun calculateBudgetState(
    categories: List<BudgetCategory>,
    totalBalance: Double
): BudgetState {
    val sortedCategories = categories.sortedBy { it.order }
    var remainingBalance = totalBalance

    val categoryStates = sortedCategories.map { category ->
        val allocated = min(remainingBalance, category.threshold).coerceAtLeast(0.0)
        val fillPercentage = if (category.threshold > 0) {
            (allocated / category.threshold).toFloat().coerceIn(0f, 1f)
        } else 0f

        remainingBalance = (remainingBalance - allocated).coerceAtLeast(0.0)

        CategoryFillState(
            category = category,
            amountAllocated = allocated,
            fillPercentage = fillPercentage
        )
    }

    return BudgetState(
        totalBalance = totalBalance,
        categories = categoryStates
    )
}
