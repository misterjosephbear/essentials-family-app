package com.isaacshub.app.banking.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.isaacshub.app.banking.domain.BankConnection
import com.isaacshub.app.banking.domain.BankProvider

@Entity(tableName = "bank_connections")
data class BankConnectionEntity(
    @PrimaryKey val id: String,
    val provider: BankProvider,
    val accessToken: String,
    val institutionName: String?,
    val createdAt: Long,
    val lastSynced: Long?
) {
    fun toDomain() = BankConnection(
        id = id,
        provider = provider,
        accessToken = accessToken,
        institutionName = institutionName,
        createdAt = createdAt,
        lastSynced = lastSynced
    )

    companion object {
        fun fromDomain(connection: BankConnection) = BankConnectionEntity(
            id = connection.id,
            provider = connection.provider,
            accessToken = connection.accessToken,
            institutionName = connection.institutionName,
            createdAt = connection.createdAt,
            lastSynced = connection.lastSynced
        )
    }
}
