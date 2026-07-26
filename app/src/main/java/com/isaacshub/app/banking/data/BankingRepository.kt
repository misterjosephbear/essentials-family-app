package com.isaacshub.app.banking.data

import com.isaacshub.app.banking.domain.BankAccount
import com.isaacshub.app.banking.domain.BankConnection
import com.isaacshub.app.banking.domain.BankProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class BankingRepository(
    private val connectionDao: BankConnectionDao,
    private val accountDao: BankAccountDao,
    private val plaidClient: PlaidClient
) {

    // Connections
    fun observeAllConnections(): Flow<List<BankConnection>> =
        connectionDao.getAllConnections().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Add a Plaid connection using a public token from Plaid Link
     */
    suspend fun addPlaidConnection(publicToken: String): Result<String> {
        return plaidClient.exchangePublicToken(publicToken).mapCatching { accessToken ->
            val connectionId = UUID.randomUUID().toString()

            // Fetch institution name
            val institutionName = plaidClient.getInstitutionName(accessToken).getOrElse { "Unknown Bank" }

            val connection = BankConnection(
                id = connectionId,
                provider = BankProvider.PLAID,
                accessToken = accessToken,
                institutionName = institutionName,
                createdAt = System.currentTimeMillis(),
                lastSynced = null
            )
            connectionDao.insertConnection(BankConnectionEntity.fromDomain(connection))

            // Immediately sync accounts after adding connection
            syncPlaidAccounts(BankConnectionEntity.fromDomain(connection))

            connectionId
        }
    }

    /**
     * Create a link token for Plaid Link initialization
     */
    suspend fun createLinkToken(userId: String): Result<String> {
        return plaidClient.createLinkToken(userId)
    }

    suspend fun deleteConnection(connectionId: String) {
        // Delete accounts first (foreign key constraint)
        accountDao.deleteAccountsByConnection(connectionId)
        connectionDao.deleteConnection(connectionId)
    }

    // Accounts
    fun observeAllAccounts(): Flow<List<BankAccount>> =
        accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeAccountsByConnection(connectionId: String): Flow<List<BankAccount>> =
        accountDao.getAccountsByConnection(connectionId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun syncAccounts(connectionId: String): Result<Unit> {
        return runCatching {
            val connection = connectionDao.getConnection(connectionId)
                ?: throw Exception("Connection not found")

            when (connection.provider) {
                BankProvider.PLAID -> syncPlaidAccounts(connection)
                else -> throw Exception("Unsupported provider: ${connection.provider}")
            }
        }
    }

    private suspend fun syncPlaidAccounts(connection: BankConnectionEntity) {
        plaidClient.fetchAccounts(connection.accessToken).getOrThrow().let { accounts ->
            // Update institution name for all accounts
            val accountsWithInstitution = accounts.map { account ->
                account.copy(institutionName = connection.institutionName ?: "Unknown")
            }

            val entities = accountsWithInstitution.map { account ->
                BankAccountEntity.fromDomain(account, connection.id)
            }
            accountDao.insertAccounts(entities)
            connectionDao.updateLastSynced(connection.id, System.currentTimeMillis())
        }
    }

    suspend fun syncAllAccounts(): Result<Unit> {
        return runCatching {
            val connections = connectionDao.getAllConnections()
            // Note: This is a Flow, so in production you'd want to collect and iterate
            // For now, we'll handle syncing from the ViewModel layer per-connection
        }
    }
}
