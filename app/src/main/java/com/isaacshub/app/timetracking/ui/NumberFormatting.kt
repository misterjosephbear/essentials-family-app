package com.isaacshub.app.timetracking.ui

import java.util.Locale

/** Always shows exactly two decimal places (e.g. "0.00", "5.50") for consistent hundredths precision. */
fun formatNumber(value: Double): String = String.format(Locale.US, "%.2f", value)

fun formatCurrency(value: Double): String = "$" + String.format(Locale.US, "%.2f", value)
