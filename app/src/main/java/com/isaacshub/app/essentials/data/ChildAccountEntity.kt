package com.isaacshub.app.essentials.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_accounts")
data class ChildAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val createdAtEpochMillis: Long
)
