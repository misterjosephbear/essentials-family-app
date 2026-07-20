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
    // Plaid API credentials
    // Get them from: https://dashboard.plaid.com/team/keys
    const val CLIENT_ID = "6a5d77cb3e50fe000efeba03"
    const val SECRET = "d14b8dcf36388baccdb535d2e379a0"

    // Environment: "sandbox", "development", or "production"
    // Use "production" for your production secret
    const val ENVIRONMENT = "production"
}
