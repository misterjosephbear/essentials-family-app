package com.isaacshub.app.banking.data

/**
 * Plaid API configuration.
 *
 * To use Plaid:
 * 1. Sign up at https://dashboard.plaid.com/signup
 * 2. Get your client_id and secret from the dashboard
 * 3. Update the values below
 */
object PlaidConfig {
    // TODO: Replace with your actual Plaid credentials
    // Get them from: https://dashboard.plaid.com/team/keys
    const val CLIENT_ID = "YOUR_CLIENT_ID_HERE"
    const val SECRET = "YOUR_SECRET_HERE"

    // Environment: "sandbox", "development", or "production"
    // Use "sandbox" for testing, "development" for Trial plan
    const val ENVIRONMENT = "sandbox"
}
