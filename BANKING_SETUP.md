# Banking Module - Plaid Setup Guide

## Overview

The Banking module now uses **Plaid** to automatically sync balances from your bank accounts (Acorns, Centra CU, InvestWithRoots).

## Why Plaid?

- ✅ **Free Trial Plan**: 10 production items for life (you only need 3)
- ✅ **No time limit**: Unlike typical trials, Plaid's free tier never expires
- ✅ **Broad coverage**: Supports thousands of US banks including Acorns
- ✅ **Automatic updates**: Real-time balance sync, no manual entry
- ✅ **Secure**: OAuth-style flow, credentials never stored in the app

## Setup Steps

### 1. Create a Plaid Account

1. Go to https://dashboard.plaid.com/signup
2. Sign up for a free account
3. Complete the verification process

### 2. Get Your API Credentials

1. Log in to the Plaid Dashboard
2. Navigate to **Team Settings** → **Keys**
3. Copy your:
   - **client_id**
   - **sandbox secret** (for testing)
   - **development secret** (for Trial plan with real banks)

### 3. Configure the App

Edit `/app/src/main/java/com/isaacshub/app/banking/data/PlaidConfig.kt`:

```kotlin
object PlaidConfig {
    const val CLIENT_ID = "your_client_id_here"
    const val SECRET = "your_secret_here"
    const val ENVIRONMENT = "development"  // or "sandbox" for testing
}
```

**Environments:**
- `"sandbox"` - Test mode with fake banks and data
- `"development"` - Trial plan with real banks (10 free items)
- `"production"` - Paid plan (not needed for personal use)

### 4. Test the Integration

1. Build and run the app
2. Navigate to **Banking** from the home screen
3. Tap **Add Connection** (the + button)
4. Select your bank from the Plaid Link UI
5. Log in with your bank credentials (OAuth flow)
6. Grant permission for read-only access
7. Your accounts should appear with current balances!

## How It Works

1. **Plaid Link** opens a secure web view for bank login
2. User authenticates directly with their bank (Plaid never stores credentials)
3. Plaid returns a **public_token**
4. App exchanges it for an **access_token** (stored securely)
5. App uses access_token to fetch balances automatically

## Refreshing Balances

- **Pull-to-refresh** on the Banking home screen
- Happens automatically in the background (configurable)
- Shows last sync time for each connection

## Troubleshooting

### "Failed to initialize Plaid Link"
- Check that `PlaidConfig` has valid credentials
- Verify you're using the correct environment secret
- Make sure you have internet connection

### "Institution not found"
- Try searching with a different spelling
- Acorns, Centra CU should both be available
- InvestWithRoots may need manual entry if not supported

### Connection expires
- Plaid access tokens can expire
- Just re-connect the account through Plaid Link
- Your previous data is preserved

## Free Trial Limits

Plaid's Trial plan:
- ✅ 10 Items (bank connections) total
- ✅ Unlimited API calls per item
- ❌ Cannot delete items to free up slots
- ❌ Must upgrade to paid plan for more than 10

Since you only need 3 accounts, you'll use 3/10 items and never hit the limit!

## Privacy & Security

- ✅ Bank credentials are **never** stored in the app
- ✅ Plaid uses bank-grade encryption (256-bit AES)
- ✅ Access is **read-only** (cannot move money)
- ✅ You can revoke access anytime from your bank's settings
- ✅ Plaid is used by Venmo, Coinbase, and other trusted apps

## Resources

- Plaid Dashboard: https://dashboard.plaid.com
- Plaid Documentation: https://plaid.com/docs/
- Free Trial Details: https://plaid.com/docs/account/billing/
- Android SDK: https://github.com/plaid/plaid-link-android

## Next Steps (Future Features)

- [ ] Add transaction history
- [ ] Budget tracking
- [ ] Spending insights
- [ ] Account alerts
- [ ] Manual account entry (for unsupported banks)

---

Enjoy automatic balance tracking! 🎉
