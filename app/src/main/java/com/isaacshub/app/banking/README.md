# Banking Module - Plaid Integration

This module provides automatic banking data aggregation using Plaid's free Trial plan.

## Setup Instructions

### 1. Sign up for Plaid

1. Go to https://dashboard.plaid.com/signup
2. Create a free account
3. You'll get access to the **Trial plan** which includes:
   - 10 free production items (bank connections)
   - Unlimited API calls to connected items
   - No time limit!

### 2. Get Your API Credentials

1. Go to https://dashboard.plaid.com/team/keys
2. Copy your `client_id` and `secret`
3. Choose your environment:
   - `sandbox` - For testing with fake banks
   - `development` - For the free Trial plan with real banks
   - `production` - For paid plans

### 3. Configure the App

Open `/app/src/main/java/com/isaacshub/app/banking/data/PlaidConfig.kt` and replace the placeholder values:

```kotlin
object PlaidConfig {
    const val CLIENT_ID = "your_actual_client_id_here"
    const val SECRET = "your_actual_secret_here"
    const val ENVIRONMENT = "development"  // Use "development" for Trial plan
}
```

**Important:** Never commit your actual credentials to version control! Consider using:
- Environment variables
- local.properties file
- Android's Secrets Gradle Plugin

### 4. Connect Your Banks

1. Open the app
2. Navigate to Banking tab
3. Tap "Add Connection"
4. Follow the Plaid Link flow to securely connect your bank
5. Your balances will automatically sync!

## Features

- ✅ Automatic balance updates from all major US banks
- ✅ Supports investment accounts (Acorns, etc.)
- ✅ Credit unions and small banks
- ✅ Pull-to-refresh to sync latest balances
- ✅ Secure: credentials never stored in the app
- ✅ Free forever with Trial plan (up to 10 connections)

## Supported Institutions

Plaid supports 12,000+ financial institutions including:
- Major banks (Chase, Bank of America, Wells Fargo, etc.)
- Investment platforms (Acorns, Robinhood, Vanguard, etc.)
- Credit unions
- Regional banks

Check if your bank is supported: https://plaid.com/docs/link/institutions/

## Architecture

- **Data Layer**: PlaidClient handles API communication
- **Domain Layer**: BankConnection and BankAccount models
- **UI Layer**: Compose-based screens with Material 3
- **Persistence**: Room database for caching
- **Integration**: Official Plaid Link SDK for secure authentication

## Troubleshooting

**"Failed to initialize Plaid Link"**
- Check that your CLIENT_ID and SECRET are correct
- Verify your ENVIRONMENT matches your account type
- Make sure you're using the correct keys for your environment

**"Connection not supported"**
- Some institutions may not be available in sandbox mode
- Switch to development environment to test with real banks

**API Rate Limits**
- Trial plan has unlimited API calls
- Daily balance refreshes are recommended
- Avoid excessive polling

## Security Notes

- Your bank credentials are never stored in this app
- Credentials are only transmitted to Plaid's secure servers
- Plaid uses bank-grade encryption and security
- The app only receives read-only access tokens
- Access tokens can be revoked anytime via Plaid dashboard

## Resources

- [Plaid Documentation](https://plaid.com/docs/)
- [Android SDK Guide](https://plaid.com/docs/link/android/)
- [Plaid Dashboard](https://dashboard.plaid.com/)
- [Trial Plan Details](https://plaid.com/docs/account/billing/)
