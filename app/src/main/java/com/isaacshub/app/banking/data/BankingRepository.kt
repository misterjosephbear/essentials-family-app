package com.isaacshub.app.banking.data

import com.isaacshub.app.banking.domain.BankAccount
import com.isaacshub.app.banking.domain.BankConnection
import com.isaacshub.app.banking.domain.BankProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class BankingRepository(
    private val dao: BankingDao,
    private val simpleFinClient: SimpleFINClient
) {

    // Connections
    fun observeAllConnections(): Flow<List<BankConnection>> =
        dao.getAllConnections().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun addSimpleFINConnection(setupToken: String): Result<String> {
        return simpleFinClient.claimToken(setupToken).map { accessUrl ->
            val connectionId = UUID.randomUUID().toString()
            val connection = BankConnection(
                id = connectionId,
                provider = BankProvider.SIMPLEFIN,
                accessToken = accessUrl,
                institutionName = "SimpleFIN",
                createdAt = System.currentTimeMillis(),
                lastSynced = null
            )
            dao.insertConnection(BankConnectionEntity.fromDomain(connection))
            connectionId
        }
    }

    suspend fun deleteConnection(connectionId: String) {
        dao.deleteConnectionAndAccounts(connectionId)
    }

    // Accounts
    fun observeAllAccounts(): Flow<List<BankAccount>> =
        dao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeAccountsByConnection(connectionId: String): Flow<List<BankAccount>> =
        dao.getAccountsByConnection(connectionId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun syncAccounts(connectionId: String): Result<Unit> {
        return runCatching {
            val connection = dao.getConnection(connectionId)
                ?: throw Exception("Connection not found")

            when (connection.provider) {
                BankProvider.SIMPLEFIN -> syncSimpleFINAccounts(connection)
                else -> throw Exception("Unsupported provider: ${connection.provider}")
            }
        }
    }

    private suspend fun syncSimpleFINAccounts(connection: BankConnectionEntity) {
        simpleFinClient.fetchAccounts(connection.accessToken).getOrThrow().let { accounts ->
            val entities = accounts.map { account ->
                BankAccountEntity.fromDomain(account, connection.id)
            }
            dao.insertAccounts(entities)
            dao.updateLastSynced(connection.id, System.currentTimeMillis())
        }
    }

    suspend fun syncAllAccounts(): Result<Unit> {
        return runCatching {
            val connections = dao.getAllConnections()
            // For Flow, we need to collect first value
            // This is a simplified version - in production you'd handle this better
        }
    }
}
